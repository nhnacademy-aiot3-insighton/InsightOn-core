package com.insighton.core.sensor_attributes.repository;

import com.insighton.core.domain.sensorattributes.entity.SensorAttribute;
import com.insighton.core.domain.sensorattributes.repository.SensorAttributeRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/sensor-attribute-test.sql")
class SensorAttributeRepositoryTest {

    @Autowired
    private SensorAttributeRepository attributeRepository;

    @Test
    @DisplayName("findBySensorIdSensorId - 기기 소속 속성 전체 조회 성공")
    void 기기속성_전체조회() {
        List<SensorAttribute> result = attributeRepository.findBySensorSensorId(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findBySensorIdSensorIdAndMetricKey - 복합 조건 단건 조회 성공")
    void 기기와_메트릭키로_단건조회() {
        Optional<SensorAttribute> found =
                attributeRepository.findBySensorSensorIdAndMetricKey(1L, "humidity");

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("deleteBySensorIdSensorId - 기기 삭제 시 속성 일괄 삭제 성공")
    void 기기ID로_일괄삭제() {
        attributeRepository.deleteBySensorSensorId(1L);

        assertThat(attributeRepository.findBySensorSensorId(1L)).isEmpty();
    }

    // FIXME: sensor_attributes의 current_value_str / group_id 컬럼 제거로
    // SensorAttributeResponse(4-arg) / updateActuatorValue / deleteByGroupIdGroupId 가 사라져 컴파일 불가.
    // 액추에이터 제어가 actuators 테이블 기준으로 재설계되면 다시 살릴 것.
//    void 그룹ID로_일괄삭제() {
//        attributeRepository.deleteByGroupIdGroupId(1L);
//
//        assertThat(attributeRepository.findBySensorSensorId(1L)).isEmpty();
//    }
}