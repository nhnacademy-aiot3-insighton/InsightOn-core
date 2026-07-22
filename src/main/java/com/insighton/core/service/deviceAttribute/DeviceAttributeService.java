package com.insighton.core.service.deviceAttribute;

import com.insighton.core.deviceAttribute.core.MetricDefinition;
import com.insighton.core.dto.deviceattribute.DeviceAttributeDto;
import com.insighton.core.entity.deviceAttribute.DeviceAttributeEntity;
import com.insighton.core.error.NoDeviceId;
import com.insighton.core.error.NoMetricKey;
import com.insighton.core.repository.device.DeviceRepository;
import com.insighton.core.repository.deviceattribute.DeviceAttributeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceAttributeService {

    private final DeviceAttributeRepository attributeRepository;
    private final DeviceRepository repository;

    //기기의 모든 속성 가져오기
    public List<DeviceAttributeDto> getAllAttributeByDeviceId(Long deviceId){

        // 기기가 존재하는지 확인
        if(!repository.existsById(deviceId)){
            throw new NoDeviceId(deviceId + "기기가 없음");
        }

        // 기기 ID로 속성 리스트를 DB에서 찾아와서 DTO로 변환
        return attributeRepository.findByDeviceId_DeviceId(deviceId)
                .stream()
                .map(attr -> {
                    // enum에서 하드코딩된 한글 명칭과 단위를 가져와 DTO에 조합
                    MetricDefinition metricDefinition = MetricDefinition.fromKey(attr.getMetricKey());

                        return new DeviceAttributeDto(
                        attr.getMetricKey(),
                        metricDefinition.getMetricName(),
                        metricDefinition.getUnit(),
                        attr.getCurrentValueStr()
                        );
                }).toList();
    }

    // 액추에이터 current_value_str 갱신 로직
    @Transactional
    public void updateActuatorValue(Long deviceId, String metricKey, String newValue){
        DeviceAttributeEntity attribute = attributeRepository.findByDeviceId_DeviceIdAndMetricKey(deviceId, metricKey)
                .orElseThrow(() -> new NoDeviceId(deviceId + "가 없음"));

        attribute.updateCurrentValue(newValue);
    }

    // 센서 패킷 인입 시 마스터 정보 검증
    public boolean isValidDeviceAttribute(Long deviceId, String metricKey){
        return attributeRepository.existsByDeviceId_DeviceIdAndMetricKey(deviceId, metricKey);
    }


}
