package com.insighton.core.domain.groupregistration.service;

import com.insighton.core.domain.groupregistration.dto.CreateGroupRegistrationRequest;
import com.insighton.core.domain.groupregistration.dto.GroupRegistrationResponse;
import com.insighton.core.domain.groupregistration.entity.GroupRegistration;
import com.insighton.core.domain.groupregistration.entity.GroupRegistrationStatus;
import com.insighton.core.domain.groupregistration.exception.AlreadyRequestedException;
import com.insighton.core.domain.groupregistration.exception.GroupRegistrationNotFoundException;
import com.insighton.core.domain.groupregistration.mapper.GroupRegistrationMapper;
import com.insighton.core.domain.groupregistration.repository.GroupRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GroupRegistrationService {

    private final GroupRegistrationRepository groupRegistrationRepository;
    private final GroupRegistrationMapper groupRegistrationMapper;

    @Transactional
    public GroupRegistrationResponse createRequest(Long requesterId, CreateGroupRegistrationRequest request) {
        validateNoPendingRequest(requesterId);
        GroupRegistration groupRegistration = GroupRegistration.create(requesterId, request.groupName(), request.description(), request.groupRegion());
        try {
            GroupRegistration saved = groupRegistrationRepository.save(groupRegistration);
            groupRegistrationRepository.flush();
            return groupRegistrationMapper.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw AlreadyRequestedException.of();
        }
    }

    // admin
    @Transactional(readOnly = true)
    public Page<GroupRegistrationResponse> getGroupRegistrations(GroupRegistrationStatus status, Pageable pageable) {
        Page<GroupRegistration> registrations;
        if (Objects.isNull(status)) {
            registrations = groupRegistrationRepository.findAll(pageable);
        } else {
            registrations = groupRegistrationRepository.findAllByStatus(status, pageable);
        }
        return registrations.map(groupRegistrationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<GroupRegistrationResponse> getMyGroupRegistrations(Long requesterId, Pageable pageable) {
        Page<GroupRegistration> registrations = groupRegistrationRepository.findAllByRequesterId(requesterId, pageable);
        return registrations.map(groupRegistrationMapper::toResponse);
    }

    @Transactional
    public void cancelGroupRegistration(Long groupRegistrationId, Long requesterId) {
        GroupRegistration groupRegistration = getGroupRegistrationOrThrow(groupRegistrationId);
        groupRegistration.cancel(requesterId);
    }

    @Transactional
    public void approveGroupRegistration(Long groupRegistrationId, Long approverId) {
        GroupRegistration groupRegistration = getGroupRegistrationOrThrow(groupRegistrationId);
        groupRegistration.approve(approverId);
    }

    @Transactional
    public void rejectGroupRegistration(Long groupRegistrationId, Long approverId) {
        GroupRegistration groupRegistration = getGroupRegistrationOrThrow(groupRegistrationId);
        groupRegistration.reject(approverId);
    }

    @Transactional
    public GroupRegistrationResponse getGroupRegistration(Long groupRegistrationId) {
        GroupRegistration groupRegistration = getGroupRegistrationOrThrow(groupRegistrationId);
        return groupRegistrationMapper.toResponse(groupRegistration);
    }

    private void validateNoPendingRequest(Long requesterId) {
        if (groupRegistrationRepository.existsByRequesterIdAndStatus(requesterId, GroupRegistrationStatus.PENDING)) {
            throw AlreadyRequestedException.of();
        }
    }

    private GroupRegistration getGroupRegistrationOrThrow(Long groupRegistrationId) {
        return groupRegistrationRepository.findByGroupRegistrationId(groupRegistrationId)
                .orElseThrow(GroupRegistrationNotFoundException::new);
    }
}
