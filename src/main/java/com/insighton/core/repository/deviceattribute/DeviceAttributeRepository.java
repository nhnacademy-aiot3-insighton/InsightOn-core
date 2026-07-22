package com.insighton.core.repository.deviceattribute;

import com.insighton.core.entity.deviceAttribute.DeviceAttributeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceAttributeRepository extends JpaRepository<DeviceAttributeEntity, Long> {

    List<DeviceAttributeEntity> findByDeviceId_DeviceId(Long deviceId);

    Optional<DeviceAttributeEntity> findByDeviceId_DeviceIdAndMetricKey(Long deviceId, String metricKey);

}
