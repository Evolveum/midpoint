/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.converter;

import java.util.Locale;

import javax.xml.datatype.Duration;

import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.MiscUtil;

public class DurationConverter implements IConverter<Duration>{

    private static final long serialVersionUID = 1L;

    @Override
    public Duration convertToObject(String value, Locale locale) throws ConversionException {
        return XmlTypeConverter.createDuration(MiscUtil.nullIfEmpty(value));
    }

    @Override
    public String convertToString(Duration value, Locale locale) {
        return value.toString();
    }

}
