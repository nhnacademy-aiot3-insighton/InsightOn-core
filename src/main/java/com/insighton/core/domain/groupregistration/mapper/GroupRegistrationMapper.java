package com.insighton.core.domain.groupregistration.mapper;

import com.insighton.core.domain.groupregistration.dto.GroupRegistrationResponse;
import com.insighton.core.domain.groupregistration.entity.GroupRegistration;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GroupRegistrationMapper {
    GroupRegistrationResponse toResponse(GroupRegistration entity);
}
