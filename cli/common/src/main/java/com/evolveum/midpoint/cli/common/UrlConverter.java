/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.cli.common;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author lazyman
 */
public class UrlConverter implements IStringConverter<URL>, IParameterValidator {

    private String optionName;

    public UrlConverter() {
        this(null);
    }

    public UrlConverter(String optionName) {
        this.optionName = optionName;
    }

    @Override
    public URL convert(String value) {
        if (value == null) {
            return null;
        }

        try {
            return new URL(value);
        } catch (MalformedURLException ex) {
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
            new URL(value);
        } catch (MalformedURLException ex) {
            throw new ParameterException("Option " + name
                    + " doesn't contain valid URL ('" + value + "'), reason: " + ex.getMessage());
        }
    }
}
