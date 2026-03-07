package dev.haritonenko.error;

import dev.haritonenko.api.error.ErrorMessageResponse;
import dev.haritonenko.orders.domain.exception.AuthorizedMoneyAmountLessThanFinalException;
import dev.haritonenko.orders.domain.exception.CapturingFailedException;
import dev.haritonenko.orders.domain.exception.IllegalStateOfAuthorizedAmountException;
import dev.haritonenko.orders.domain.exception.IllegalStateOfFinalAmountException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthorizedMoneyAmountLessThanFinalException.class)
    public ResponseEntity<ErrorMessageResponse> handleAuthorizedMoneyAmountLessThanFinalException(
            AuthorizedMoneyAmountLessThanFinalException ex
    ) {
        log.warn("Got AuthorizedMoneyAmountLessThanFinalException", ex);
        var errorDto = getErrorMessageResponse(
                "Authorized amount is less than final amount",
                ex.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorDto);
    }

    @ExceptionHandler(CapturingFailedException.class)
    public ResponseEntity<ErrorMessageResponse> handleCapturingFailedException(
            CapturingFailedException ex
    ) {
        log.warn("Got CapturingFailedException", ex);
        var errorDto = getErrorMessageResponse(
                "Payment capture failed",
                ex.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorDto);
    }

    @ExceptionHandler(IllegalStateOfAuthorizedAmountException.class)
    public ResponseEntity<ErrorMessageResponse> handleIllegalStateOfAuthorizedAmountException(
            IllegalStateOfAuthorizedAmountException ex
    ) {
        log.warn("Got IllegalStateOfAuthorizedAmountException", ex);
        var errorDto = getErrorMessageResponse(
                "Authorized amount is in an invalid state",
                ex.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorDto);
    }

    @ExceptionHandler(IllegalStateOfFinalAmountException.class)
    public ResponseEntity<ErrorMessageResponse> handleIllegalStateOfFinalAmountException(
            IllegalStateOfFinalAmountException ex
    ) {
        log.warn("Got IllegalStateOfFinalAmountException", ex);
        var errorDto = getErrorMessageResponse(
                "Final amount is in an invalid state",
                ex.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorDto);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorMessageResponse> handleIllegalStateException(
            IllegalStateException ex
    ) {
        log.warn("Got IllegalStateException", ex);
        var errorDto = getErrorMessageResponse(
                "Invalid application state",
                ex.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorDto);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessageResponse> handleException(Exception ex) {
        log.error("Unexpected exception", ex);
        var errorDto = getErrorMessageResponse(
                "Unexpected internal error",
                ex.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorDto);
    }

    private ErrorMessageResponse getErrorMessageResponse(
            String message,
            String detailedMessage
    ) {
        return new ErrorMessageResponse(
                message,
                detailedMessage,
                LocalDateTime.now().toString()
        );
    }
}