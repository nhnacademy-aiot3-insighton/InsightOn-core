package com.insighton.core.domain.actuatortypedefinition.init;

import com.insighton.core.domain.actuatortypedefinition.entity.ActuatorTypeDefinition;
import com.insighton.core.domain.actuatortypedefinition.repository.ActuatorTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// actuatorType이 고정 enum(AIRCON/AIR_PURIFIER/VENTILATION_FAN)에서 DB 카탈로그로 바뀌며 앱을 처음 띄웠을 때
// 시작시 기본 액추에이터 3종 추가
@Component
@RequiredArgsConstructor
@Slf4j
public class ActuatorTypeDefaultSeeder implements ApplicationRunner {

    private static final List<ActuatorTypeDefinition> DEFAULT_TYPES = List.of(
            ActuatorTypeDefinition.builder().typeCode("AIRCON").typeName("에어컨").build(),
            ActuatorTypeDefinition.builder().typeCode("AIR_PURIFIER").typeName("공기청정기").build(),
            ActuatorTypeDefinition.builder().typeCode("VENTILATION_FAN").typeName("환풍기").build()
    );

    private final ActuatorTypeRepository actuatorTypeRepository;

    @Override
    public void run(ApplicationArguments args) {
        DEFAULT_TYPES.stream()
                .filter(defaultType -> !actuatorTypeRepository.existsById(defaultType.getTypeCode()))
                .forEach(defaultType -> {
                    actuatorTypeRepository.save(defaultType);
                    log.info("기본 액추에이터 종류 등록: {}", defaultType.getTypeCode());
                });
    }
}
