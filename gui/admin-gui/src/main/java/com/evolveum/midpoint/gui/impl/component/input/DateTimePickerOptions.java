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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class DateTimePickerOptions {

    private final static List<String> LIST_OF_LOCALIZATION_KEYS;

    public enum Theme {LIGHT, DARK, AUTO}
    private Theme theme;

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

    public static DateTimePickerOptions of(){
        return new DateTimePickerOptions();
    }

    public DateTimePickerOptions theme(Theme theme) {
        this.theme = Objects.requireNonNullElse(theme, Theme.LIGHT);
        return this;
    }

    public String toJsConfiguration() {
        StringBuilder sb = new StringBuilder("{");

        sb.append("display: {").append("theme: '").append(theme.name().toLowerCase()).append("'").append("}, ");

        sb.append("localization: {");
        LIST_OF_LOCALIZATION_KEYS.forEach(
                key -> sb.append(key)
                        .append(": '")
                        .append(LocalizationUtil.translate("DateTimePickerPanel." + key))
                        .append("', "));
        sb.append("dayViewHeaderFormat: { month: 'long', year: 'numeric' }, ");

        @NotNull Locale locale = LocalizationUtil.findLocale();
        sb.append("locale: '").append(locale.toLanguageTag()).append("', ");

        sb.append("format: '")
                .append(getDateTimeFormatForLocale(locale))
                .append("' ");

        sb.append("}");

        sb.append("}");
        return sb.toString();
    }

    public String getDateTimeFormat() {
        @NotNull Locale locale = LocalizationUtil.findLocale();
        String localizedDatePattern = getDateTimeFormat(locale);

        if (localizedDatePattern.contains("MMMM")){
            localizedDatePattern = localizedDatePattern.replaceAll("MMMM", "LLLL");
        }

        if (localizedDatePattern != null && !localizedDatePattern.contains("yyyy")){
            localizedDatePattern = localizedDatePattern.replaceAll("yy", "yyyy");
        }

        return localizedDatePattern;
    }

    private String getDateTimeFormat(@NotNull Locale locale) {
        String dateFormat = ((SimpleDateFormat) SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG, locale)).toPattern();
        String timeFormat = ((SimpleDateFormat) SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, locale)).toPattern();
        return dateFormat + " " + timeFormat;
    }

    private String getDateTimeFormatForLocale(@NotNull Locale locale) {
        return replacingSingleQuotationMark(getDateTimeFormat(locale));
    }

    private String replacingSingleQuotationMark(String dateTimeFormat) {
        if (StringUtils.isEmpty(dateTimeFormat)) {
            return "";
        }

        String replacingChar = "[";
        while (dateTimeFormat.indexOf("'") != -1) {
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
        for (int i = 0; i < chars.length; i++){
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

}
