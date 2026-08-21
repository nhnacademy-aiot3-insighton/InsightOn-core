package com.insighton.core.usecase.dashboard;

import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.repository.DashboardRepository;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.repository.GroupMemberRepository;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.repository.GroupRepository;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.repository.LocationRepository;
import com.insighton.core.domain.region.loader.RegionCsvLoader;
import com.insighton.core.domain.widgets.dto.request.WidgetSaveRequest;
import com.insighton.core.domain.widgets.entity.Widget;
import com.insighton.core.domain.widgets.entity.WidgetConfig;
import com.insighton.core.domain.widgets.exception.AlreadyDashboardSaveException;
import com.insighton.core.domain.widgets.repository.InfluxDbRepository;
import com.insighton.core.domain.widgets.repository.WidgetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
class DashboardSaveUseCaseConcurrencyTest {

    private final Long userId = 100L;
    @Autowired
    private DashboardSaveUseCase dashboardSaveUseCase;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private DashboardRepository dashboardRepository;
    @Autowired
    private WidgetRepository widgetRepository;
    @MockitoSpyBean
    private DashboardService dashboardService;
    @MockitoBean
    private InfluxDbRepository influxDbRepository;
    @MockitoBean
    private RegionCsvLoader regionCsvLoader;
    @MockitoBean
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    @MockitoBean
    private org.springframework.data.redis.core.RedisTemplate<String, com.insighton.core.domain.widgets.entity.WidgetConfig> widgetRedisTemplate;
    @MockitoBean
    private org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory;
    private Long groupId;
    private Long locationId;
    private Long dashboardId;
    private Long widgetIdA;
    private Long widgetIdB;

    @BeforeEach
    void setUp() {
        widgetRepository.deleteAll();
        dashboardRepository.deleteAll();
        locationRepository.deleteAll();
        groupMemberRepository.deleteAll();
        groupRepository.deleteAll();

        // 1. Group 생성
        Group group = Group.builder()
                .name("Concurrency-Group")
                .description("Test Group")
                .groupRegion("Test-Region")
                .inviteToken("token-123")
                .build();
        Group savedGroup = groupRepository.save(group);
        groupId = savedGroup.getGroupId();

        // 2. GroupMember (Super Manager / Admin) 생성
        GroupMember member = GroupMember.builder()
                .group(savedGroup)
                .userId(userId)
                .groupRole(GroupMember.GroupRole.SUPER_MANAGER)
                .build();
        groupMemberRepository.save(member);

        // 3. Location 생성
        Location location = Location.builder()
                .group(savedGroup)
                .locationName("Test-Location")
                .build();
        Location savedLocation = locationRepository.save(location);
        locationId = savedLocation.getLocationId();

        // 4. Dashboard 생성
        Dashboard dashboard = Dashboard.builder()
                .location(savedLocation)
                .title("Concurrency-Dashboard")
                .build();
        Dashboard savedDashboard = dashboardRepository.save(dashboard);
        dashboardId = savedDashboard.getDashboardId();

        // 5. Initial Widgets (Widget A, Widget B) 생성
        Widget widgetA = Widget.builder()
                .dashboard(savedDashboard)
                .xPos(0).yPos(0).width(6).height(4)
                .build();
        Widget widgetB = Widget.builder()
                .dashboard(savedDashboard)
                .xPos(6).yPos(0).width(6).height(4)
                .build();

        Widget savedWidgetA = widgetRepository.save(widgetA);
        Widget savedWidgetB = widgetRepository.save(widgetB);

        widgetIdA = savedWidgetA.getWidgetId();
        widgetIdB = savedWidgetB.getWidgetId();
    }

    @Test
    @DisplayName("동시 대시보드 저장 요청 시 비관적 락으로 인해 요청이 직렬화되어 데이터 정합성 유지")
    void saveDashboard_concurrency_pessimisticLock() throws InterruptedException, ExecutionException {
        // given
        // 요청 1 (User A): Widget A (widgetIdA)만 유지하고 Widget B 삭제 시도
        WidgetSaveRequest requestA = WidgetSaveRequest.builder()
                .widgetId(widgetIdA)
                .xPos(0).yPos(0).width(6).height(4)
                .build();
        List<WidgetSaveRequest> requests1 = List.of(requestA);

        // 요청 2 (User B): Widget B (widgetIdB)만 유지하고 Widget A 삭제 시도
        WidgetSaveRequest requestB = WidgetSaveRequest.builder()
                .widgetId(widgetIdB)
                .xPos(6).yPos(0).width(6).height(4)
                .build();
        List<WidgetSaveRequest> requests2 = List.of(requestB);

        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            CountDownLatch lockAcquiredLatch = new CountDownLatch(1);

            // 요청 A가 getDashboardWithLockByLocationId(SELECT ... FOR UPDATE)를 호출하여 DB 비관적 락을 점유한 직후 Latch 신호 전송
            doAnswer(invocation -> {
                Object result = invocation.callRealMethod();
                lockAcquiredLatch.countDown();
                return result;
            }).when(dashboardService).getDashboardWithLockByLocationId(locationId);

            // when
            // 1. 요청 A (User A) 제출 -> 대시보드 비관적 락 획득 시도
            Future<List<Long>> future1 = executorService.submit(() ->
                    dashboardSaveUseCase.saveDashboard(userId, groupId, locationId, requests1)
            );

            // 2. 요청 A가 DB 비관적 락을 획득할 때까지 명시적 대기 (요청 A의 락 선점 결정적 보장)
            lockAcquiredLatch.await();

            // 3. 요청 A가 DB 락을 선점한 상태에서 요청 B (User B) 제출 -> 요청 B는 DB 비관적 락 대기(Lock Wait) 진입
            Future<List<Long>> future2 = executorService.submit(() ->
                    dashboardSaveUseCase.saveDashboard(userId, groupId, locationId, requests2)
            );

            // then
            // 1. 요청 A는 락을 선점하여 성공적으로 저장 완료 후 위젯 ID 목록 반환
            List<Long> resultA = future1.get();
            assertThat(resultA).isNotEmpty();
            assertThat(resultA).contains(widgetIdA);

            // 2. 요청 B는 요청 A에 의해 락 대기 후 실행되므로, 상대방(A)이 Widget B를 삭제했음을 감지하고 AlreadyDashboardSaveException 발생
            assertThatThrownBy(() -> {
                try {
                    future2.get();
                } catch (ExecutionException e) {
                    throw e.getCause();
                }
            }).isInstanceOf(AlreadyDashboardSaveException.class);

            executorService.shutdown();
        }

        // 3. 비관적 락으로 직렬화되어 두 요청이 서로 위젯을 지워 0개가 되는 유실 버그 방지 검증 (Widget A 1개만 남음)
        List<Widget> remainingWidgets = widgetRepository.findAllByDashboardDashboardId(dashboardId);
        assertThat(remainingWidgets).hasSize(1);
        assertThat(remainingWidgets.getFirst().getWidgetId()).isEqualTo(widgetIdA);
    }
}
