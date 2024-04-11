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
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Converter for Date to String. Converter need pattern. We can define a boolean whether the time zone is used.
 */
public class DateConverter implements IConverter<Date> {

    private final boolean useTimeZone;
    private final List<String> dateTimePattern;

    public DateConverter(String dateTimePattern, boolean useTimeZone) {
        this(List.of(dateTimePattern), useTimeZone);
    }

    public DateConverter(List<String> dateTimePattern, boolean useTimeZone) {
        this.useTimeZone = useTimeZone;
        this.dateTimePattern = dateTimePattern;
    }

    public DateConverter(List<String> dateTimePattern) {
        this(dateTimePattern, false);
    }

    @Override
    public Date convertToObject(String value, Locale locale) throws ConversionException {
        if (Strings.isEmpty(value)) {
            return null;
        }

        Exception ex = null;
        for (String pattern : dateTimePattern) {
            DateTimeFormatter formatter = getFormatter(pattern);
            try {
                return Date.from(
                        LocalDateTime.parse(value, formatter)
                                .atZone(ZoneId.systemDefault())
                                .toInstant());
            } catch (Exception e) {
                if (ex == null) {
                    ex = e;
                }
            }
        }
        throw newConversionException(ex);
    }

    private ConversionException newConversionException(Exception cause) {
        return new ConversionException(cause)
                .setVariable("formatter", dateTimePattern);
    }

    private DateTimeFormatter getFormatter(String dateTimePattern) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                .appendPattern(dateTimePattern);
        @NotNull Locale locale = LocalizationUtil.findLocale();

        if (!dateTimePattern.contains("h") && !dateTimePattern.contains("H")) {
            if (usesAmPm(locale)) {
                builder.parseDefaulting(ChronoField.CLOCK_HOUR_OF_AMPM, 12);
                builder.parseDefaulting(ChronoField.AMPM_OF_DAY, 0);
            } else {
                builder.parseDefaulting(ChronoField.HOUR_OF_DAY, 0);
            }
        }

        if (!dateTimePattern.contains("m")) {
            builder.parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0);
        }

        DateTimeFormatter formatter = builder.toFormatter(locale);

        if (useTimeZone) {
            TimeZone timezone = getTimeZone();
            if (timezone != null) {
                formatter.withZone(timezone.toZoneId());
            }
        }

        return formatter;
    }

    @Override
    public String convertToString(Date date, Locale locale) {
        Instant instant = date.toInstant();

        LocalDateTime ldt = instant.atZone(getZoneId()).toLocalDateTime();
        return ldt.format(getFormatter(this.dateTimePattern.get(0)));
    }

    private ZoneId getZoneId() {
        if (useTimeZone) {
            TimeZone timezone = getTimeZone();
            if (timezone != null) {
                return timezone.toZoneId();
            }
        }

        return ZoneId.systemDefault();
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

    private boolean usesAmPm(Locale locale) {
        String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                FormatStyle.SHORT, FormatStyle.SHORT, IsoChronology.INSTANCE, locale);
        return pattern.toLowerCase().contains("a");
    }

}
