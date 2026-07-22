package com.insighton.core.deviceAttribute.service;

import com.insighton.core.deviceAttribute.dto.DeviceAttributeDto;
import com.insighton.core.deviceAttribute.entity.DeviceAttributeEntity;
import com.insighton.core.deviceAttribute.entity.MetricDefinition;
import com.insighton.core.device.repository.DeviceRepository;
import com.insighton.core.deviceAttribute.error.CustomException;
import com.insighton.core.deviceAttribute.error.ErrorCode;
import com.insighton.core.deviceAttribute.repository.DeviceAttributeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 장치 속성(DeviceAttribute) 데이터 서빙 및 액추에이터 제어 상태 갱신을 담당하는 서비스 클래스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceAttributeService {

    private final DeviceAttributeRepository attributeRepository;
    private final DeviceRepository repository;

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
    public List<DeviceAttributeDto> getAllAttributeByDeviceId(Long deviceId){

        // 1. 해당 장치의 존재 유무 검증
        if(!repository.existsById(deviceId)){
            throw new CustomException(ErrorCode.DEVICE_NOT_FOUND);
        }

        // 2. DB에서 장치 속성 목록 조회 후 Enum 정보를 매핑하여 DTO로 변환
        return attributeRepository.findByDeviceId_DeviceId(deviceId)
                .stream()
                .map(attr -> {
                    // Enum에서 메트릭 표준 정의(한글 명칭, 단위) 바인딩
                    MetricDefinition metricDefinition = MetricDefinition.fromKey(attr.getMetricKey());

                    return new DeviceAttributeDto(
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
        DeviceAttributeEntity attribute = attributeRepository.findByDeviceId_DeviceIdAndMetricKey(deviceId, metricKey)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));

        // 엔티티 내부 메서드를 호출하여 상태값 업데이트 (Dirty Checking 적용)
        attribute.updateCurrentValue(newValue);
    }

    /**
     * 외부 수집 엔진/MQTT 등을 통해 입력된 센서 패킷이 유효한 기기 속성인지 검증합니다.
     *
     * @param deviceId 장치 ID
     * @param metricKey 메트릭 키
     * @return 해당 기기에 메트릭 키가 정상 등록되어 있으면 true, 없으면 false
     */
    public boolean isValidDeviceAttribute(Long deviceId, String metricKey){
        return attributeRepository.existsByDeviceId_DeviceIdAndMetricKey(deviceId, metricKey);
    }
}