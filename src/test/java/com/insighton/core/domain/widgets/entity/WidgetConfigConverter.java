package com.insighton.core.domain.widgets.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 테스트 코드 영역(src/test) 전용 JPA AttributeConverter.
 * 운영 코드(src/main)는 100% 순수하게 유지하면서 H2 test.sql 역직렬화를 지원합니다.
 */
@Converter(autoApply = true)
public class WidgetConfigConverter implements AttributeConverter<WidgetConfig, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(WidgetConfig attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize WidgetConfig in test", e);
        }
    }

    @Override
    public WidgetConfig convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            String jsonStr = dbData.trim();
            if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
                jsonStr = OBJECT_MAPPER.readValue(jsonStr, String.class);
            }
            return OBJECT_MAPPER.readValue(jsonStr, WidgetConfig.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize WidgetConfig in test: " + dbData, e);
        }
    }
}
