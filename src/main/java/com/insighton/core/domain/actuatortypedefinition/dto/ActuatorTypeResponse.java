package com.insighton.core.domain.actuatortypedefinition.dto;

import com.insighton.core.domain.actuatortypedefinition.entity.ActuatorTypeDefinition;

// 액추에이터 종류 조회 응답 DTO
public record ActuatorTypeResponse (
        String typeCode,
        String typeName,
        String description
){
    public static ActuatorTypeResponse from(ActuatorTypeDefinition entity){
        return new ActuatorTypeResponse(entity.getTypeCode(),
                entity.getTypeName(),
                entity.getDescription());
    }
}
