package com.evolveum.midpoint.ninja.util;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

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
