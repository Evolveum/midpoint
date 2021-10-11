/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * System-wide clock. This class provides current time. By default is only proxies the usual system
 * current time functions. But it may be explicitly manipulated to artificially shift the time. This
 * of little value for a running system but it is really useful in the tests. Especially tests that
 * test time-based behavior. Using the Clock avoids changing the actual system time (or JVM's perception
 * of time) therefore the tests are easier to use and usual tools still make sense (e.g. log record timestamps
 * are correct).
 *
 * @author Radovan Semancik
 */
public class Clock {

    private static final Trace LOGGER = TraceManager.getTrace(Clock.class);

    private Long override = null;
    private Long overrideOffset = null;

    public long currentTimeMillis() {
        long time;
        if (override == null) {
            time = System.currentTimeMillis();
        } else {
            time = override;
        }
        if (overrideOffset != null) {
            time = time + overrideOffset;
        }
        return time;
    }

    public XMLGregorianCalendar currentTimeXMLGregorianCalendar() {
        long millis = currentTimeMillis();
        return XmlTypeConverter.createXMLGregorianCalendar(millis);
    }

    public boolean isPast(long date) {
        return currentTimeMillis() > date;
    }

    public boolean isPast(XMLGregorianCalendar date) {
        return isPast(XmlTypeConverter.toMillis(date));
    }


    public boolean isFuture(long date) {
        return currentTimeMillis() < date;
    }

    public boolean isFuture(XMLGregorianCalendar date) {
        return isFuture(XmlTypeConverter.toMillis(date));
    }

    public void override(long overrideTimestamp) {
        LOGGER.info("Clock override: {}", override);
        this.override = overrideTimestamp;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Clock current time: {}", currentTimeXMLGregorianCalendar());
        }
    }

    public void override(XMLGregorianCalendar overrideTimestamp) {
        override(XmlTypeConverter.toMillis(overrideTimestamp));
    }

    /**
     * Extends offset on top of existing offset.
     */
    public void overrideDuration(String durationString) {
        overrideDuration(XmlTypeConverter.createDuration(durationString));
    }

    /**
     * Extends offset on top of existing offset.
     */
    public void overrideDuration(Duration duration) {
        long millis = currentTimeMillis();
        XMLGregorianCalendar time = XmlTypeConverter.createXMLGregorianCalendar(millis);
        time.add(duration);
        long offset = XmlTypeConverter.toMillis(time) - millis;
        overrideDuration(offset);
    }

    /**
     * Extends offset on top of existing offset.
     */
    public void overrideDuration(Long offsetMillis) {
        if (overrideOffset == null) {
            overrideOffset = offsetMillis;
        } else {
            overrideOffset = overrideOffset + offsetMillis;
        }
    }

    public void overrideOffset(Long offsetMillis) {
        this.overrideOffset = offsetMillis;
    }

    public void resetOverride() {
        LOGGER.info("Clock override reset");
        this.override = null;
        this.overrideOffset = null;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Clock current time: {}", currentTimeXMLGregorianCalendar());
        }
    }

}
