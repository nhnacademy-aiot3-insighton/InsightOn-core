package com.insighton.core.device_attributes.repository;

import com.insighton.core.device_attributes.entity.DeviceAttributeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * {@link DeviceAttributeEntity} 엔티티에 대한 데이터베이스 접근을 담당하는 JPA Repository 인터페이스.
 */
public interface DeviceAttributeRepository extends JpaRepository<DeviceAttributeEntity, Long> {

    /**
     * 특정 기기 ID(deviceId)에 속한 모든 디바이스 속성 목록을 조회합니다.
     *
     * @param deviceId 조회할 장치 ID
     * @return 해당 장치가 보유한 속성 엔티티 리스트
     */
    List<DeviceAttributeEntity> findByDeviceId_DeviceId(Long deviceId);

    /**
     * 특정 기기 ID와 메트릭 키(metricKey) 조합에 해당하는 단일 속성을 조회합니다.
     * <p>
     * (deviceId, metricKey) 복합 UNIQUE 제약 조건을 활용한 정밀 조회에 사용됩니다.
     *
     * @param deviceId 장치 ID
     * @param metricKey 메트릭 키 (ex. "co2", "temperature", "power_status")
     * @return 속성 엔티티 Optional 객체
     */
    Optional<DeviceAttributeEntity> findByDeviceId_DeviceIdAndMetricKey(Long deviceId, String metricKey);

    /**
     * 특정 기기에 해당 메트릭 키 속성이 등록되어 존재하는지 여부를 확인합니다.
     *
     * @param deviceId 장치 ID
     * @param metricKey 메트릭 키
     * @return 존재할 경우 true, 그렇지 않으면 false
     */
    boolean existsByDeviceId_DeviceIdAndMetricKey(Long deviceId, String metricKey);

    void deleteByDeviceId_DeviceId(Long deviceId);

    void deleteByGroupId_GroupId(Long groupIdGroupId);

}