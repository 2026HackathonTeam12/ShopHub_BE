package org.hackathon12.shophub.infrastructure.mockmap;

public class MockMapApiException extends RuntimeException {

    public MockMapApiException(String message) {
        super(message);
    }

    public MockMapApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
