package com.insighton.core.device_attributes.repository;

import com.insighton.core.device_attributes.entity.DeviceAttributeEntity;
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

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/device-attribute-test.sql")
class DeviceAttributeRepositoryTest {

    @Autowired
    private DeviceAttributeRepository attributeRepository;

    @Test
    @DisplayName("findByDeviceId_DeviceId - 기기 소속 속성 전체 조회 성공")
    void 기기속성_전체조회() {
        List<DeviceAttributeEntity> result = attributeRepository.findByDeviceId_DeviceId(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByDeviceId_DeviceIdAndMetricKey - 복합 조건 단건 조회 성공")
    void 기기와_메트릭키로_단건조회() {
        Optional<DeviceAttributeEntity> found =
                attributeRepository.findByDeviceId_DeviceIdAndMetricKey(1L, "humidity");

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("deleteByDeviceId_DeviceId - 기기 삭제 시 속성 일괄 삭제 성공")
    void 기기ID로_일괄삭제() {
        attributeRepository.deleteByDeviceId_DeviceId(1L);

        assertThat(attributeRepository.findByDeviceId_DeviceId(1L)).isEmpty();
    }

    @Test
    @DisplayName("deleteByGroupId_GroupId - 그룹 삭제 시 속성 일괄 삭제 성공")
    void 그룹ID로_일괄삭제() {
        attributeRepository.deleteByGroupId_GroupId(1L);

        assertThat(attributeRepository.findByDeviceId_DeviceId(1L)).isEmpty();
    }
}