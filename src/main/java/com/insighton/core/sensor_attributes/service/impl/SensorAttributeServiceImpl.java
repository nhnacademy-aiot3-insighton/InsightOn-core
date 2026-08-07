package com.insighton.core.sensor_attributes.service.impl;

import com.insighton.core.sensor_attributes.dto.SensorAttributeResponse;
import com.insighton.core.sensor_attributes.entity.MetricDefinition;
import com.insighton.core.sensor_attributes.entity.SensorAttribute;
import com.insighton.core.sensor_attributes.exception.MetricKeyNotFoundException;
import com.insighton.core.sensor_attributes.repository.SensorAttributeRepository;
import com.insighton.core.sensor_attributes.service.SensorAttributeService;
import com.insighton.core.groupmember.entity.GroupMember;
import com.insighton.core.groupmember.service.GroupMemberService;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.sensors.entity.Sensor;
import com.insighton.core.sensors.exception.SensorNotFoundException;
import com.insighton.core.sensors.exception.InvalidSensorValueException;
import com.insighton.core.sensors.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@link SensorAttributeService}의 기본 JPA 구현체.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorAttributeServiceImpl implements SensorAttributeService {

    private final SensorAttributeRepository attributeRepository; // 속성 조회/저장
    private final SensorRepository sensorRepository; // 소속 센서 조회
    private final GroupMemberService groupMembersService; // 그룹 멤버 권한 검증용 서비스


    // 요청자가 해당 그룹의 MANGER 이상의 권한을 가졌는지 검증
    private void validateManagerRole(Long userId, Long groupsId) {
        GroupMember member = groupMembersService.validateGroupMembers(groupsId, userId);
        if (member.isMember()) {
            throw NoPermissionException.forAdmin(member.getGroupMemberId());
        }
    }


    // 조회용 - 그룹 소속만 확인, 역할은 무관
    private void validateGroupMembership(Long userId, Long groupId) {
        groupMembersService.validateGroupMembers(groupId, userId);
    }

    public List<SensorAttributeResponse> getAllAttributeBySensorId(Long userId, Long sensorId) {

        // 1. 해당 장치의 존재 유무 검증
        Sensor entity = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new SensorNotFoundException(sensorId));

        validateGroupMembership(userId, entity.getGroup().getGroupId()); // Groups 객체에서 Long ID 호출

        // 2. DB에서 장치 속성 목록 조회 후 Enum 정보를 매핑하여 DTO로 변환
        return attributeRepository.findBySensorSensorId(sensorId)
                .stream()
                .map(attr -> {
                    // Enum에서 메트릭 표준 정의(한글 명칭, 단위) 바인딩
                    MetricDefinition metricDefinition = MetricDefinition.fromKey(attr.getMetricKey());

                    return new SensorAttributeResponse(
                            attr.getMetricKey(),
                            metricDefinition.getMetricName(),
                            metricDefinition.getUnit(),
                            attr.getCurrentValueStr()
                    );
                }).toList();
    }


    /**
     * 액추에이터(조명, 에어컨 등) 제어 명령 실행 시, DB 상의 최신 상태값({@code currentValueStr})을 갱신합니다.
     *
     * @param sensorId  대상 액추에이터 기기 ID
     * @param metricKey 변경 대상 메트릭 키 (ex. "power_status", "ac_mode")
     * @param newValue  변경하고자 하는 상태/수치값 (ex. "ON", "OFF", "24")
     */
    @Transactional
    public void updateActuatorValue(Long userId, Long sensorId, String metricKey, String newValue) {

        // existsById 대신 findById로 직접 조회
        // DB조회 1번으로 줄이기
        Sensor entity = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new SensorNotFoundException(sensorId));

        // 권한 체크
        validateManagerRole(userId, entity.getGroup().getGroupId()); // Groups 객체에서 Long ID 호출

        //TODO: SensorType enum 제거(센서/액추에이터 테이블 분리)로 "센서 로우는 제어 불가" 가드가 사라짐.
        // 액추에이터 제어는 actuators 테이블 기준으로 재설계 필요 — 현재는 아무 센서 속성이나 수정 가능한 상태.

        // 변경할 수치 값 유효성 검사
        // MQTT 패킷 수신, 룰엔진 내부 호출, 비동기 이벤트를 통해 직접 호출될 수 있기 때문에
        // 재활용성을 보장하기 위한 방어적 프로그래밍
        if (newValue == null || newValue.trim().isEmpty()) {
            throw new InvalidSensorValueException("액추에이터 제어 값은 비어있을 수 없습니다");
        }


        /**
         * 코더레빗 제안
         * 대소문자 불일치로 인한 404 방지
         * 정규화된 메트릭 키로 조회하세요. MetricDefinition.fromKey(metricKey)는 대소문자를 무시하지만, 바로 아래 조회는 원본 metricKey를 사용합니다.
         * CO2처럼 대문자로 들어온 값은 검증을 통과해도 co2로 저장된 행을 못 찾을 수 있으니
         * MetricDefinition.fromKey(metricKey).getMetricKey()를 조회 인자로 쓰세요.
         */
        MetricDefinition definition = MetricDefinition.fromKey(metricKey);
        String normalizedMetricKey = definition.getMetricKey();

        // 정규화된 metricKey로 DB 조회
        SensorAttribute attribute = attributeRepository
                .findBySensorSensorIdAndMetricKey(sensorId, normalizedMetricKey)
                .orElseThrow(() -> new MetricKeyNotFoundException("매트릭 키를 찾을 수 없습니다 (ID: " + metricKey + ")"));

        attribute.updateCurrentValue(newValue);
    }

    /**
     * 외부 수집 엔진/MQTT 등을 통해 입력된 센서 패킷이 유효한 기기 속성인지 검증합니다.
     *
     * @param sensorId  장치 ID
     * @param metricKey 메트릭 키
     * @return 해당 기기에 메트릭 키가 정상 등록되어 있으면 true, 없으면 false
     */
    // 등록되지 않는 메트릭키가 전달되어도 예외가 터지지않고 false 반환
    // 대소문자 정규화
    public boolean isValidSensorAttribute(Long sensorId, String metricKey) {
        if (sensorId == null || metricKey == null) {
            return false;
        }
        return MetricDefinition.findFromKey(metricKey)
                .map(def -> attributeRepository.
                        existsBySensorSensorIdAndMetricKey(sensorId, def.getMetricKey()))
                .orElse(false);
    }
}