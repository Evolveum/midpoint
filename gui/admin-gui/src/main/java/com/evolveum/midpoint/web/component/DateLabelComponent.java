/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.gui.impl.component.input.converter.DateConverter;

import com.evolveum.midpoint.web.component.util.SerializableBiFunction;

import org.apache.wicket.IGenericComponent;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

import java.util.Date;

/**
 * Created by honchar
 * Component for displaying date value as a label
 * By default (if no converter is set) the date is formatted
 * according to the client's locale, timezone (not implemented yet),
 * with applying long style for date and long style for time.
 */
public class DateLabelComponent extends Label implements IGenericComponent<Date, DateLabelComponent> {

    @SuppressWarnings("unused")
    public static final String SHORT_SHORT_STYLE = "SS";    //short style for date, short style for time
    @SuppressWarnings("unused")
    public static final String MEDIUM_MEDIUM_STYLE = "MM";    //medium style for date, medium style for time
    @SuppressWarnings("unused")
    public static final String LONG_LONG_STYLE = "LL";    //long style for date, long style for time
    @SuppressWarnings("unused")
    public static final String FULL_FULL_STYLE = "FF";    //full style for date, full style for time
    @SuppressWarnings("unused")
    public static final String SHORT_MEDIUM_STYLE = "SM";    //short style for date, medium style for time
    @SuppressWarnings("unused")
    public static final String SHORT_LONG_STYLE = "SL";    //short style for date, long style for time
    @SuppressWarnings("unused")
    public static final String SHORT_FULL_STYLE = "SF";    //short style for date, full style for time
    @SuppressWarnings("unused")
    public static final String SHORT_NOTIME_STYLE = "S-";    //short style for date, no time
    @SuppressWarnings("unused")
    public static final String MEDIUM_SHORT_STYLE = "MS";    //medium style for date, short style for time
    @SuppressWarnings("unused")
    public static final String MEDIUM_LONG_STYLE = "ML";    //medium style for date, long style for time
    @SuppressWarnings("unused")
    public static final String MEDIUM_FULL_STYLE = "MF";    //medium style for date, full style for time
    @SuppressWarnings("unused")
    public static final String MEDIUM_NOTIME_STYLE = "M-";    //medium style for date, no time
    @SuppressWarnings("unused")
    public static final String LONG_SHORT_STYLE = "LS";    //long style for date, short style for time
    @SuppressWarnings("unused")
    public static final String LONG_MEDIUM_STYLE = "LM";    //long style for date, medium style for time TODO ? let it be default style
    //if no other is specified
    @SuppressWarnings("unused")
    public static final String LONG_FULL_STYLE = "LF";    //long style for date, full style for time
    @SuppressWarnings("unused")
    public static final String LONG_NOTIME_STYLE = "L-";    //long style for date, no time
    @SuppressWarnings("unused")
    public static final String FULL_SHORT_STYLE = "FS";    //full style for date, short style for time
    @SuppressWarnings("unused")
    public static final String FULL_MEDIUM_STYLE = "FM";    //full style for date, medium style for time
    @SuppressWarnings("unused")
    public static final String FULL_LONG_STYLE = "FL";    //full style for date, long style for time
    @SuppressWarnings("unused")
    public static final String FULL_NOTIME_STYLE = "F-";    //full style for date, no time
    @SuppressWarnings("unused")
    public static final String NODATE_SHORT_STYLE = "-S";    //no date, short style for time
    @SuppressWarnings("unused")
    public static final String NODATE_MEDIUM_STYLE = "-M";    //no date, medium style for time
    @SuppressWarnings("unused")
    public static final String NODATE_LONG_STYLE = "-L";    //no date, long style for time
    @SuppressWarnings("unused")
    public static final String NODATE_FULL_STYLE = "-F";    //no date, full style for time

    private final String style;
    private String nullDateText = "";
    private SerializableBiFunction<String, Date, String> customizeNotNullDate;

    public DateLabelComponent(String id, IModel<Date> model, String style) {
        super(id, model);
        this.style = style;
    }

    /**
     * Set text that will be showed when value of date is null.
     */
    public void setTextOnDateNull(String nullDateText) {
        this.nullDateText = nullDateText;
    }

    @Override
    protected IConverter<?> createConverter(Class<?> type) {
        if (Date.class.isAssignableFrom(type)) {
            return new DateConverter(WebComponentUtil.getLocalizedDatePattern(style == null ? LONG_MEDIUM_STYLE : style), true);
        }
        return null;
    }

    @Override
    public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {

        Date date = getModelObject();
        if (date == null) {
            replaceComponentTagBody(markupStream, openTag, nullDateText);
            return;
        }

        String dateAsString = getDefaultModelObjectAsString();
        if (customizeNotNullDate != null) {
            dateAsString = customizeNotNullDate.apply(dateAsString, date);
        }
        replaceComponentTagBody(markupStream, openTag, dateAsString);

    }

    /**
     * Function for customize showed text for date. Input parameters are date as string and date, that can't be null.
     */
    public void customizeDateString(SerializableBiFunction<String, Date, String> customizeNotNullDate) {
        this.customizeNotNullDate = customizeNotNullDate;
    }
}

