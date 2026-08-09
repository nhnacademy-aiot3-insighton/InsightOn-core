package com.insighton.core.usecase;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groupregistration.presentation.GroupRegistrationResponse;
import com.insighton.core.domain.groupregistration.service.GroupRegistrationService;
import com.insighton.core.domain.groups.dto.request.GroupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GroupRegistrationApprovalUseCase {

    private final GroupRegistrationService groupRegistrationService;
    private final GroupUseCase groupUseCase;

    @Transactional
    public void approve(String userRole, Long groupRegistrationId, Long approverId) {
        GroupRegistrationResponse groupRegistrationResponse = groupRegistrationService.approveGroupRegistration(userRole, groupRegistrationId, approverId);

        GroupRequest groupRequest = new GroupRequest(
                groupRegistrationResponse.groupName(),
                groupRegistrationResponse.description(),
                groupRegistrationResponse.groupRegion()
        );
        groupUseCase.createGroup(groupRequest, groupRegistrationResponse.requesterId());
    }
}
