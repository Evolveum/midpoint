/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.LEFT;
import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.RIGHT;

/**
 * Prints activity progress information.
 *
 * Does not support sorting by time, as this is not relevant for activity progress.
 * Also does not support sorting by counters. This could be done in the future.
 * Also does not support expected totals (yet). It does not fit into the table.
 */
public class ActivityProgressPrinter extends AbstractStatisticsPrinter<ActivityProgressType> {

    public ActivityProgressPrinter(@NotNull ActivityProgressType information, Options options) {
        super(information, options, null, null);
    }

    @Override
    public void prepare() {
        createData();
        createFormatting();
    }

    private void createData() {
        initData();
        createData(information);
    }

    private void createData(@NotNull ActivityProgressType component) {
        addData(ActivityProgressType.F_UNCOMMITTED, component.getUncommitted());
        addData(ActivityProgressType.F_COMMITTED, component.getCommitted());
    }

    private void addData(@NotNull ItemName state, @NotNull List<OutcomeKeyedCounterType> counters) {

        List<OutcomeKeyedCounterType> sortedCounters = new ArrayList<>(counters);
        sortedCounters.sort(OutcomeKeyedCounterTypeUtil.createOutcomeKeyedCounterComparator());

        for (OutcomeKeyedCounterType counter : sortedCounters) {
            Data.Record record = data.createRecord();
            record.add(state.getLocalPart());
            record.add(OutcomeKeyedCounterTypeUtil.getOutcome(counter));
            record.add(OutcomeKeyedCounterTypeUtil.getOutcomeQualifierUri(counter));
            record.add(counter.getCount());
        }
    }

    private void createFormatting() {
        initFormatting();
        addColumn("State", LEFT, formatString());
        addColumn("Outcome", LEFT, formatString());
        addColumn("Qualifier", LEFT, formatString());
        addColumn("Count", RIGHT, formatInt());
    }
}
