package com.insighton.core.device.repository;

import com.insighton.core.device.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * IoT 장치({@link DeviceEntity}) 데이터베이스 접근을 담당하는 JPA Repository 인터페이스.
 */
public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {

    /**
     * 특정 게이트웨이에 연결된 모든 장치 목록을 조회합니다.
     *
     * @param gatewaysId 게이트웨이 식별 ID
     * @return 해당 게이트웨이를 거치는 장치 엔티티 리스트
     */
    List<DeviceEntity> findByGatewaysId(Long gatewaysId);

    /**
     * 특정 장소(오피스 구역)에 설치된 모든 장치 목록을 조회합니다.
     *
     * @param locationsId 장소/구역 식별 ID
     * @return 해당 구역에 위치한 장치 엔티티 리스트
     */
    List<DeviceEntity> findByLocationsId(Long locationsId);

    /**
     * 장치명(name)으로 장치 목록을 조회합니다.
     *
     * @param name 검색할 장치 이름
     * @return 일치하는 이름을 가진 장치 엔티티 리스트
     */
    List<DeviceEntity> findByName(String name);

    /**
     * 장치 타입(SENSOR, AIRCON, VENTILATION_FAN 등)별로 장치 목록을 조회합니다.
     *
     * @param type 장치 대분류 스펙 분류
     * @return 해당 타입의 장치 엔티티 리스트
     */
    List<DeviceEntity> findByType(String type);

    /**
     * LoRaWAN 하드웨어 고유 식별 코드(DevEUI)로 단일 장치를 조회합니다.
     * <p>
     * MQTT/ChirpStack 센서 패킷 수신 시 패킷 헤더의 DevEUI와 매핑하는 용도로 사용됩니다.
     *
     * @param deviceEui 하드웨어 고유 식별 문자열 (DevEUI)
     * @return 해당 DevEUI를 가지는 장치 엔티티 Optional 객체
     */
    Optional<DeviceEntity> findByDeviceEui(String deviceEui);

}