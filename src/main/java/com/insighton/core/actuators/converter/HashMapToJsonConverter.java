package com.insighton.core.actuators.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.actuators.exception.CouldNotConvertMapToJson;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter(autoApply = true)
public class HashMapToJsonConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new CouldNotConvertMapToJson("Could not convert map to json string." + e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            String cleanData = dbData.trim();

            // 1. 이스케이프된 문자열 처리 ("{\"power\":\"OFF\"}" -> {"power":"OFF"})
            if (cleanData.startsWith("\"") && cleanData.endsWith("\"")) {
                cleanData = cleanData.substring(1, cleanData.length() - 1);
            }
            cleanData = cleanData.replace("\\\"", "\"");

            // 2. 홑따옴표로 감싸진 경우 처리
            if (cleanData.startsWith("'") && cleanData.endsWith("'")) {
                cleanData = cleanData.substring(1, cleanData.length() - 1).trim();
            }

            return objectMapper.readValue(cleanData, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new CouldNotConvertMapToJson("Could not convert json string to map: " + dbData + " / " + e);
        }
    }
}