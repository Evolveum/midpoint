package com.evolveum.midpoint.ninja.util;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import java.net.URI;

/**
 * Created by Viliam Repan (lazyman).
 */
public class URIConverter implements IStringConverter<URI>, IParameterValidator {

    private String optionName;

    public URIConverter() {
        this(null);
    }

    public URIConverter(String optionName) {
        this.optionName = optionName;
    }

    @Override
    public URI convert(String value) {
        if (value == null) {
            return null;
        }

        try {
            return URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Option " + optionName
                    + " doesn't contain valid URL ('" + value + "')", ex);
        }
    }

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (value == null) {
            return;
        }

        try {
            URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw new ParameterException("Option " + name
                    + " doesn't contain valid URL ('" + value + "'), reason: " + ex.getMessage());
        }
    }
}
