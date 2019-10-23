/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author lazyman
 */
public class DateInput extends DateTimeField {

    public DateInput(String id, IModel<Date> model) {
        super(id, model);
        ((DropDownChoice)get("amOrPmChoice")).add(new EmptyOnChangeAjaxFormUpdatingBehavior(){
            @Override
            protected void onUpdate(AjaxRequestTarget target){
                DateInput.this.setModelObject(computeDateTime());
            }
        });
    }

    @Override
    protected DateTextField newDateTextField(String id, PropertyModel dateFieldModel) {
        String localizedDatePattern = WebComponentUtil.getLocalizedDatePattern(DateLabelComponent.SHORT_NOTIME_STYLE);
        if (localizedDatePattern != null && !localizedDatePattern.contains("yyyy")){
            localizedDatePattern = localizedDatePattern.replaceAll("yy", "yyyy");
        }
        DateTextField dateField = DateTextField.forDatePattern(id, dateFieldModel, localizedDatePattern);
        dateField.add(new EmptyOnChangeAjaxFormUpdatingBehavior(){
            @Override
            protected void onUpdate(AjaxRequestTarget target){
                DateInput.this.setModelObject(computeDateTime());
            }
        });
        return dateField;

    }

    @Override
    public void convertInput() {
        super.convertInput();
        Date convertedDate = getConvertedInput();
        Date modelDate = getModelObject();
        if (convertedDate == null || modelDate == null){
            return;
        }

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(modelDate.getTime());

        //set seconds and milliseconds only in case when the date input value wasn't changed
        if (gregorianCalendar.get(13) > 0 || gregorianCalendar.get(14) > 0){
            GregorianCalendar convertedCalendar = new GregorianCalendar();
            convertedCalendar.setTimeInMillis(convertedDate.getTime());

            convertedCalendar.set(13, gregorianCalendar.get(13));
            convertedCalendar.set(14, gregorianCalendar.get(14));
            setConvertedInput(convertedCalendar.getTime());
        }
    }

    @Override
    protected TextField<Integer> newMinutesTextField(String id, IModel<Integer> model, Class<Integer> type) {
        TextField<Integer> textField = super.newMinutesTextField(id, model, type);
        textField.add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target){
                DateInput.this.setModelObject(computeDateTime());
            }
        });

        return textField;
    }

    @Override
    protected TextField<Integer> newHoursTextField(final String id, IModel<Integer> model, Class<Integer> type) {
        TextField<Integer> textField = super.newHoursTextField(id, model, type);
        textField.add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target){
                DateInput.this.setModelObject(computeDateTime());
            }
        });
        return textField;
    }

    public Date computeDateTime() {
        Date dateFieldInput = getDate();
        if (dateFieldInput == null) {
            return null;
        }

        Integer hoursInput = getHours();
        Integer minutesInput = getMinutes();
        AM_PM amOrPmInput = getAmOrPm();

        // Get year, month and day ignoring any timezone of the Date object
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateFieldInput);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hours = (hoursInput == null ? 0 : hoursInput % 24);
        int minutes = (minutesInput == null ? 0 : minutesInput);

        // Use the input to create a date object with proper timezone
        MutableDateTime date = new MutableDateTime(year, month, day, hours, minutes, 0, 0,
                DateTimeZone.forTimeZone(getClientTimeZone()));

        // Adjust for halfday if needed
        if (use12HourFormat()) {
            int halfday = (amOrPmInput == AM_PM.PM ? 1 : 0);
            date.set(DateTimeFieldType.halfdayOfDay(), halfday);
            date.set(DateTimeFieldType.hourOfHalfday(), hours % 12);
        }

        // The date will be in the server's timezone
        return newDateInstance(date.getMillis());
    }
}
