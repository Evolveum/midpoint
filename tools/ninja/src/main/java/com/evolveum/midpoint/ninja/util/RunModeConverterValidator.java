package com.evolveum.midpoint.ninja.util;

import java.util.Arrays;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import com.evolveum.midpoint.ninja.action.RunSqlOptions;

public class RunModeConverterValidator implements IStringConverter<RunSqlOptions.Mode>, IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
        RunSqlOptions.Mode mode = convert(value);
        if (mode == null) {
            throw new ParameterException("Unknown mode '" + value + "', supported values: " +
                    Arrays.toString(
                            Arrays.stream(RunSqlOptions.Mode.values())
                                    .map(m -> m.name().toLowerCase())
                                    .toArray()));
        }
    }

    @Override
    public RunSqlOptions.Mode convert(String value) {
        if (value == null) {
            return null;
        }

        for (RunSqlOptions.Mode mode : RunSqlOptions.Mode.values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }

        return null;
    }
}
