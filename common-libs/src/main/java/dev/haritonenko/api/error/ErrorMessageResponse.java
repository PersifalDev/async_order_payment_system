package dev.haritonenko.api.error;

public record ErrorMessageResponse(
        String message,
        String detailedMessage,
        String dateTime
) {
}
