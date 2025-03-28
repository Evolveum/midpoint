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
 * @param duration if duration is null, it means that it is not fixed duration, but it is calculated from as difference
 * between current time and time provided {@link #timeSupplier}
 * @param anchor if not provided, defaults to {@link DurationAnchor#TO}
 * @param timeSupplier if not provided, default supplier return current time in millis.
 * @param text
 */
public record NamedIntervalPreset(
        @Nullable Duration duration,
        @NotNull DurationAnchor anchor,
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

    public static final List<NamedIntervalPreset> DEFAULT_PRESETS = List.of(
            new NamedIntervalPreset(
                    XmlTypeConverter.createDuration("PT15M"),
                    new SingleLocalizableMessage("NamedIntervalPreset.last15Minutes")),
            new NamedIntervalPreset(
                    XmlTypeConverter.createDuration("PT30M"),
                    new SingleLocalizableMessage("NamedIntervalPreset.last30Minutes")),
            new NamedIntervalPreset(
                    XmlTypeConverter.createDuration("PT1H"),
                    new SingleLocalizableMessage("NamedIntervalPreset.last1Hour")),
            new NamedIntervalPreset(
                    XmlTypeConverter.createDuration("PT6H"),
                    new SingleLocalizableMessage("NamedIntervalPreset.last6Hours")),
            new NamedIntervalPreset(
                    XmlTypeConverter.createDuration("PT12H"),
                    new SingleLocalizableMessage("NamedIntervalPreset.last12Hours")),
            new NamedIntervalPreset(
                    XmlTypeConverter.createDuration("P1D"),
                    new SingleLocalizableMessage("NamedIntervalPreset.last1Day")),
            new NamedIntervalPreset(
                    XmlTypeConverter.createDuration("P7D"),
                    new SingleLocalizableMessage("NamedIntervalPreset.last7Days")),
            new NamedIntervalPreset(
                    XmlTypeConverter.createDuration("P1M"),
                    new SingleLocalizableMessage("NamedIntervalPreset.last1Month")),
            new NamedIntervalPreset(
                    XmlTypeConverter.createDuration("P3M"),
                    new SingleLocalizableMessage("NamedIntervalPreset.last3Months")),
            new NamedIntervalPreset(
                    XmlTypeConverter.createDuration("P1Y"),
                    new SingleLocalizableMessage("NamedIntervalPreset.last1Year"))
    );

    public DurationAnchor getAnchorOrDefault() {
        return anchor() != null ? anchor() : DurationAnchor.TO;
    }

    public long getTimeOrDefault() {
        Long time = timeSupplier != null ? timeSupplier().get() : System.currentTimeMillis();
        if (time != null) {
            return time;
        }

        return System.currentTimeMillis();
    }

    public Duration getDurationOrDefault() {
        Duration duration = duration();
        if (duration != null) {
            return duration;
        }

        long time = getTimeOrDefault();

        long durationMillis = Math.abs(System.currentTimeMillis() - time);
        return XmlTypeConverter.createDuration(durationMillis);
    }
}
