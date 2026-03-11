package dev.haritonenko.orders.domain.exception;

public class CapturingFailedException extends RuntimeException {
    public CapturingFailedException(String message) {
        super(message);
    }
}
