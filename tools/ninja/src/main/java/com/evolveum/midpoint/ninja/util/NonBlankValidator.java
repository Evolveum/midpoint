/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja.util;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import org.apache.commons.lang3.StringUtils;

public class NonBlankValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (StringUtils.isBlank(value)) {
            throw new ParameterException("Parameter " + name + " should not be empty");
        }
    }
}
