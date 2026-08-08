package com.insighton.core.domain.location.exception;

public class LocationAlreadyException extends RuntimeException {
    public LocationAlreadyException(String locationName) {
        super("Already exists location name. Location Name : " + locationName);
    }
}
