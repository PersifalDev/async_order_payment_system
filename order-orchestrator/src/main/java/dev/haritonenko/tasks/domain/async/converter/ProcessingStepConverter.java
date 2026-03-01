package dev.haritonenko.tasks.domain.async.converter;

import dev.haritonenko.api.utils.EnumUtils;
import dev.haritonenko.tasks.domain.async.status.ProcessingStep;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter()
public class ProcessingStepConverter implements AttributeConverter<ProcessingStep, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ProcessingStep statusEnum) {
        return statusEnum == null
                ? null
                : statusEnum.getCode();
    }

    @Override
    public ProcessingStep convertToEntityAttribute(Integer intCode) {
        return intCode == null
                ? null
                : EnumUtils.fromCode(ProcessingStep.class, intCode);
    }
}
