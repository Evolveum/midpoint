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

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;

public record NamedIntervalPreset(Duration duration, LocalizableMessage text) implements Serializable {

    public static final List<NamedIntervalPreset> DEFAULT_PRESETS = List.of(
            new NamedIntervalPreset(
                    XmlTypeConverter.createDuration("PT15M"),
                    new SingleLocalizableMessage("NamedIntervalPreset.last15Minutes")),
            new NamedIntervalPreset(XmlTypeConverter.createDuration("PT30M"),
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
}
