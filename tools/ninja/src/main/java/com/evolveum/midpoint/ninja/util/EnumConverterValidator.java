package com.evolveum.midpoint.ninja.util;

import java.util.Arrays;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import org.jetbrains.annotations.NotNull;

public class EnumConverterValidator<T extends Enum> implements IStringConverter<T>, IParameterValidator {

    private Class<T> enumClass;

    public EnumConverterValidator(@NotNull Class<T> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public void validate(String name, String value) throws ParameterException {
        T mode = convert(value);
        if (mode == null) {
            throw new ParameterException("Unknown value '" + value + "', supported values: " +
                    Arrays.toString(
                            Arrays.stream(enumClass.getEnumConstants())
                                    .map(m -> m.name().toLowerCase())
                                    .toArray()));
        }
    }

    @Override
    public T convert(String value) {
        if (value == null) {
            return null;
        }

        for (T t : enumClass.getEnumConstants()) {
            if (t.name().equalsIgnoreCase(value)) {
                return t;
            }
        }

        return null;
    }
}
