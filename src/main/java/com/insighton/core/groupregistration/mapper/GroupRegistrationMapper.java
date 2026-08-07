package com.insighton.core.groupregistration.mapper;

import com.insighton.core.groupregistration.dto.GroupRegistrationResponse;
import com.insighton.core.groupregistration.entity.GroupRegistration;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GroupRegistrationMapper {
    GroupRegistrationResponse toResponse(GroupRegistration entity);
}
