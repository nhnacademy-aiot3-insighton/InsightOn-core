package com.insighton.core.groupregistration.usercase;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.groupregistration.dto.GroupRegistrationResponse;
import com.insighton.core.groupregistration.service.GroupRegistrationService;
import com.insighton.core.groups.dto.request.GroupsRequest;
import com.insighton.core.groups.service.GroupManagementUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GroupRegistrationApprovalUseCase {

    private final GroupRegistrationService groupRegistrationService;
    private final GroupManagementUseCase groupManagementUseCase;

    @Transactional
    public void approve(Long groupRegistrationId, Long approverId) {
        groupRegistrationService.approveGroupRegistration(groupRegistrationId, approverId);

        GroupRegistrationResponse groupRegistrationResponse = groupRegistrationService.getGroupRegistration(groupRegistrationId);

        GroupsRequest groupsRequest = new GroupsRequest(
                groupRegistrationResponse.groupName(),
                groupRegistrationResponse.description(),
                groupRegistrationResponse.groupRegion()
        );
        groupManagementUseCase.createGroup(groupsRequest, groupRegistrationResponse.requesterId());
    }
}
