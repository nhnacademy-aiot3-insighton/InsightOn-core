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
}
