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
import java.util.Comparator;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Prints structured task progress.
 */
public class StructuredTaskProgressPrinter extends AbstractStatisticsPrinter<StructuredTaskProgressType> {

    private static final String STATE_CLOSED = "Closed";
    private static final String STATE_OPEN = "Open";

    public StructuredTaskProgressPrinter(StructuredTaskProgressType information, Options options) {
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

    private void createDataFromCounters(TaskPartProgressType partProgress, List<TaskProgressCounterType> countersOriginal,
            String state) {

        ArrayList<TaskProgressCounterType> counters = new ArrayList<>(countersOriginal);
        counters.sort(Comparator.comparing(StatisticsUtil::getOutcomeSortingKey)); // FIXME

        for (TaskProgressCounterType counter : counters) {
            Data.Record record = data.createRecord();
            record.add(partProgress.getPartUri());
            record.add(state);
            record.add(StatisticsUtil.getOutcome(counter));
            record.add(StatisticsUtil.getOutcomeQualifierUri(counter));
            record.add(counter.getCount());
        }
    }

    private void createFormatting() {
        initFormatting();
        addColumn("Part", LEFT, formatString());
        addColumn("State", LEFT, formatString());
        addColumn("Outcome", LEFT, formatString());
        addColumn("Qualifier", LEFT, formatString());
        addColumn("Count", RIGHT, formatInt());
    }
}
