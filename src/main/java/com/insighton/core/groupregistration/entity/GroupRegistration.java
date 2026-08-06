package com.insighton.core.groupregistration.entity;

import com.insighton.core.groupregistration.exception.AlreadyProcessedException;
import com.insighton.core.groupregistration.exception.UnauthorizedGroupRegistrationAccessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "group_registrations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_registration_id")
    private Long groupRegistrationId;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "description")
    private String description;

    @Column(name = "group_region", nullable = false)
    private String groupRegion;

    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "approver_id")
    private Long approverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GroupRegistrationStatus status;

    @Column(name = "approved_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime approvedAt;

    public static GroupRegistration create(Long requesterId,
                                           String groupName,
                                           String description,
                                           String groupRegion
    ) {
        GroupRegistration groupRegistration = new GroupRegistration();
        groupRegistration.requesterId = requesterId;
        groupRegistration.groupName = groupName;
        groupRegistration.description = description;
        groupRegistration.groupRegion = groupRegion;
        groupRegistration.createdAt = OffsetDateTime.now(ZoneId.systemDefault());
        groupRegistration.status = GroupRegistrationStatus.PENDING;

        return groupRegistration;
    }

    public void approve(Long approverId) {
        validatePending();
        this.status = GroupRegistrationStatus.APPROVED;
        this.approverId = approverId;
        this.approvedAt = OffsetDateTime.now(ZoneId.systemDefault());
    }

    public void reject(Long approverId) {
        validatePending();
        this.status = GroupRegistrationStatus.REJECTED;
        this.approverId = approverId;
        this.approvedAt = OffsetDateTime.now(ZoneId.systemDefault());
    }

    public void cancel(Long requesterId) {
        if (!this.requesterId.equals(requesterId)) {
            throw new UnauthorizedGroupRegistrationAccessException(requesterId);
        }
        validatePending();
        this.status = GroupRegistrationStatus.CANCELLED;
    }

    private void validatePending() {
        if (status != GroupRegistrationStatus.PENDING) {
            throw AlreadyProcessedException.of(status);
        }
    }
}
