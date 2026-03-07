package dev.haritonenko.orders.domain.exception;

public class IllegalStateOfFinalAmountException extends RuntimeException {
    public IllegalStateOfFinalAmountException(String message) {
        super(message);
    }
}
