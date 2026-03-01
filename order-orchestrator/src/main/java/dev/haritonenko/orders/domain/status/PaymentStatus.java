package dev.haritonenko.orders.domain.status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import dev.haritonenko.api.utils.EnumUtils;

public enum PaymentStatus implements EnumUtils.IntEnum, EnumUtils.StringEnum {

    NEW(1, "NEW"),
    AUTHORIZED_SUCCESSFULLY(2, "AUTHORIZED_SUCCESSFULLY"),
    AUTHORIZATION_FAILED(3, "AUTHORIZATION_FAILED"),
    PRICE_CHANGED_FAILED(4, "PRICE_CHANGED_FAILED"),
    CAPTURE_FAILED(5, "CAPTURE_FAILED"),
    SUCCEED_PAID(6, "SUCCEED_PAID");

    private final int code;
    private final String value;

    PaymentStatus(int code, String value) {
        this.code = code;
        this.value = value;
    }

    @JsonCreator
    public static PaymentStatus fromValue(String value) {
        return EnumUtils.fromValue(PaymentStatus.class, value);
    }

    @Override
    public int getCode() {
        return code;
    }

    @JsonValue
    @Override
    public String getValue() {
        return value;
    }

}
