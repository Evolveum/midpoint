/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.input;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.SessionStorage;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Session;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.*;

/**
 * Options that will be used for date time picker in js.
 * We can generate options as string for js script by {@link #toJsConfiguration()}.
 */
public class DateTimePickerOptions implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private final static List<String> LIST_OF_LOCALIZATION_KEYS;

    public enum Theme {LIGHT, DARK, AUTO}

    private Theme theme;

    // List of key that contains translation values. Key for localization is DateTimePickerOptions."key".
    static {
        LIST_OF_LOCALIZATION_KEYS = List.of(
                "today",
                "clear",
                "close",
                "selectMonth",
                "previousMonth",
                "nextMonth",
                "selectYear",
                "previousYear",
                "nextYear",
                "selectDecade",
                "previousDecade",
                "nextDecade",
                "previousCentury",
                "nextCentury",
                "pickHour",
                "incrementHour",
                "decrementHour",
                "pickMinute",
                "incrementMinute",
                "decrementMinute",
                "pickSecond",
                "incrementSecond",
                "decrementSecond",
                "toggleMeridiem",
                "selectTime",
                "selectDate"
        );
    }

    private DateTimePickerOptions() {
        SessionStorage.Mode mode = ((MidPointAuthWebSession) Session.get()).getSessionStorage().getMode();
        this.theme = Theme.LIGHT;
        if (SessionStorage.Mode.DARK.equals(mode)) {
            this.theme = Theme.DARK;
        }
    }

    public static DateTimePickerOptions of() {
        return new DateTimePickerOptions();
    }

    public DateTimePickerOptions theme(Theme theme) {
        this.theme = Objects.requireNonNullElse(theme, Theme.LIGHT);
        return this;
    }

    /**
     * Produce string that represent options for date time picker which can be used for js script.
     */
    public String toJsConfiguration() {
        StringBuilder sb = new StringBuilder("{");

        sb.append("display: {").append("theme: '").append(theme.name().toLowerCase()).append("'").append("}, ");

        sb.append("localization: {");
        LIST_OF_LOCALIZATION_KEYS.forEach(
                key -> sb.append(key)
                        .append(": '")
                        .append(LocalizationUtil.translate("DateTimePickerOptions." + key))
                        .append("', "));
        sb.append("dayViewHeaderFormat: { month: 'long', year: 'numeric'}, ");

        @NotNull Locale locale = LocalizationUtil.findLocale();
        sb.append("locale: '").append(locale.toLanguageTag()).append("', ");

        if (usesAmPm(locale)) {
            sb.append("hourCycle: 'h12',");
        } else {
            sb.append("hourCycle: 'h23',");
        }

        sb.append("format: '")
                .append(getDateTimeFormatForOption(locale))
                .append("' ");

        sb.append("},");

        sb.append("useCurrent: false,");

        sb.append("viewDate: MidPointTheme.createCurrentDateForDatePicker()");

        sb.append("}");
        return sb.toString();
    }

    /**
     * Produce string that represent options for date time picker displayed in a modal dialog which can be used for js script.
     */
    public String toJsConfiguration(String modalDialogId) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        sb.append("container: document.querySelector('#").append(modalDialogId).append(" .modal-content'),");

        String configuration = toJsConfiguration();
        configuration = configuration.substring(1, configuration.length() - 1);
        sb.append(configuration);

        sb.append("}");
        return sb.toString();
    }

    public List<String> getDateTimeFormat() {
        @NotNull Locale locale = LocalizationUtil.findLocale();
        String localizedDatePattern = getDateTimeFormat(locale);

        return List.of(
                getPatternForConverter(localizedDatePattern),
                getPatternForConverter(
                        replaceSpecificCharacters(((SimpleDateFormat) SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG, locale)).toPattern())),
                ((SimpleDateFormat) SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, locale)).toPattern()
        );
    }

    private String getPatternForConverter(String localizedDatePattern) {
        if (localizedDatePattern.contains("MMMM")) {
            localizedDatePattern = localizedDatePattern.replaceAll("MMMM", "LLLL");
        }
        return localizedDatePattern;
    }

    private String replaceSpecificCharacters(String localizedDatePattern) {
        if (!localizedDatePattern.contains("yyyy")) {
            localizedDatePattern = localizedDatePattern.replaceAll("yy", "yyyy");
        }

        //hack because of issue in js of date picker implementation
        if (localizedDatePattern.toLowerCase().contains("a")) {
            if (!localizedDatePattern.toLowerCase().contains("hh")
                    && localizedDatePattern.toLowerCase().contains("h")) {
                localizedDatePattern = localizedDatePattern.replaceAll("h", "hh");
                localizedDatePattern = localizedDatePattern.replaceAll("H", "HH");
            }
        } else {
            if (localizedDatePattern.toLowerCase().contains("hh")) {
                localizedDatePattern = localizedDatePattern.replaceAll("hh", "h");
                localizedDatePattern = localizedDatePattern.replaceAll("HH", "H");
            }
        }

        //hack because of issue in js of date picker implementation
        if (!localizedDatePattern.contains("MM")) {
            localizedDatePattern = localizedDatePattern.replaceAll("M", "MM");
            if (!localizedDatePattern.contains("dd")) {
                localizedDatePattern = localizedDatePattern.replaceAll("d", "dd");
            }
        }

        return localizedDatePattern;
    }

    private String getDateTimeFormat(@NotNull Locale locale) {
        return replaceSpecificCharacters(((SimpleDateFormat) SimpleDateFormat.getDateTimeInstance(
                SimpleDateFormat.LONG, SimpleDateFormat.SHORT, locale)).toPattern());
    }

    private String getDateTimeFormatForOption(@NotNull Locale locale) {
        return formatPatterForDateTimePicker(getDateTimeFormat(locale));
    }

    private String formatPatterForDateTimePicker(String dateTimeFormat) {
        if (StringUtils.isEmpty(dateTimeFormat)) {
            return "";
        }

        String replacingChar = "[";
        while (dateTimeFormat.contains("'")) {
            dateTimeFormat = dateTimeFormat.replaceFirst("'", replacingChar);
            if (replacingChar.equals("[")) {
                replacingChar = "]";
            } else {
                replacingChar = "[";
            }
        }

        dateTimeFormat = replacingCharForAmOrPm(dateTimeFormat);

        return dateTimeFormat;
    }

    private String replacingCharForAmOrPm(String dateTimeFormat) {
        if (!dateTimeFormat.toLowerCase().contains("a")) {
            return dateTimeFormat;
        }

        boolean isInBrackets = false;
        char[] chars = dateTimeFormat.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char currentChar = chars[i];

            if (!isInBrackets && currentChar == 'a') {
                dateTimeFormat = replaceChar(dateTimeFormat, i, 't');
                continue;
            }

            if (!isInBrackets && currentChar == 'A') {
                dateTimeFormat = replaceChar(dateTimeFormat, i, 'T');
                continue;
            }

            if (currentChar == '[') {
                isInBrackets = true;
                continue;
            }

            if (currentChar == ']') {
                isInBrackets = false;
            }

        }
        return dateTimeFormat;
    }

    private String replaceChar(String dateTimeFormat, int i, char replacingChar) {
        StringBuilder sb = new StringBuilder(dateTimeFormat);
        sb.setCharAt(i, replacingChar);
        return sb.toString();
    }

    private boolean usesAmPm(Locale locale) {
        String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                FormatStyle.SHORT, FormatStyle.SHORT, IsoChronology.INSTANCE, locale);
        return pattern.toLowerCase().contains("a");
    }

}
