package dev.haritonenko.tasks.domain.async.converter;

import dev.haritonenko.api.utils.EnumUtils;
import dev.haritonenko.tasks.domain.async.status.AsyncPaymentTaskStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter()
public class AsyncPaymentStatusConverter implements AttributeConverter<AsyncPaymentTaskStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(AsyncPaymentTaskStatus statusEnum) {
        return statusEnum == null
                ? null
                : statusEnum.getCode();
    }

    @Override
    public AsyncPaymentTaskStatus convertToEntityAttribute(Integer intCode) {
        return intCode == null
                ? null
                : EnumUtils.fromCode(AsyncPaymentTaskStatus.class, intCode);
    }
}
