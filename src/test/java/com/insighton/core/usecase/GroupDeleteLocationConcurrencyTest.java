package com.insighton.core.usecase;

import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.repository.GroupMemberRepository;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.exception.GroupNotFoundException;
import com.insighton.core.domain.groups.repository.GroupRepository;
import com.insighton.core.domain.groups.service.GroupService;
import com.insighton.core.domain.location.dto.request.LocationCreateRequest;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.repository.LocationRepository;
import com.insighton.core.domain.region.loader.RegionCsvLoader;
import com.insighton.core.domain.widgets.repository.InfluxDbRepository;
import com.insighton.core.usecase.group.GroupDeleteUseCase;
import com.insighton.core.usecase.location.LocationCreateUseCase;
import com.insighton.core.usecase.location.LocationDeleteUseCase;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
class GroupDeleteLocationConcurrencyTest {

    private final Long userId = 100L;
    private final String inviteToken = "token-123";

    @Autowired
    private GroupDeleteUseCase groupDeleteUseCase;
    @Autowired
    private LocationDeleteUseCase locationDeleteUseCase;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    @Autowired
    private LocationRepository locationRepository;

    @MockitoSpyBean
    private GroupService groupService;

    @Autowired
    private LocationCreateUseCase locationCreateUseCase;

    @MockitoBean
    private InfluxDbRepository influxDbRepository;
    @MockitoBean
    private RegionCsvLoader regionCsvLoader;

    private Long groupId;

    @BeforeEach
    void setUp() {
        locationRepository.deleteAll();
        groupMemberRepository.deleteAll();
        groupRepository.deleteAll();

        // 1. Group 생성
        Group group = Group.builder()
                .name("Group-Concurrency")
                .description("Desc")
                .groupRegion("Region")
                .inviteToken(inviteToken)
                .build();
        Group savedGroup = groupRepository.save(group);
        groupId = savedGroup.getGroupId();

        // 2. GroupMember (SUPER_MANAGER) 생성
        GroupMember member = GroupMember.builder()
                .group(savedGroup)
                .userId(userId)
                .groupRole(GroupMember.GroupRole.SUPER_MANAGER)
                .build();
        groupMemberRepository.save(member);
    }

    @Test
    @DisplayName("그룹 삭제와 위치 생성이 동시에 요청될 때 그룹 비관적 락으로 직렬화되어 데이터 정합성 유지")
    void groupDelete_and_locationCreate_concurrency_pessimisticLock() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch lockAcquiredLatch = new CountDownLatch(1);

        // 그룹 삭제 요청이 Group findWithLockByGroupId(SELECT FOR UPDATE)를 호출하여 락을 잡은 직후 Latch 신호 전송
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            lockAcquiredLatch.countDown();
            return result;
        }).when(groupService).findWithLockByGroupId(groupId);

        // when
        // 1. 그룹 삭제 요청 제출 -> 그룹 락 선점 시도
        Future<Void> deleteFuture = executorService.submit(() -> {
            groupDeleteUseCase.deleteGroup(userId, groupId, inviteToken);
            return null;
        });

        // 2. 그룹 삭제 요청이 그룹 DB 락을 잡을 때까지 명시적 대기
        lockAcquiredLatch.await();

        // 3. 그룹 락이 선점된 상태에서 위치 생성 요청 제출 -> 그룹 비관적 락 대기(Lock Wait) 진입
        LocationCreateRequest createRequest = new LocationCreateRequest("New-Location", Location.AutoControlMode.SUGGESTION);
        Future<Void> createFuture = executorService.submit(() -> {
            locationCreateUseCase.createLocation(userId, groupId, createRequest);
            return null;
        });

        // then
        // 1. 그룹 삭제 요청은 성공 완료
        try {
            deleteFuture.get();
        } catch (ExecutionException ignored) {
            log.error("에러터짐");
        }

        // 2. 위치 생성 요청은 락 대기 후 늦게 진입하지만 그룹이 삭제되었으므로 GroupNotFoundException 발생
        boolean locationCreateFailedAsExpected = false;
        try {
            createFuture.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof GroupNotFoundException) {
                locationCreateFailedAsExpected = true;
            }
        }

        executorService.shutdown();

        // 3. 최종적으로 그룹이 삭제되어 위치 잔재 데이터가 없어야 함
        List<Location> remainingLocations = locationRepository.findByGroupGroupId(groupId);
        assertThat(remainingLocations).isEmpty();
        assertThat(locationCreateFailedAsExpected).isTrue();
    }
}
