/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.util;

import java.io.File;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by Viliam Repan (lazyman).
 */
public class FileReferenceConverter implements IStringConverter<FileReference>, IParameterValidator {

    @Override
    public FileReference convert(String value) {
        if (value == null) {
            return null;
        }

        if (value.startsWith("@")) {
            String filePath = StringUtils.removeStart(value, "@");
            return new FileReference(new File(filePath));
        }

        return new FileReference(value);
    }

    @Override
    public void validate(String name, String value) throws ParameterException {
        convert(value);
    }
}
