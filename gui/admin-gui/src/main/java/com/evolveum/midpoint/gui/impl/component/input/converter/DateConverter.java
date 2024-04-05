/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.input.converter;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;

import org.apache.wicket.Session;
import org.apache.wicket.core.request.ClientInfo;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.Strings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Converter for Date to String. Converter need pattern. We can define a boolean whether the time zone is used.
 */
public class DateConverter implements IConverter<Date> {

    private final boolean useTimeZone;
    private final String dateTimePattern;

    public DateConverter(String dateTimePattern, boolean useTimeZone) {
        this.useTimeZone = useTimeZone;
        this.dateTimePattern = dateTimePattern;
    }

    public DateConverter(String dateTimePattern) {
        this(dateTimePattern, false);
    }

    @Override
    public Date convertToObject(String value, Locale locale) throws ConversionException {
        if (Strings.isEmpty(value)) {
            return null;
        }

        SimpleDateFormat formatter = getFormatter();
        try {
            return formatter.parse(value);
        } catch (Exception e) {
            throw newConversionException(e);
        }
    }

    private ConversionException newConversionException(Exception cause) {
        return new ConversionException(cause)
                .setVariable("formatter", dateTimePattern);
    }

    private SimpleDateFormat getFormatter() {
        SimpleDateFormat formatter = new SimpleDateFormat(dateTimePattern, LocalizationUtil.findLocale());

        if (useTimeZone) {
            TimeZone timezone = getTimeZone();
            if (timezone != null) {
                formatter.setTimeZone(timezone);
            }
        }

        return formatter;
    }

    @Override
    public String convertToString(Date date, Locale locale) {
        return getFormatter().format(date);
    }

    private TimeZone getTimeZone() {
        TimeZone timezone = WebModelServiceUtils.getTimezone();
        if (timezone != null) {
            return timezone;
        }

        ClientInfo info = Session.get().getClientInfo();
        if (info instanceof WebClientInfo) {
            return ((WebClientInfo) info).getProperties().getTimeZone();
        }
        return null;
    }

}
