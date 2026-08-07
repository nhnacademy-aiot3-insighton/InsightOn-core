package com.insighton.core.groupmember.entity;

import com.insighton.core.groups.entity.Group;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "group_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupMemberId;
    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩 설정으로 성능 최적화
    @JoinColumn(
            name = "group_id",
            nullable = false)
    private Group group;
    @Column(unique = true, nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private GroupRole groupRole;
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime joinedAt;

    /**
     * {@code @Builder} 패턴을 적용한 생성자
     * - 엔티티 최초 가입/생성 시 필요한 필드(group, usersId, groupRole)만 빌더로 넘겨받아 안전하게 객체를 생성합니다.
     * - DB에서 자동 생성되는 PK(groupMemberId)와 @PrePersist로 자동 설정되는 가입 일자(joinedAt)는 제외하여 실수를 방지합니다.
     */
    @Builder
    public GroupMember(Group group, Long userId, GroupRole groupRole) {
        this.group = group;
        this.userId = userId;
        this.groupRole = groupRole;
    }

    /**
     * JPA 영속화(DB Insert) 직전에 실행되어 자동으로 가입 일시를 주입합니다.
     * 서비스 레이어에서 일일이 LocalDateTime.now()를 주입해줄 필요가 없어 편리하며 누락을 방지합니다.
     */
    @PrePersist
    protected void onJoin() {
        this.joinedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void updateRole(GroupRole newGroupRole) {
        this.groupRole = newGroupRole;
    }

    public boolean isMember() {
        return this.groupRole == GroupRole.MEMBER;
    }

    public boolean isManager() {
        return this.groupRole == GroupRole.MANAGER;
    }

    public boolean isSuperManager() {
        return this.groupRole == GroupRole.SUPER_MANAGER;
    }

    public enum GroupRole {
        MEMBER, MANAGER, SUPER_MANAGER
    }
}
