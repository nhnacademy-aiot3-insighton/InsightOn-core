package com.insighton.core.sensor_attributes.repository;

import com.insighton.core.sensor_attributes.entity.SensorAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * {@link SensorAttribute} 엔티티에 대한 데이터베이스 접근을 담당하는 JPA Repository 인터페이스.
 */
public interface SensorAttributeRepository extends JpaRepository<SensorAttribute, Long> {

    /**
     * 특정 기기 ID(sensorId)에 속한 모든 센서 속성 목록을 조회합니다.
     *
     * @param sensorId 조회할 장치 ID
     * @return 해당 장치가 보유한 속성 엔티티 리스트
     */
    List<SensorAttribute> findBySensorSensorId(Long sensorId);

    /**
     * 특정 기기 ID와 메트릭 키(metricKey) 조합에 해당하는 단일 속성을 조회합니다.
     * <p>
     * (sensorId, metricKey) 복합 UNIQUE 제약 조건을 활용한 정밀 조회에 사용됩니다.
     *
     * @param sensorId 장치 ID
     * @param metricKey 메트릭 키 (ex. "co2", "temperature", "power_status")
     * @return 속성 엔티티 Optional 객체
     */
    Optional<SensorAttribute> findBySensorSensorIdAndMetricKey(Long sensorId, String metricKey);

    /**
     * 특정 기기에 해당 메트릭 키 속성이 등록되어 존재하는지 여부를 확인합니다.
     *
     * @param sensorId 장치 ID
     * @param metricKey 메트릭 키
     * @return 존재할 경우 true, 그렇지 않으면 false
     */
    boolean existsBySensorSensorIdAndMetricKey(Long sensorId, String metricKey);

    // 기기 삭제 시 소속 속성 일괄 삭제
    void deleteBySensorSensorId(Long sensorId);

    // 장소 삭제 시, 그 장소 소속 센서들의 속성 일괄 삭제용
    void deleteAllBySensorSensorIdIn(List<Long> sensorIds);

    // 특정 센서의 특정 메트릭 속성 하나만 삭제 - (sensorId, metricKey) 유니크 조합으로 정확히 그 행 하나만 지정
    void deleteBySensorSensorIdAndMetricKey(Long sensorId, String metricKey);
}