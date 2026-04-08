/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web;

import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertSame;
import static org.testng.AssertJUnit.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.web.model.XmlGregorianCalendarModel;

import org.apache.wicket.model.IModel;
import org.testng.annotations.Test;

public class TestXmlGregorianCalendarModel extends AbstractGuiUnitTest {

    private static final TimeZone PRAGUE = TimeZone.getTimeZone("Europe/Prague");

    @Test
    void testKeepStoredValueWhenGuiOnlyDropsSeconds() {
        XMLGregorianCalendar stored = XmlTypeConverter.createXMLGregorianCalendar("2025-05-15T09:32:12+02:00");

        GregorianCalendar sameMinuteDifferentCalendar = stored.toGregorianCalendar(PRAGUE, null, null);
        sameMinuteDifferentCalendar.setLenient(false); // makes GregorianCalendar.equals(...) differ

        XMLGregorianCalendar wrapped = new CalendarOverride(stored, sameMinuteDifferentCalendar);

        IModel<XMLGregorianCalendar> backing = modelOf(wrapped);
        XmlGregorianCalendarModel model = new XmlGregorianCalendarModel(backing);

        Date submitted = minuteDate(32);

        GregorianCalendar current = wrapped.toGregorianCalendar();
        zeroSubMinuteFields(current);

        GregorianCalendar submittedCalendar = XmlTypeConverter.createXMLGregorianCalendar(submitted).toGregorianCalendar(PRAGUE, null, null);
        zeroSubMinuteFields(submittedCalendar);

        assertEquals(current.getTimeInMillis(), submittedCalendar.getTimeInMillis());
        assertNotEquals(current, submittedCalendar);

        model.setObject(submitted);

        assertSame(wrapped, backing.getObject());
        assertEquals(12, backing.getObject().getSecond());
        assertEquals("2025-05-15T09:32:12+02:00", backing.getObject().toXMLFormat());
    }

    @Test
    void testReplaceValueWhenMinuteChanges() {
        XMLGregorianCalendar stored = XmlTypeConverter.createXMLGregorianCalendar("2025-05-15T09:32:12+02:00");

        IModel<XMLGregorianCalendar> backing = modelOf(stored);
        XmlGregorianCalendarModel model = new XmlGregorianCalendarModel(backing);

        model.setObject(minuteDate(33));

        assertEquals("2025-05-15T09:33:00.000+02:00", backing.getObject().toXMLFormat());
    }

    private static Date minuteDate(int minute) {
        Calendar calendar = Calendar.getInstance(PRAGUE);
        calendar.clear();
        calendar.set(2025, Calendar.MAY, 15, 9, minute, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private static void zeroSubMinuteFields(Calendar calendar) {
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private static IModel<XMLGregorianCalendar> modelOf(XMLGregorianCalendar initialValue) {
        return new IModel<>() {
            private XMLGregorianCalendar object = initialValue;

            @Override
            public XMLGregorianCalendar getObject() {
                return object;
            }

            @Override
            public void setObject(XMLGregorianCalendar object) {
                this.object = object;
            }
        };
    }

    //Reproduces the old failure where calendars representing the same normalized
    //minute were not equal according to GregorianCalendar.equals(...).
    private static final class CalendarOverride extends XMLGregorianCalendar {
        private final XMLGregorianCalendar delegate;
        private final GregorianCalendar overriddenCalendar;

        private CalendarOverride(XMLGregorianCalendar delegate, GregorianCalendar overriddenCalendar) {
            this.delegate = delegate;
            this.overriddenCalendar = overriddenCalendar;
        }

        @Override
        public GregorianCalendar toGregorianCalendar() {
            return (GregorianCalendar) overriddenCalendar.clone();
        }

        @Override
        public Object clone() {
            return new CalendarOverride((XMLGregorianCalendar) delegate.clone(), (GregorianCalendar) overriddenCalendar.clone());
        }

        @Override public void clear() { delegate.clear(); }
        @Override public void reset() { delegate.reset(); }
        @Override public void setYear(BigInteger year) { delegate.setYear(year); }
        @Override public void setYear(int year) { delegate.setYear(year); }
        @Override public void setMonth(int month) { delegate.setMonth(month); }
        @Override public void setDay(int day) { delegate.setDay(day); }
        @Override public void setTimezone(int offset) { delegate.setTimezone(offset); }
        @Override public void setHour(int hour) { delegate.setHour(hour); }
        @Override public void setMinute(int minute) { delegate.setMinute(minute); }
        @Override public void setSecond(int second) { delegate.setSecond(second); }
        @Override public void setMillisecond(int millisecond) { delegate.setMillisecond(millisecond); }
        @Override public void setFractionalSecond(BigDecimal fractional) { delegate.setFractionalSecond(fractional); }
        @Override public BigInteger getEon() { return delegate.getEon(); }
        @Override public int getYear() { return delegate.getYear(); }
        @Override public BigInteger getEonAndYear() { return delegate.getEonAndYear(); }
        @Override public int getMonth() { return delegate.getMonth(); }
        @Override public int getDay() { return delegate.getDay(); }
        @Override public int getTimezone() { return delegate.getTimezone(); }
        @Override public int getHour() { return delegate.getHour(); }
        @Override public int getMinute() { return delegate.getMinute(); }
        @Override public int getSecond() { return delegate.getSecond(); }
        @Override public BigDecimal getFractionalSecond() { return delegate.getFractionalSecond(); }
        @Override public int compare(XMLGregorianCalendar cal) { return delegate.compare(cal); }
        @Override public XMLGregorianCalendar normalize() { return delegate.normalize(); }
        @Override public String toXMLFormat() { return delegate.toXMLFormat(); }
        @Override public QName getXMLSchemaType() { return delegate.getXMLSchemaType(); }
        @Override public boolean isValid() { return delegate.isValid(); }
        @Override public void add(Duration duration) { delegate.add(duration); }
        @Override
        public GregorianCalendar toGregorianCalendar(TimeZone timezone, Locale locale, XMLGregorianCalendar defaults) {
            return delegate.toGregorianCalendar(timezone, locale, defaults);
        }
        @Override public TimeZone getTimeZone(int defaultZoneoffset) { return delegate.getTimeZone(defaultZoneoffset); }
        @Override public int getMillisecond() { return delegate.getMillisecond(); }
    }
}
