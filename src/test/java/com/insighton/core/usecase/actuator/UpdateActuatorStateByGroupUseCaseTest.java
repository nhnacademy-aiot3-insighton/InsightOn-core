package com.insighton.core.usecase.actuator;

import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuators.dto.ActuatorCommandRequest;
import com.insighton.core.domain.actuators.entity.Actuator;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.exception.ActuatorLocationsActuatorTypeNotFound;
import com.insighton.core.domain.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.domain.actuators.exception.InvalidServiceCredentialException;
import com.insighton.core.domain.actuators.repository.ActuatorRepository;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.location.exception.LocationNotFoundException;
import com.insighton.core.domain.location.service.LocationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UpdateActuatorStateByGroupUseCaseTest {

    @Mock
    private LocationService locationService;

    @Mock
    private ActuatorRepository actuatorRepository;

    @Mock
    private ActuatorService actuatorService;

    @InjectMocks
    private UpdateActuatorStateByGroupUseCase updateActuatorStateByGroupUseCase;

    @Test
    @DisplayName("성공 - 소유권 검증 통과 후 같은 위치+타입 액추에이터 전부에 상태 반영")
    void 실행_성공_전체반영() {
        Actuator a1 = Actuator.builder().actuatorId(1L).actuatorType(ActuatorType.AIRCON).build();
        Actuator a2 = Actuator.builder().actuatorId(2L).actuatorType(ActuatorType.AIRCON).build();
        given(actuatorRepository.findByLocationLocationIdAndActuatorType(20L, ActuatorType.AIRCON))
                .willReturn(List.of(a1, a2));

        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "ON", ExecutedByType.RULE_ENGINE);

        updateActuatorStateByGroupUseCase.execute(5L, 20L, request);

        verify(locationService).getLocationByGroupId(20L, 5L);
        verify(actuatorService).updateActuatorState(5L, 1L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null);
        verify(actuatorService).updateActuatorState(5L, 2L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null);
    }

    @Test
    @DisplayName("USER가 호출하면 차단 - 소유권 검증조차 안 함")
    void 실행_USER호출_차단() {
        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "ON", ExecutedByType.USER);

        assertThatThrownBy(() -> updateActuatorStateByGroupUseCase.execute(5L, 20L, request))
                .isInstanceOf(InvalidServiceCredentialException.class);

        verifyNoInteractions(locationService);
        verifyNoInteractions(actuatorRepository);
        verifyNoInteractions(actuatorService);
    }

    @Test
    @DisplayName("groupId+locationId 소유권 불일치 - 조회/변경 전혀 안 함")
    void 실행_그룹불일치() {
        willThrow(LocationNotFoundException.notFoundLocationByLocationId(20L))
                .given(locationService).getLocationByGroupId(20L, 5L);

        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "ON", ExecutedByType.RULE_ENGINE);

        assertThatThrownBy(() -> updateActuatorStateByGroupUseCase.execute(5L, 20L, request))
                .isInstanceOf(LocationNotFoundException.class);

        verifyNoInteractions(actuatorRepository);
        verifyNoInteractions(actuatorService);
    }

    @Test
    @DisplayName("알 수 없는 actuatorType이면 예외, 조회 전혀 안 함")
    void 실행_알수없는타입() {
        ActuatorCommandRequest request = new ActuatorCommandRequest("UNKNOWN_TYPE", "power", "ON", ExecutedByType.RULE_ENGINE);

        assertThatThrownBy(() -> updateActuatorStateByGroupUseCase.execute(5L, 20L, request))
                .isInstanceOf(InvalidActuatorValueException.class);

        verify(locationService).getLocationByGroupId(20L, 5L);
        verifyNoInteractions(actuatorRepository);
        verifyNoInteractions(actuatorService);
    }

    @Test
    @DisplayName("소유권은 확인됐지만 해당 위치+타입 액추에이터가 없으면 예외")
    void 실행_대상없음() {
        given(actuatorRepository.findByLocationLocationIdAndActuatorType(20L, ActuatorType.AIRCON))
                .willReturn(List.of());

        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "ON", ExecutedByType.RULE_ENGINE);

        assertThatThrownBy(() -> updateActuatorStateByGroupUseCase.execute(5L, 20L, request))
                .isInstanceOf(ActuatorLocationsActuatorTypeNotFound.class);

        verifyNoInteractions(actuatorService);
    }

    @Test
    @DisplayName("허용 안 된 명령 값이면 예외, 상태변경 호출 안 함")
    void 실행_허용안된값() {
        Actuator actuator = Actuator.builder().actuatorId(1L).actuatorType(ActuatorType.AIRCON).build();
        given(actuatorRepository.findByLocationLocationIdAndActuatorType(20L, ActuatorType.AIRCON))
                .willReturn(List.of(actuator));

        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "EXPLODE", ExecutedByType.RULE_ENGINE);

        assertThatThrownBy(() -> updateActuatorStateByGroupUseCase.execute(5L, 20L, request))
                .isInstanceOf(InvalidActuatorValueException.class);

        verify(actuatorService, never()).updateActuatorState(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("중간 실패 - 앞 액추에이터까지만 처리된 상태로 예외가 그대로 전파됨 (실제 롤백 여부는 @Transactional에 의존, 통합 테스트 별도 필요)")
    void 실행_중간실패_예외전파() {
        Actuator a1 = Actuator.builder().actuatorId(1L).actuatorType(ActuatorType.AIRCON).build();
        Actuator a2 = Actuator.builder().actuatorId(2L).actuatorType(ActuatorType.AIRCON).build();
        given(actuatorRepository.findByLocationLocationIdAndActuatorType(20L, ActuatorType.AIRCON))
                .willReturn(List.of(a1, a2));

        // 1번째 액추에이터는 정상 처리되도록 명시적으로 스텁 (안 그러면 strict stubbing이 "같은 메서드의 다른 스텁과 인자 불일치"로 오판해서 실패함)
        willDoNothing().given(actuatorService).updateActuatorState(5L, 1L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null);
        willThrow(new RuntimeException("두번째 액추에이터 처리 중 장애"))
                .given(actuatorService).updateActuatorState(5L, 2L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null);

        ActuatorCommandRequest request = new ActuatorCommandRequest("AIRCON", "power", "ON", ExecutedByType.RULE_ENGINE);

        assertThatThrownBy(() -> updateActuatorStateByGroupUseCase.execute(5L, 20L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("두번째 액추에이터 처리 중 장애");

        verify(actuatorService).updateActuatorState(5L, 1L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null);
        verify(actuatorService).updateActuatorState(5L, 2L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null);
    }
}
