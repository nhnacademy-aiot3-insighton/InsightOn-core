package com.insighton.core.device_attributes.service.impl;

import com.insighton.core.exception.CustomException;
import com.insighton.core.exception.ErrorCode;
import com.insighton.core.device_attributes.dto.DeviceAttribute;
import com.insighton.core.device_attributes.entity.DeviceAttributeEntity;
import com.insighton.core.device_attributes.entity.MetricDefinition;
import com.insighton.core.device_attributes.repository.DeviceAttributeRepository;
import com.insighton.core.device_attributes.service.DeviceAttributeService;
import com.insighton.core.sensors.entity.DeviceEntity;
import com.insighton.core.sensors.entity.DeviceType;
import com.insighton.core.sensors.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@link DeviceAttributeService}의 기본 JPA 구현체.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceAttributeServiceImpl implements DeviceAttributeService {

    private final DeviceAttributeRepository attributeRepository;
    private final DeviceRepository deviceRepository;

    /**
     * 특정 기기에 정의된 모든 속성(메트릭) 목록을 조회합니다.
     * <p>
     * DB의 {@code metricKey} 정보를 기반으로 Enum({@link MetricDefinition})에서 하드코딩된
     * 한글 명칭({@code metricName}) 및 단위({@code unit}) 정보를 추출하여 클라이언트용 DTO로 조합합니다.
     *
     * @param deviceId 조회할 기기 ID
     * @return 기기의 메트릭 정보 및 상태값이 포함된 DTO 리스트
     * @throws CustomException 전달받은 deviceId가 DB에 존재하지 않을 경우 던짐
     */
    public List<DeviceAttribute> getAllAttributeByDeviceId(Long deviceId){

        // 1. 해당 장치의 존재 유무 검증
        if(!deviceRepository.existsById(deviceId)){
            throw new CustomException(ErrorCode.DEVICE_NOT_FOUND);
        }

        // 2. DB에서 장치 속성 목록 조회 후 Enum 정보를 매핑하여 DTO로 변환
        return attributeRepository.findByDeviceId_DeviceId(deviceId)
                .stream()
                .map(attr -> {
                    // Enum에서 메트릭 표준 정의(한글 명칭, 단위) 바인딩
                    MetricDefinition metricDefinition = MetricDefinition.fromKey(attr.getMetricKey());

                    return new DeviceAttribute(
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
     * @param deviceId 대상 액추에이터 기기 ID
     * @param metricKey 변경 대상 메트릭 키 (ex. "power_status", "ac_mode")
     * @param newValue 변경하고자 하는 상태/수치값 (ex. "ON", "OFF", "24")
     * @throws CustomException 지정한 기기 ID 및 메트릭 키 조건의 속성을 찾을 수 없을 경우 발생
     */
    @Transactional
    public void updateActuatorValue(Long deviceId, String metricKey, String newValue){

        // existsById 대신 findById로 직접 조회
        // DB조회 1번으로 줄이기
        DeviceEntity entity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));

//        if(!deviceRepository.existsById(deviceId)){
//            throw new CustomException(ErrorCode.DEVICE_NOT_FOUND);
//        }

        // 센서타입인 경우 제어 API를 통한 수치 변경을 거부
        if(entity.getDeviceType() != DeviceType.ACTUATOR){
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE,"센서 장치의 수집데이터는 제어 API로 수정 불가");
        }

        // 변경할 수치 값 유효성 검사
        if(newValue == null || newValue.trim().isEmpty()){
            throw new CustomException(ErrorCode.NO_NEW_ACTUATOR_VALUE);
        }

        /**
         * 코더레빗 제안
         * 대소문자 불일치로 인한 404 방지
         * 정규화된 메트릭 키로 조회하세요. MetricDefinition.fromKey(metricKey)는 대소문자를 무시하지만, 바로 아래 조회는 원본 metricKey를 사용합니다.
         * CO2처럼 대문자로 들어온 값은 검증을 통과해도 co2로 저장된 행을 못 찾을 수 있으니
         * MetricDefinition.fromKey(metricKey).getMetricKey()를 조회 인자로 쓰세요.
         */
        // MetricDefinition에서 정규화된 표준 metricKey(예: "co2") 추출
        MetricDefinition definition = MetricDefinition.fromKey(metricKey);
        String normalizedMetricKey = definition.getMetricKey();

        // 정규화된 metricKey로 DB 조회
        DeviceAttributeEntity attribute = attributeRepository
                .findByDeviceId_DeviceIdAndMetricKey(deviceId, normalizedMetricKey)
                .orElseThrow(() -> new CustomException(ErrorCode.METRIC_KEY_NOT_FOUND));

        attribute.updateCurrentValue(newValue);
    }

    /**
     * 외부 수집 엔진/MQTT 등을 통해 입력된 센서 패킷이 유효한 기기 속성인지 검증합니다.
     *
     * @param deviceId 장치 ID
     * @param metricKey 메트릭 키
     * @return 해당 기기에 메트릭 키가 정상 등록되어 있으면 true, 없으면 false
     */
    // 등록되지 않는 메트릭키가 전달되어도 예외가 터지지않고 false 반환
    // 대소문자 정규화
    public boolean isValidDeviceAttribute(Long deviceId, String metricKey){
        if(deviceId == null || metricKey == null){
            return false;
        }
        return MetricDefinition.findFromKey(metricKey)
                .map(def -> attributeRepository.
                        existsByDeviceId_DeviceIdAndMetricKey(deviceId, def.getMetricKey()))
                .orElse(false);
    }
}