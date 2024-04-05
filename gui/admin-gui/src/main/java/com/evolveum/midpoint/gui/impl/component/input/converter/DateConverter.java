/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.input.converter;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.input.DateTimePickerOptions;

import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.Strings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateConverter implements IConverter<Date> {

    @Override
    public Date convertToObject(String value, Locale locale) throws ConversionException {
        if (Strings.isEmpty(value))
        {
            return null;
        }

        SimpleDateFormat formatter = getFormatter();
            try
            {
                return formatter.parse(value);
            }
            catch (Exception e)
            {
                throw newConversionException(e);
            }
    }

    private ConversionException newConversionException(Exception cause)
    {
        return new ConversionException(cause)
                .setVariable("formatter", getDatePattern());
    }

    private String getDatePattern() {
        return DateTimePickerOptions.of().getDateTimeFormat();
    }

    private SimpleDateFormat getFormatter() {
        return new SimpleDateFormat(getDatePattern(), LocalizationUtil.findLocale());
    }

    @Override
    public String convertToString(Date date, Locale locale) {
        SimpleDateFormat formatter = getFormatter();
        TimeZone timezone = WebModelServiceUtils.getTimezone();
        if (timezone != null) {
            formatter.setTimeZone(timezone);
        }
        return formatter.format(date);
    }
}
