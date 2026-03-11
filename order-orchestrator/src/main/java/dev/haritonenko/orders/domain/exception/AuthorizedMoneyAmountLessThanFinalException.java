package dev.haritonenko.orders.domain.exception;

public class AuthorizedMoneyAmountLessThanFinalException extends RuntimeException {
    public AuthorizedMoneyAmountLessThanFinalException(String message) {
        super(message);
    }
}
