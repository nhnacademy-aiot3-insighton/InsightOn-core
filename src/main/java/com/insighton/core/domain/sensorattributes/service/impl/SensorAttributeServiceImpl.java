package com.insighton.core.domain.sensorattributes.service.impl;

import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionCreateRequest;
import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionResponse;
import com.insighton.core.domain.sensorattributes.dto.SensorAttributeResponse;
import com.insighton.core.domain.sensorattributes.entity.MetricDefinition;
import com.insighton.core.domain.sensorattributes.entity.SensorAttribute;
import com.insighton.core.domain.sensorattributes.exception.MetricKeyAlreadyExistsException;
import com.insighton.core.domain.sensorattributes.exception.MetricKeyNotFoundException;
import com.insighton.core.domain.sensorattributes.repository.MetricDefinitionRepository;
import com.insighton.core.domain.sensorattributes.repository.SensorAttributeRepository;
import com.insighton.core.domain.sensorattributes.service.SensorAttributeService;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.sensors.entity.Sensor;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import com.insighton.core.domain.sensors.repository.SensorRepository;
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
    private final MetricDefinitionRepository metricDefinitionRepository;


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


    // 추가적인 메트릭 정의가 있을시 추가
    @Override
    @Transactional
    public void createMetricDefinition(MetricDefinitionCreateRequest request) {

        if(metricDefinitionRepository.findByMetricKeyIgnoreCase(request.metricKey()).isPresent()){
            throw new MetricKeyAlreadyExistsException(request.metricKey());
        }

        MetricDefinition metricDefinition = MetricDefinition.builder()
                .metricKey(request.metricKey())
                .metricName(request.metricName())
                .unit(request.unit())
                .build();

        metricDefinitionRepository.save(metricDefinition);
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
                    MetricDefinition metricDefinition = metricDefinitionRepository
                            .findByMetricKeyIgnoreCase(attr.getMetricKey())
                            .orElseThrow(() -> new MetricKeyNotFoundException(attr.getMetricKey()));

                    return new SensorAttributeResponse(
                            attr.getMetricKey(),
                            metricDefinition.getMetricName(),
                            metricDefinition.getUnit()
                    );
                }).toList();
    }


    // 매트릭 정의 목록 조회
    @Override
    public List<MetricDefinitionResponse> getAllMetricDefinitions() {
        return metricDefinitionRepository.findAll().stream()
                .map(MetricDefinitionResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAttribute(Long userId, Long sensorId, String metricKey) {
        SensorAttribute attribute = attributeRepository.findBySensorSensorIdAndMetricKey(sensorId, metricKey)
                .orElseThrow(() -> new MetricKeyNotFoundException(metricKey));

        // 그 센서가 속한 그룹의 매니저 이상만 삭제 가능
        validateManagerRole(userId, attribute.getSensor().getGroup().getGroupId());

        attributeRepository.deleteBySensorSensorIdAndMetricKey(sensorId, metricKey);
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
        return metricDefinitionRepository.findByMetricKeyIgnoreCase(metricKey)
                .map(def -> attributeRepository.existsBySensorSensorIdAndMetricKey(sensorId, def.getMetricKey()))
                .orElse(false);
    }
}