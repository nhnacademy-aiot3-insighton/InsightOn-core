package com.insighton.core.usecase.actuator;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.actuators.control.ActuatorControlAdapterRegistry;
import com.insighton.core.domain.actuators.control.ControlProvider;
import com.insighton.core.domain.actuators.control.ProviderDevice;
import com.insighton.core.domain.actuators.dto.ActuatorResponse;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.location.dto.response.LocationListResponse;
import com.insighton.core.domain.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 액추에이터 등록 화면 / "공급자 장치" 현황 섹션에서 쓰는, 공급자 계정에 연결된 장치 목록.
// 각 장치가 이미 우리 액추에이터/장소에 매핑됐는지를 함께 채워준다. 매니저 이상만 조회 가능.
@UseCase
@RequiredArgsConstructor
public class ListProviderDevicesUseCase {

    private final GroupMemberService groupMemberService;
    private final ActuatorControlAdapterRegistry adapterRegistry;
    private final ActuatorService actuatorService;
    private final LocationRepository locationRepository;

    public List<ProviderDevice> execute(Long userId, Long groupsId, ControlProvider provider) {
        groupMemberService.validateGroupAdmin(groupsId, userId);

        List<ProviderDevice> devices = adapterRegistry.get(provider).listDevices();

        // 이 그룹의 액추에이터 중 같은 공급자로 이미 매핑된 것: externalDeviceId -> 액추에이터
        Map<String, ActuatorResponse> boundByDeviceId = actuatorService.getActuatorsByGroupId(groupsId).stream()
                .filter(a -> a.controlProvider() == provider && a.externalDeviceId() != null)
                .collect(Collectors.toMap(ActuatorResponse::externalDeviceId, Function.identity(), (a, b) -> a));

        // locationId -> 장소 이름 (매핑된 장치에 "어느 장소의" 액추에이터인지 보여주기 위함)
        Map<Long, String> locationNames = locationRepository.findAllByGroupGroupId(groupsId).stream()
                .collect(Collectors.toMap(LocationListResponse::locationId, LocationListResponse::locationName, (a, b) -> a));

        return devices.stream()
                .map(d -> {
                    ActuatorResponse bound = boundByDeviceId.get(d.externalDeviceId());
                    return bound == null
                            ? d
                            : d.withBinding(bound.actuatorId(), bound.sensorName(), locationNames.get(bound.locationId()));
                })
                .toList();
    }
}
