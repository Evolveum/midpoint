/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
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
        DateTextField dateField = super.newDateTextField(id, dateFieldModel);
        dateField.add(new EmptyOnChangeAjaxFormUpdatingBehavior(){
            @Override
            protected void onUpdate(AjaxRequestTarget target){
                DateInput.this.setModelObject(computeDateTime());
            }
        });
        return dateField;

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
