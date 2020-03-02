/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter.predicate;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;

/**
 * Methods for construction of various time-related assertion predicates.
 *
 * Experimental.
 */
public class TimeAssertionPredicates {

    public static AssertionPredicate<XMLGregorianCalendar> approximatelyCurrent(long tolerance) {
        return new GenericAssertionPredicate<>(
                value -> Math.abs(XmlTypeConverter.toMillis(value) - System.currentTimeMillis()) <= tolerance,
                value -> "value of " + value + " is not within tolerance of " + tolerance + " regarding current time");
    }

    public static AssertionPredicate<XMLGregorianCalendar> timeBetween(long min, long max) {
        return new SimplifiedGenericAssertionPredicate<>(
                value -> timeIsBetweenFailureMessage(XmlTypeConverter.toMillis(value), min, max));
    }

    private static String timeIsBetweenFailureMessage(long value, long min, long max) {
        if (value < min) {
            return "Time " + formatTime(value) + " is earlier than expected " + formatTime(min);
        } else if (value > max) {
            return "Time " + formatTime(value) + " is later than expected " + formatTime(max);
        } else {
            return null;
        }
    }

    @NotNull
    private static String formatTime(long value) {
        return new Date(value) + " (" + value + ")";
    }
}
