package com.insighton.core.service.deviceAttribute;

import com.insighton.core.dto.deviceAttribute.DeviceAttributeDto;
import com.insighton.core.error.NoDeviceId;
import com.insighton.core.repository.device.DeviceRepository;
import com.insighton.core.repository.deviceAttribute.DeviceAttributeRepository;
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
                .map(attr -> new DeviceAttributeDto(
                        attr.getMetricKey(),
                        attr.getDisplayName(),
                        attr.getUnit(),
                        attr.getCurrentValueStr()
                )).toList();
    }
}
