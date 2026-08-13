package com.insighton.core.usecase;

import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.repository.DashboardRepository;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.repository.GroupMemberRepository;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.repository.GroupRepository;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.repository.LocationRepository;
import com.insighton.core.domain.region.loader.RegionCsvLoader;
import com.insighton.core.domain.widgets.dto.request.WidgetSaveRequest;
import com.insighton.core.domain.widgets.entity.Widget;
import com.insighton.core.domain.widgets.exception.AlreadyDashboardSaveException;
import com.insighton.core.domain.widgets.repository.InfluxDbRepository;
import com.insighton.core.domain.widgets.repository.WidgetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @MockitoBean
    private InfluxDbRepository influxDbRepository;
    @MockitoBean
    private RegionCsvLoader regionCsvLoader;
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

        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        // when
        // 요청 A 제출
        Future<List<Long>> future1 = executorService.submit(() -> {
            startLatch.await();
            return dashboardSaveUseCase.saveDashboard(userId, groupId, locationId, requests1);
        });

        // 요청 B 제출 (요청 A가 먼저 대시보드 비관적 락을 점유하도록 50ms 미세 지연)
        Future<List<Long>> future2 = executorService.submit(() -> {
            startLatch.await();
            Thread.sleep(50);
            return dashboardSaveUseCase.saveDashboard(userId, groupId, locationId, requests2);
        });

        // 두 스레드가 동시에 실행 시작
        startLatch.countDown();

        // then
        // 1. 요청 A (User A)는 성공하여 저장된 위젯 ID 목록 반환
        List<Long> resultA = future1.get();
        assertThat(resultA).isNotEmpty();
        assertThat(resultA).contains(widgetIdA);

        // 2. 요청 B (User B)는 요청 A에 의해 widgetIdB가 이미 삭제되었으므로 AlreadyDashboardSaveException 예외 발생 명시적 검증
        assertThatThrownBy(() -> {
            try {
                future2.get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        }).isInstanceOf(AlreadyDashboardSaveException.class);

        executorService.shutdown();

        // 3. 두 요청이 동시에 상대방 위젯을 삭제하여 위젯이 전부(0개) 삭제되는 데이터 유실 버그 방지 검증 (Widget A 1개만 유지됨)
        List<Widget> remainingWidgets = widgetRepository.findAllByDashboardDashboardId(dashboardId);
        assertThat(remainingWidgets).hasSize(1);
        assertThat(remainingWidgets.getFirst().getWidgetId()).isEqualTo(widgetIdA);
    }
}
