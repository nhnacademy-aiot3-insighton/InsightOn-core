package com.insighton.core.domain.groupregistration.repository;

import com.insighton.core.domain.groupregistration.entity.GroupRegistration;
import com.insighton.core.domain.groupregistration.entity.GroupRegistrationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface GroupRegistrationRepository extends JpaRepository<GroupRegistration, Long> {

    // admin
    Page<GroupRegistration> findAllByStatus(GroupRegistrationStatus status, Pageable pageable);

    // requester
    Page<GroupRegistration> findAllByRequesterId(Long requesterId, Pageable pageable);

    // validate
    @Lock(value = LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    boolean existsByRequesterIdAndStatus(Long requesterId, GroupRegistrationStatus status);
}
