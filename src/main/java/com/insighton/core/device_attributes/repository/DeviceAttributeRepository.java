package com.insighton.core.device_attributes.repository;

import com.insighton.core.device_attributes.entity.DeviceAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * {@link DeviceAttribute} 엔티티에 대한 데이터베이스 접근을 담당하는 JPA Repository 인터페이스.
 */
public interface DeviceAttributeRepository extends JpaRepository<DeviceAttribute, Long> {

    /**
     * 특정 기기 ID(deviceId)에 속한 모든 디바이스 속성 목록을 조회합니다.
     *
     * @param deviceId 조회할 장치 ID
     * @return 해당 장치가 보유한 속성 엔티티 리스트
     */
    List<DeviceAttribute> findByDeviceIdDeviceId(Long deviceId);

    /**
     * 특정 기기 ID와 메트릭 키(metricKey) 조합에 해당하는 단일 속성을 조회합니다.
     * <p>
     * (deviceId, metricKey) 복합 UNIQUE 제약 조건을 활용한 정밀 조회에 사용됩니다.
     *
     * @param deviceId 장치 ID
     * @param metricKey 메트릭 키 (ex. "co2", "temperature", "power_status")
     * @return 속성 엔티티 Optional 객체
     */
    Optional<DeviceAttribute> findByDeviceIdDeviceIdAndMetricKey(Long deviceId, String metricKey);

    /**
     * 특정 기기에 해당 메트릭 키 속성이 등록되어 존재하는지 여부를 확인합니다.
     *
     * @param deviceId 장치 ID
     * @param metricKey 메트릭 키
     * @return 존재할 경우 true, 그렇지 않으면 false
     */
    boolean existsByDeviceIdDeviceIdAndMetricKey(Long deviceId, String metricKey);

    // 기기 삭제 시 소속 속성 일괄 삭제
    void deleteByDeviceIdDeviceId(Long deviceId);

    // 그룹 삭제 시 소속 속성 일괄 삭제
    void deleteByGroupIdGroupId(Long groupIdGroupId);

    // 장소 삭제 시, 그 장소 소속 디바이스들의 속성 일괄 삭제용
    void deleteAllByDeviceIdDeviceIdIn(List<Long> deviceIds);

}