package dev.haritonenko.tasks.domain.async.status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import dev.haritonenko.api.utils.EnumUtils;

public enum ProcessingStep implements EnumUtils.IntEnum, EnumUtils.StringEnum {

    VALIDATE(1, "VALIDATE"),
    REPRICE(2, "REPRICE"),
    AUTH(3, "AUTH"),
    CAPTURE(4, "CAPTURE");

    private final int code;
    private final String value;

    ProcessingStep(int code, String value) {
        this.code = code;
        this.value = value;
    }

    @JsonCreator
    public static ProcessingStep fromStringValue(String value) {
        return EnumUtils.fromValue(ProcessingStep.class, value);
    }

    public static ProcessingStep fromCode(int code) {
        return EnumUtils.fromCode(ProcessingStep.class, code);
    }

    @JsonValue
    @Override
    public String getValue() {
        return value;
    }

    @Override
    public int getCode() {
        return code;
    }
}
