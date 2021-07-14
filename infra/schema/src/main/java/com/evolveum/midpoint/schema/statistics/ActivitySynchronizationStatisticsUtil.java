/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.util.SyncSituationUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivitySynchronizationStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationTransitionType;

import java.util.ArrayList;
import java.util.List;

public class ActivitySynchronizationStatisticsUtil {

    public static void addTo(ActivitySynchronizationStatisticsType sum, @Nullable ActivitySynchronizationStatisticsType delta) {
        if (delta != null) {
            addTransitions(sum, delta);
        }
    }

    private static void addTransitions(@NotNull ActivitySynchronizationStatisticsType sum,
            @NotNull ActivitySynchronizationStatisticsType delta) {
        for (SynchronizationSituationTransitionType deltaTransition : delta.getTransition()) {
            addTransition(sum.getTransition(), deltaTransition);
        }
    }

    private static void addTransition(@NotNull List<SynchronizationSituationTransitionType> sumTransitions,
            @NotNull SynchronizationSituationTransitionType deltaTransition) {
        SynchronizationSituationTransitionType existingTransition = SyncSituationUtil.findMatchingTransition(sumTransitions,
                deltaTransition.getOnProcessingStart(), deltaTransition.getOnSynchronizationStart(),
                deltaTransition.getOnSynchronizationEnd(), deltaTransition.getExclusionReason());
        if (existingTransition != null) {
            addTo(existingTransition, deltaTransition);
        } else {
            sumTransitions.add(deltaTransition.cloneWithoutId());
        }
    }

    private static void addTo(@NotNull SynchronizationSituationTransitionType sum,
            @NotNull SynchronizationSituationTransitionType delta) {
        OutcomeKeyedCounterTypeUtil.addCounters(sum.getCounter(), delta.getCounter());
    }

    public static String format(@Nullable ActivitySynchronizationStatisticsType source) {
        return new SynchronizationInformationPrinter(source != null ? source : new ActivitySynchronizationStatisticsType(), null)
                .print();
    }

    public static @NotNull List<SynchronizationSituationTransitionType> summarize(
            @NotNull List<SynchronizationSituationTransitionType> raw) {
        List<SynchronizationSituationTransitionType> summarized = new ArrayList<>();
        for (SynchronizationSituationTransitionType rawTransition : raw) {
            addTransition(summarized, rawTransition);
        }
        return summarized;
    }
}
