package com.insighton.core.domain.location.exception;

public class LocationNotFoundException extends RuntimeException {
    public LocationNotFoundException(String message) {
        super(message);
    }

    public static LocationNotFoundException notFoundLocationByLocationId(Long locationId) {
        return new LocationNotFoundException(
                String.format("location not found. Location ID : " + locationId)
        );
    }

    public static LocationNotFoundException notFoundLocationByGroupId(Long groupId) {
        return new LocationNotFoundException(
                String.format("location not found. Group ID : " + groupId)
        );
    }

    // 사용자가 locationId를 모르고 이름으로 요청할 때(ActuatorRequest, SensorUpdateRequest 등) 못 찾으면 사용
    public static LocationNotFoundException notFoundLocationByName(String locationName) {
        return new LocationNotFoundException(
                String.format("location not found. Location Name : " + locationName)
        );
    }
}
