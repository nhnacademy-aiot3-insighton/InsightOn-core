package com.insighton.core.entity.device;

import com.insighton.core.entity.deviceAttribute.DeviceAttributeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 시스템에 등록된 센서 및 액추에이터 물리 장치 마스터 정보를 관리하는 JPA 엔티티 클래스.
 */
@Entity
@Table(name = "device")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceEntity {

    /**
     * 장치 고유 식별 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deviceId;

    /**
     * 해당 장치가 무선 패킷을 라우팅하는 상위 게이트웨이 ID (FK)
     */
    private Long gatewaysId;

    /**
     * 장치가 물리적으로 설치된 세부 오피스 구역/장소 ID (FK)
     */
    private Long locationsId;

    /**
     * ChirpStack/LoRaWAN 패킷 파싱 매핑용 하드웨어 고유 식별 문자열 (DevEUI)
     */
    @Column(name = "device_eui", nullable = false, unique = true)
    private String deviceEui;

    /**
     * 현장 관리자가 명명한 장치 이름 (ex. "4층 개발팀 환경 센서")
     */
    @NotNull
    private String name;

    /**
     * 장치 대분류 스펙 (ex. "SENSOR", "AIRCON", "AIR_PURIFIER", "VENTILATION_FAN")
     */
    @NotNull
    private String type;

    /**
     * 가장 최근에 센서 패킷 또는 Heartbeat 신호가 수신된 시각
     */
    private ZonedDateTime lastSeenAt;

    /**
     * 장치 최초 시스템 등록 일시
     */
    private ZonedDateTime createdAt;

    /**
     * 장치의 소속 위치/구역 변경 시 사용하는 엔티티 수정 메서드 (Dirty Checking)
     *
     * @param newLocationId 이동할 새로운 위치 ID
     */
    public void updateLocation(Long newLocationId){
        this.locationsId = newLocationId;
    }

    /**
     * 패킷 수신 시 장치의 마지막 통신 시각을 현재 시각으로 갱신하는 메서드
     */
    public void updateLastSeen(){
        this.lastSeenAt = ZonedDateTime.now();
    }

    /**
     * 장치에 종속된 세부 속성(메트릭) 목록과의 1:N 양방향 연관관계 매핑
     */
    @OneToMany(mappedBy = "deviceId")
    private List<DeviceAttributeEntity> attributeList = new ArrayList<>();

}