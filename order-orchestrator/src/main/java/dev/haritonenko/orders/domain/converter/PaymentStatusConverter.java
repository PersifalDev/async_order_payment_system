package dev.haritonenko.orders.domain.converter;

import dev.haritonenko.api.utils.EnumUtils;
import dev.haritonenko.orders.domain.status.PaymentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter()
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(PaymentStatus statusEnum) {
        return statusEnum == null
                ? null
                : statusEnum.getCode();
    }

    @Override
    public PaymentStatus convertToEntityAttribute(Integer intCode) {
        return intCode == null
                ? null
                : EnumUtils.fromCode(PaymentStatus.class, intCode);
    }

}
