package com.insighton.core.domain.actuators.control;

// 공급자 계정에 연결된 장치 한 건 - 액추에이터 등록 시 매핑 대상 선택용.
// actuatorType은 공급자가 알려준 문자열 그대로 (CORE ActuatorType과 매칭 여부는 호출부가 판단).
// bound* 필드: 이 장치가 이미 매핑된 우리 액추에이터/장소 (전부 null이면 아직 미연결).
//   -> 어댑터는 공급자 응답만 알아서 3-인자 생성자로 만들고, UseCase가 매핑 현황을 채운다.
public record ProviderDevice(
        String externalDeviceId,
        String name,
        String actuatorType,
        Long boundActuatorId,
        String boundActuatorName,
        String boundLocationName
) {

    public ProviderDevice(String externalDeviceId, String name, String actuatorType) {
        this(externalDeviceId, name, actuatorType, null, null, null);
    }

    public ProviderDevice withBinding(Long actuatorId, String actuatorName, String locationName) {
        return new ProviderDevice(externalDeviceId, name, actuatorType, actuatorId, actuatorName, locationName);
    }
}
