package com.insighton.core.groupregistration.usercase;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.groupregistration.dto.GroupRegistrationResponse;
import com.insighton.core.groupregistration.service.GroupRegistrationService;
import com.insighton.core.groups.dto.request.GroupRequest;
import com.insighton.core.groups.service.CoreManagementUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GroupRegistrationApprovalUseCase {

    private final GroupRegistrationService groupRegistrationService;
    private final CoreManagementUseCase coreManagementUseCase;

    @Transactional
    public void approve(Long groupRegistrationId, Long approverId) {
        groupRegistrationService.approveGroupRegistration(groupRegistrationId, approverId);

        GroupRegistrationResponse groupRegistrationResponse = groupRegistrationService.getGroupRegistration(groupRegistrationId);

        GroupRequest groupRequest = new GroupRequest(
                groupRegistrationResponse.groupName(),
                groupRegistrationResponse.description(),
                groupRegistrationResponse.groupRegion()
        );
        coreManagementUseCase.createGroup(groupRequest, groupRegistrationResponse.requesterId());
    }
}
