package com.insighton.core.entity.groups;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
//@Table(name = "groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Groups {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String location;

    @Column(unique = true, nullable = false)
    private String inviteToken;

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private LocalDateTime createdAt;

    /**
     * @Builder 패턴을 적용한 생성자
     * - 엔티티 최초 생성 시 필요한 필드만 깔끔하게 빌더로 넘겨받아 안전하게 객체를 생성합니다.
     * - PK인 groupId와 자동으로 들어갈 createdAt은 빌더 대상에서 제외하여 객체 오용을 방지합니다.
     */
    @Builder
    public Groups(String name, String description, String location, String inviteToken) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.inviteToken = inviteToken;
    }

    /**
     * JPA 영속화(DB Insert) 직전에 실행되어 자동으로 생성 일시를 주입합니다.
     * 매번 Service 레이어에서 일일이 LocalDateTime.now()를 세팅해줄 필요가 없어 편리합니다.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * [비즈니스 메서드 - 초대 토큰 재발급(로테이션)]
     * - 엔티티의 불필요한 setter 사용을 지양하고, 비즈니스 목적을 명확히 한 로테이션 메서드입니다.
     */
    public void rotateInviteToken(String newInviteToken) {
        if (newInviteToken == null || newInviteToken.isBlank()) {
            throw new IllegalArgumentException("새로운 초대 토큰은 비어있을 수 없습니다.");
        }
        this.inviteToken = newInviteToken;
    }
}
