/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.LEFT;
import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.RIGHT;
import static com.evolveum.midpoint.schema.statistics.OutcomeKeyedCounterTypeUtil.getOutcome;
import static com.evolveum.midpoint.schema.statistics.OutcomeKeyedCounterTypeUtil.getOutcomeQualifierUri;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import org.jetbrains.annotations.NotNull;

/**
 * Prints synchronization information (new, i.e. transition-based).
 */
public class SynchronizationInformationPrinter extends AbstractStatisticsPrinter<SynchronizationInformationType> {

    public SynchronizationInformationPrinter(@NotNull SynchronizationInformationType information, Options options) {
        super(information, options, null, null);
    }

    public String print() {
        createData();
        createFormatting();

        return applyFormatting() + "\n";
    }

    private void createData() {
        initData();

        ArrayList<SynchronizationSituationTransitionType> transitions = new ArrayList<>(information.getTransition());
        transitions.sort(createComparator());

        for (SynchronizationSituationTransitionType transition : transitions) {
            List<OutcomeKeyedCounterType> counters = new ArrayList<>(transition.getCounter());
            counters.sort(OutcomeKeyedCounterTypeUtil.createOutcomeKeyedCounterComparator());

            for (OutcomeKeyedCounterType counter : counters) {
                Data.Record record = data.createRecord();
                record.add(transition.getOnProcessingStart());
                record.add(transition.getOnSynchronizationStart());
                record.add(transition.getOnSynchronizationEnd());
                record.add(transition.getExclusionReason());
                record.add(getOutcome(counter));
                record.add(getOutcomeQualifierUri(counter));
                record.add(counter.getCount());
            }
        }
    }

    private static Comparator<? super SynchronizationSituationTransitionType> createComparator() {
        return (o1, o2) ->
                ComparisonChain.start()
                        .compare(o1.getOnProcessingStart(), o2.getOnProcessingStart(), Ordering.natural().nullsLast())
                        .compare(o1.getOnSynchronizationStart(), o2.getOnSynchronizationStart(), Ordering.natural().nullsLast())
                        .compare(o1.getOnSynchronizationEnd(), o2.getOnSynchronizationEnd(), Ordering.natural().nullsLast())
                        .compare(o1.getExclusionReason(), o2.getExclusionReason(), Ordering.natural().nullsLast())
                        .result();
    }


    private void createFormatting() {
        initFormatting();
        addColumn("On Start", LEFT, formatString());
        addColumn("On Sync Start", LEFT, formatString());
        addColumn("On Sync End", LEFT, formatString());
        addColumn("Exclusion Reason", LEFT, formatString());
        addColumn("Outcome", LEFT, formatString());
        addColumn("Qualifier", LEFT, formatString());
        addColumn("Count", RIGHT, formatInt());
    }
}
