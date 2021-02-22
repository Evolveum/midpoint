package com.evolveum.midpoint.task.quartzimpl.util;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Used by the node and task cleaners.
 */
public class TimeBoundary {

    private final Duration positiveDuration;
    private final XMLGregorianCalendar boundary;

    private TimeBoundary(Duration positiveDuration, XMLGregorianCalendar boundary) {
        this.positiveDuration = positiveDuration;
        this.boundary = boundary;
    }

    public static TimeBoundary compute(Duration rawDuration) {
        Duration positiveDuration = rawDuration.getSign() > 0 ? rawDuration : rawDuration.negate();
        XMLGregorianCalendar boundary = XmlTypeConverter.createXMLGregorianCalendar();
        boundary.add(positiveDuration.negate());
        return new TimeBoundary(positiveDuration, boundary);
    }

    public Duration getPositiveDuration() {
        return positiveDuration;
    }

    public XMLGregorianCalendar getBoundary() {
        return boundary;
    }
}
