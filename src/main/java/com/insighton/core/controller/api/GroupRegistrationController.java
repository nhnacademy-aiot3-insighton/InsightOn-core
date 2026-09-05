package com.insighton.core.controller.api;

import com.insighton.core.controller.swagger.GroupRegistrationControllerApi;
import com.insighton.core.domain.groupregistration.dto.CreateGroupRegistrationRequest;
import com.insighton.core.domain.groupregistration.dto.GroupRegistrationResponse;
import com.insighton.core.domain.groupregistration.entity.GroupRegistrationStatus;
import com.insighton.core.domain.groupregistration.service.GroupRegistrationService;
import com.insighton.core.usecase.groupregistration.GroupRegistrationApprovalUseCase;
import com.insighton.core.usecase.groupregistration.GroupRegistrationCreationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/group-registrations")
public class GroupRegistrationController implements GroupRegistrationControllerApi {

    private final GroupRegistrationService groupRegistrationService;
    private final GroupRegistrationApprovalUseCase approvalUseCase;
    private final GroupRegistrationCreationUseCase groupRegistrationCreationUseCase;

    @Override
    @PostMapping
    public ResponseEntity<GroupRegistrationResponse> createRequest(@RequestHeader("X-User-Id") Long requesterId,
                                                                   @Valid @RequestBody CreateGroupRegistrationRequest registrationRequest
    ) {
        GroupRegistrationResponse registrationResponse = groupRegistrationCreationUseCase.createRequest(requesterId, registrationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationResponse);
    }

    @Override
    @GetMapping
    public ResponseEntity<Page<GroupRegistrationResponse>> getGroupRegistrations(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                                                                 @RequestParam(required = false) GroupRegistrationStatus status,
                                                                                 @PageableDefault(sort = "groupRegistrationId") Pageable pageable
    ) {
        Page<GroupRegistrationResponse> response = groupRegistrationService.getGroupRegistrations(userRole, status, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Override
    @GetMapping("/my")
    public ResponseEntity<Page<GroupRegistrationResponse>> getMyGroupRegistrations(@RequestHeader("X-User-Id") Long requesterId,
                                                                                   @PageableDefault(sort = "groupRegistrationId") Pageable pageable
    ) {
        Page<GroupRegistrationResponse> response = groupRegistrationService.getMyGroupRegistrations(requesterId, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Override
    @GetMapping("/{group-registration-id}")
    public ResponseEntity<GroupRegistrationResponse> getGroupRegistration(@RequestHeader("X-User-Id") Long userId,
                                                                          @RequestHeader(value = "X-User-Role", required = false) String userRole,
                                                                          @PathVariable("group-registration-id") Long groupRegistrationId
    ) {
        GroupRegistrationResponse response = groupRegistrationService.getGroupRegistration(groupRegistrationId, userId, userRole);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Override
    @PutMapping("/{group-registration-id}/cancel")
    public ResponseEntity<Void> cancelGroupRegistration(@RequestHeader("X-User-Id") Long requesterId,
                                                        @PathVariable("group-registration-id") Long groupRegistrationId
    ) {
        groupRegistrationService.cancelGroupRegistration(groupRegistrationId, requesterId);
        return ResponseEntity.ok().build();
    }

    @Override
    @PutMapping("/{group-registration-id}/approve")
    public ResponseEntity<Void> approveGroupRegistration(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                                         @RequestHeader("X-User-Id") Long approverId,
                                                         @PathVariable("group-registration-id") Long groupRegistrationId
    ) {
        approvalUseCase.approve(userRole, groupRegistrationId, approverId);
        return ResponseEntity.ok().build();
    }

    @Override
    @PutMapping("/{group-registration-id}/reject")
    public ResponseEntity<Void> rejectGroupRegistration(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                                        @RequestHeader("X-User-Id") Long approverId,
                                                        @PathVariable("group-registration-id") Long groupRegistrationId
    ) {
        groupRegistrationService.rejectGroupRegistration(userRole, groupRegistrationId, approverId);
        return ResponseEntity.ok().build();
    }
}

