/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;

public record NamedIntervalPreset(
        SerializableSupplier<Long> from,
        SerializableSupplier<Long> to,
        Duration duration,
        @NotNull LocalizableMessage text)
        implements Serializable, Comparable<NamedIntervalPreset> {

    public NamedIntervalPreset(Duration duration, LocalizableMessage text) {
        this(null, () -> System.currentTimeMillis(), duration, text);
    }

    @Override
    public int compareTo(@NotNull NamedIntervalPreset o) {
        Pair<XMLGregorianCalendar, XMLGregorianCalendar> interval = getInterval();
        Long from = millisFromCalendar(interval.getLeft());
        Long to = millisFromCalendar(interval.getRight());

        Pair<Long, Long> first = Pair.of(from, to);

        Pair<XMLGregorianCalendar, XMLGregorianCalendar> otherInterval = o.getInterval();
        Long otherFrom = millisFromCalendar(otherInterval.getLeft());
        Long otherTo = millisFromCalendar(otherInterval.getRight());

        Pair<Long, Long> second = Pair.of(otherFrom, otherTo);

        return Comparator.comparing((Pair<Long, Long> value) -> value.getLeft(), Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing((Pair<Long, Long> value) -> value.getRight(), Comparator.nullsFirst(Comparator.naturalOrder()))
                .compare(first, second);
    }

    private Long millisFromCalendar(XMLGregorianCalendar calendar) {
        return calendar != null ? calendar.toGregorianCalendar().getTimeInMillis() : null;
    }

    public Pair<XMLGregorianCalendar, XMLGregorianCalendar> getInterval() {
        Long fromMillis = from != null ? from.get() : null;
        Long toMillis = to != null ? to.get() : null;

        XMLGregorianCalendar fromDate = null;
        if (fromMillis != null) {
            fromDate = XmlTypeConverter.createXMLGregorianCalendar(fromMillis);
        }

        XMLGregorianCalendar toDate = null;
        if (toMillis != null) {
            toDate = XmlTypeConverter.createXMLGregorianCalendar(toMillis);
        }

        if (fromDate == null) {
            if (toDate != null && duration != null) {
                fromDate = XmlTypeConverter.createXMLGregorianCalendar(toMillis);
                fromDate.add(duration.negate());
            }
        }

        if (toDate == null) {
            if (fromDate != null && duration != null) {
                toDate = XmlTypeConverter.createXMLGregorianCalendar(fromMillis);
                toDate.add(duration);
            }
        }

        return Pair.of(fromDate, toDate);
    }

    public static final NamedIntervalPreset ALL = new NamedIntervalPreset(
            null,
            null,
            null,
            new SingleLocalizableMessage("NamedIntervalPreset.all"));

    public static final NamedIntervalPreset LAST_15_MINUTES = new NamedIntervalPreset(
            XmlTypeConverter.createDuration("PT15M"),
            new SingleLocalizableMessage("NamedIntervalPreset.last15Minutes"));

    public static final NamedIntervalPreset LAST_30_MINUTES = new NamedIntervalPreset(
            XmlTypeConverter.createDuration("PT30M"),
            new SingleLocalizableMessage("NamedIntervalPreset.last30Minutes"));

    public static final NamedIntervalPreset LAST_1_HOUR = new NamedIntervalPreset(
            XmlTypeConverter.createDuration("PT1H"),
            new SingleLocalizableMessage("NamedIntervalPreset.last1Hour"));

    public static final NamedIntervalPreset LAST_6_HOURS = new NamedIntervalPreset(
            XmlTypeConverter.createDuration("PT6H"),
            new SingleLocalizableMessage("NamedIntervalPreset.last6Hours"));

    public static final NamedIntervalPreset LAST_12_HOURS = new NamedIntervalPreset(
            XmlTypeConverter.createDuration("PT12H"),
            new SingleLocalizableMessage("NamedIntervalPreset.last12Hours"));

    public static final NamedIntervalPreset LAST_1_DAY = new NamedIntervalPreset(
            XmlTypeConverter.createDuration("P1D"),
            new SingleLocalizableMessage("NamedIntervalPreset.last1Day"));

    public static final NamedIntervalPreset LAST_7_DAYS = new NamedIntervalPreset(
            XmlTypeConverter.createDuration("P7D"),
            new SingleLocalizableMessage("NamedIntervalPreset.last7Days"));

    public static final NamedIntervalPreset LAST_1_MONTH = new NamedIntervalPreset(
            XmlTypeConverter.createDuration("P1M"),
            new SingleLocalizableMessage("NamedIntervalPreset.last1Month"));

    public static final NamedIntervalPreset LAST_3_MONTHS = new NamedIntervalPreset(
            XmlTypeConverter.createDuration("P3M"),
            new SingleLocalizableMessage("NamedIntervalPreset.last3Months"));

    public static final NamedIntervalPreset LAST_1_YEAR = new NamedIntervalPreset(
            XmlTypeConverter.createDuration("P1Y"),
            new SingleLocalizableMessage("NamedIntervalPreset.last1Year"));

    public static final List<NamedIntervalPreset> DEFAULT_PRESETS = List.of(
            ALL,
            LAST_15_MINUTES,
            LAST_30_MINUTES,
            LAST_1_HOUR,
            LAST_6_HOURS,
            LAST_12_HOURS,
            LAST_1_DAY,
            LAST_7_DAYS,
            LAST_1_MONTH,
            LAST_3_MONTHS,
            LAST_1_YEAR
    );
}
