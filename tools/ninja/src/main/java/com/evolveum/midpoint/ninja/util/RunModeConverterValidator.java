package com.evolveum.midpoint.ninja.util;

import com.evolveum.midpoint.ninja.action.RunSqlOptions;

public class RunModeConverterValidator extends EnumConverterValidator<RunSqlOptions.Mode> {

    public RunModeConverterValidator() {
        super(RunSqlOptions.Mode.class);
    }
}
