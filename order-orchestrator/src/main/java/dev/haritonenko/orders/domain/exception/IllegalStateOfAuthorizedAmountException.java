package dev.haritonenko.orders.domain.exception;

public class IllegalStateOfAuthorizedAmountException extends RuntimeException {
    public IllegalStateOfAuthorizedAmountException(String message) {
        super(message);
    }
}
