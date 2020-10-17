/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.LEFT;
import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.RIGHT;

import java.util.Locale;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * Prints iterative task performance information.
 */
public class IterativeTaskInformationPrinter extends AbstractStatisticsPrinter<IterativeTaskInformationType> {

    public IterativeTaskInformationPrinter(@NotNull IterativeTaskInformationType information, Options options) {
        super(information, options, null, null);
    }

    public String print() {
        createData();
        createFormatting();

        StringBuilder sb = new StringBuilder();
        sb.append(applyFormatting());
        sb.append("\n");

        if (information.getCurrentObjectStartTimestamp() != null) {
            sb.append(String.format(Locale.US, "Current: %s started at %tc\n",
                    formatObject(
                            information.getCurrentObjectType(),
                            information.getCurrentObjectName(),
                            information.getCurrentObjectDisplayName(),
                            information.getCurrentObjectOid()),
                    XmlTypeConverter.toDate(information.getCurrentObjectStartTimestamp())));
        }
        return sb.toString();
    }

    private void createData() {
        initData();
        boolean hasLastSuccess = information.getLastSuccessEndTimestamp() != null;
        boolean hasLastFailure = information.getLastFailureEndTimestamp() != null;

        Data.Record success = data.createRecord();
        success.add("Successfully processed");
        success.add(information.getTotalSuccessCount());
        success.add(information.getTotalSuccessDuration());
        success.add(div(information.getTotalSuccessDuration(), information.getTotalSuccessCount()));

        success.add(nullIfFalse(hasLastSuccess, formatObject(information.getLastSuccessObjectType(), information.getLastSuccessObjectName(), information.getLastSuccessObjectDisplayName(), information.getLastSuccessObjectOid())));
        success.add(nullIfFalse(hasLastSuccess, XmlTypeConverter.toDate(information.getLastSuccessEndTimestamp())));
        success.add(nullIfFalse(hasLastSuccess, information.getLastSuccessDuration()));

        Data.Record failure = data.createRecord();
        failure.add("Failed");
        failure.add(information.getTotalFailureCount());
        failure.add(information.getTotalFailureDuration());
        failure.add(div(information.getTotalFailureDuration(), information.getTotalFailureCount()));

        failure.add(nullIfFalse(hasLastFailure, formatObject(information.getLastFailureObjectType(), information.getLastFailureObjectName(), information.getLastFailureObjectDisplayName(), information.getLastFailureObjectOid())));
        failure.add(nullIfFalse(hasLastFailure, XmlTypeConverter.toDate(information.getLastFailureEndTimestamp())));
        failure.add(nullIfFalse(hasLastFailure, information.getLastFailureDuration()));
    }

    private String formatObject(QName type, String name, String displayName, String oid) {
        return String.format("%s:%s (%s, %s)", QNameUtil.getLocalPart(type), name, displayName, oid);
    }

    private void createFormatting() {
        initFormatting();
        addColumn("Status", LEFT, formatString());
        addColumn("Count", RIGHT, formatInt());
        addColumn("Total time (ms)", RIGHT, formatInt());
        addColumn("Per object", RIGHT, formatFloat1());
        addColumn("Last object", LEFT, formatString());
        addColumn("Finished on", LEFT, formatString());
        addColumn("Finished in (ms)", RIGHT, formatInt());
    }
}
