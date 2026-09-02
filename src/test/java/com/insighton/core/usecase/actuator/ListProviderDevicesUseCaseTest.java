package com.insighton.core.usecase.actuator;

import com.insighton.core.domain.actuators.control.ActuatorControlAdapter;
import com.insighton.core.domain.actuators.control.ActuatorControlAdapterRegistry;
import com.insighton.core.domain.actuators.control.ControlProvider;
import com.insighton.core.domain.actuators.control.ProviderDevice;
import com.insighton.core.domain.actuators.exception.UnsupportedControlProviderException;
import com.insighton.core.domain.actuators.dto.ActuatorResponse;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.location.dto.response.LocationListResponse;
import com.insighton.core.domain.location.repository.LocationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ListProviderDevicesUseCaseTest {

    @Mock private GroupMemberService groupMemberService;
    @Mock private ActuatorControlAdapterRegistry adapterRegistry;
    @Mock private ActuatorControlAdapter adapter;
    @Mock private ActuatorService actuatorService;
    @Mock private LocationRepository locationRepository;

    @InjectMocks
    private ListProviderDevicesUseCase useCase;

    private static ActuatorResponse actuator(Long id, String name, ControlProvider provider, String externalDeviceId) {
        return new ActuatorResponse(id, 7L, name, ActuatorType.AIRCON, null, null, null, provider, externalDeviceId);
    }

    @Test
    @DisplayName("매니저 이상 권한 검증 후 해당 provider 어댑터의 장치 목록 반환")
    void execute_성공() {
        given(adapterRegistry.get(ControlProvider.SMART_THINGS)).willReturn(adapter);
        given(adapter.listDevices()).willReturn(List.of(new ProviderDevice("st-aircon-001", "회의실 에어컨", "AIRCON")));
        given(actuatorService.getActuatorsByGroupId(5L)).willReturn(List.of());
        given(locationRepository.findAllByGroupGroupId(5L)).willReturn(List.of());

        List<ProviderDevice> result = useCase.execute(1L, 5L, ControlProvider.SMART_THINGS);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).boundActuatorId()).isNull();
        verify(groupMemberService).validateGroupAdmin(5L, 1L);
    }

    @Test
    @DisplayName("이미 매핑된 장치는 boundActuatorId/Name이 채워지고, 안 된 장치는 null")
    void execute_매핑현황_채움() {
        given(adapterRegistry.get(ControlProvider.SMART_THINGS)).willReturn(adapter);
        given(adapter.listDevices()).willReturn(List.of(
                new ProviderDevice("st-aircon-001", "회의실 에어컨", "AIRCON"),
                new ProviderDevice("st-purifier-001", "로비 공기청정기", "AIR_PURIFIER")
        ));
        given(actuatorService.getActuatorsByGroupId(5L)).willReturn(List.of(
                actuator(42L, "3층 에어컨", ControlProvider.SMART_THINGS, "st-aircon-001"),
                actuator(43L, "LG 에어컨", ControlProvider.LG_THINQ, "lg-aircon-001") // 다른 공급자 - 무시돼야 함
        ));
        given(locationRepository.findAllByGroupGroupId(5L)).willReturn(List.of(
                new LocationListResponse(7L, "3층 회의실", null)
        ));

        List<ProviderDevice> result = useCase.execute(1L, 5L, ControlProvider.SMART_THINGS);

        assertThat(result).extracting(ProviderDevice::externalDeviceId, ProviderDevice::boundActuatorId,
                        ProviderDevice::boundActuatorName, ProviderDevice::boundLocationName)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("st-aircon-001", 42L, "3층 에어컨", "3층 회의실"),
                        org.assertj.core.groups.Tuple.tuple("st-purifier-001", null, null, null)
                );
    }

    @Test
    @DisplayName("MEMBER 권한이면 NoPermissionException, 어댑터 조회 안 함")
    void execute_권한없음() {
        given(groupMemberService.validateGroupAdmin(5L, 1L)).willThrow(NoPermissionException.forAdmin(1L));

        assertThatThrownBy(() -> useCase.execute(1L, 5L, ControlProvider.SMART_THINGS))
                .isInstanceOf(NoPermissionException.class);
    }

    @Test
    @DisplayName("구현체가 없는 provider면 UnsupportedControlProviderException")
    void execute_미지원provider() {
        given(adapterRegistry.get(ControlProvider.LG_THINQ)).willThrow(new UnsupportedControlProviderException(ControlProvider.LG_THINQ));

        assertThatThrownBy(() -> useCase.execute(1L, 5L, ControlProvider.LG_THINQ))
                .isInstanceOf(UnsupportedControlProviderException.class);
    }
}
