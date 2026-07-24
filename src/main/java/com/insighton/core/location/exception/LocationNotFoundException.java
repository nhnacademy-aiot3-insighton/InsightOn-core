package com.insighton.core.location.exception;

public class LocationNotFoundException extends RuntimeException {
    public LocationNotFoundException(Long locationId) {
        super("location not found. Location ID : " + locationId);
    }
}
