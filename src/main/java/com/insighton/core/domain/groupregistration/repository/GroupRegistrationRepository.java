package com.insighton.core.domain.groupregistration.repository;

import com.insighton.core.domain.groupregistration.entity.GroupRegistration;
import com.insighton.core.domain.groupregistration.entity.GroupRegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRegistrationRepository extends JpaRepository<GroupRegistration, Long> {

    // admin
    Page<GroupRegistration> findAllByStatus(GroupRegistrationStatus status, Pageable pageable);

    // requester
    Page<GroupRegistration> findAllByRequesterId(Long requesterId, Pageable pageable);

    // validate
    boolean existsByRequesterIdAndStatus(Long requesterId, GroupRegistrationStatus status);
}
