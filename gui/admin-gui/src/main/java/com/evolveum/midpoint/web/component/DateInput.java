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

import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.model.IModel;

import java.util.Date;

/**
 * @author lazyman
 */
public class DateInput extends DateTimeField {

    public DateInput(String id, IModel<Date> model) {
        super(id, model);
    }

//    public void updateDateTimeModel() {
//        setModelObject(computeDateTime());
//    }
//
//    public Date computeDateTime() {
//        Date dateFieldInput = getDate();
//        if (dateFieldInput == null) {
//            return null;
//        }
//
//        Integer hoursInput = getHours();
//        Integer minutesInput = getMinutes();
//        AM_PM amOrPmInput = getAmOrPm();
//
//        // Get year, month and day ignoring any timezone of the Date object
//        Calendar cal = Calendar.getInstance();
//        cal.setTime(dateFieldInput);
//        int year = cal.get(Calendar.YEAR);
//        int month = cal.get(Calendar.MONTH) + 1;
//        int day = cal.get(Calendar.DAY_OF_MONTH);
//        int hours = (hoursInput == null ? 0 : hoursInput % 24);
//        int minutes = (minutesInput == null ? 0 : minutesInput);
//
//        // Use the input to create a date object with proper timezone
//        MutableDateTime date = new MutableDateTime(year, month, day, hours, minutes, 0, 0,
//                DateTimeZone.forTimeZone(getClientTimeZone()));
//
//        // Adjust for halfday if needed
//        if (use12HourFormat()) {
//            int halfday = (amOrPmInput == AM_PM.PM ? 1 : 0);
//            date.set(DateTimeFieldType.halfdayOfDay(), halfday);
//            date.set(DateTimeFieldType.hourOfHalfday(), hours % 12);
//        }
//
//        // The date will be in the server's timezone
//        return newDateInstance(date.getMillis());
//    }
}
