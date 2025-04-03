/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.io.Serializable;
import java.util.List;
import javax.xml.datatype.Duration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;

/**
 * @param duration
 * @param anchor if null, then the meaning is interval is not defined
 * @param timeSupplier if supplier returns null value, then the meaning is interval is not defined
 * @param text
 */
public record NamedIntervalPreset(
        @Nullable Duration duration,
        @Nullable DurationAnchor anchor,
        @NotNull SerializableSupplier<Long> timeSupplier,
        @NotNull LocalizableMessage text)
        implements Serializable {

    public NamedIntervalPreset(Duration duration, LocalizableMessage text) {
        this(duration, DurationAnchor.TO, () -> System.currentTimeMillis(), text);
    }

    public enum DurationAnchor {

        FROM,

        TO
    }

    public static final NamedIntervalPreset ALL = new NamedIntervalPreset(
            null,
            null,
            () -> null,
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
