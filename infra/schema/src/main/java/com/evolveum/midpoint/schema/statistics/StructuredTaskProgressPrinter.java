/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.LEFT;
import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.RIGHT;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Prints structured task progress.
 */
public class StructuredTaskProgressPrinter extends AbstractStatisticsPrinter<StructuredTaskProgressType> {

    private static final String STATE_CLOSED = "Closed";
    private static final String STATE_OPEN = "Open";

    StructuredTaskProgressPrinter(StructuredTaskProgressType information, Options options) {
        super(information, options, null, null);
    }

    public String print() {
        createData();
        createFormatting();

        return "Current part " + information.getCurrentPartUri()
                + " (" + information.getCurrentPartNumber() + " of " + information.getExpectedParts() + "):\n\n"
                + applyFormatting() + "\n";
    }

    private void createData() {
        initData();
        information.getPart().forEach(this::createData);
    }

    private void createData(TaskPartProgressType partProgress) {
        createDataFromCounters(partProgress, partProgress.getClosed(), STATE_CLOSED);
        createDataFromCounters(partProgress, partProgress.getOpen(), STATE_OPEN);
    }

    private void createDataFromCounters(TaskPartProgressType partProgress, List<OutcomeKeyedCounterType> countersOriginal,
            String state) {

        ArrayList<OutcomeKeyedCounterType> counters = new ArrayList<>(countersOriginal);
        counters.sort(OutcomeKeyedCounterTypeUtil.createOutcomeKeyedCounterComparator());

        for (OutcomeKeyedCounterType counter : counters) {
            Data.Record record = data.createRecord();
            record.add(partProgress.getPartUri());
            record.add(partProgress.isComplete());
            record.add(state);
            record.add(OutcomeKeyedCounterTypeUtil.getOutcome(counter));
            record.add(OutcomeKeyedCounterTypeUtil.getOutcomeQualifierUri(counter));
            record.add(counter.getCount());
        }
    }

    private void createFormatting() {
        initFormatting();
        addColumn("Part", LEFT, formatString());
        addColumn("Complete", LEFT, formatString());
        addColumn("State", LEFT, formatString());
        addColumn("Outcome", LEFT, formatString());
        addColumn("Qualifier", LEFT, formatString());
        addColumn("Count", RIGHT, formatInt());
    }
}
