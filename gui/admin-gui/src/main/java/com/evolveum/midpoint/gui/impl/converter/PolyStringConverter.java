/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.converter;

import java.util.Locale;

import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import com.evolveum.midpoint.prism.polystring.PolyString;

public class PolyStringConverter implements IConverter<PolyString> {

    private static final long serialVersionUID = 1L;

    @Override
    public PolyString convertToObject(String value, Locale locale) throws ConversionException {
        if (value == null) {
            return null;
        }
        return new PolyString(value);
    }

    @Override
    public String convertToString(PolyString value, Locale locale) {
        if (value == null) {
            return null;
        }

        return value.getOrig();
    }



}
