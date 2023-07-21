/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
