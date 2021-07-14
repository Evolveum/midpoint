/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SyncSituationUtil {

    public static SynchronizationSituationTransitionType findMatchingTransition(ActivitySynchronizationStatisticsType sum,
            SynchronizationSituationType onProcessingStart, SynchronizationSituationType onSynchronizationStart,
            SynchronizationSituationType onSynchronizationEnd, SynchronizationExclusionReasonType exclusionReason) {
        return findMatchingTransition(sum.getTransition(), onProcessingStart, onSynchronizationStart, onSynchronizationEnd,
                exclusionReason);
    }

    public static SynchronizationSituationTransitionType findMatchingTransition(
            @NotNull List<SynchronizationSituationTransitionType> transitions, SynchronizationSituationType onProcessingStart,
            SynchronizationSituationType onSynchronizationStart, SynchronizationSituationType onSynchronizationEnd,
            SynchronizationExclusionReasonType exclusionReason) {
        for (SynchronizationSituationTransitionType existingTransition : transitions) {
            if (matches(existingTransition, onProcessingStart, onSynchronizationStart, onSynchronizationEnd, exclusionReason)) {
                return existingTransition;
            }
        }
        return null;
    }

    private static boolean matches(SynchronizationSituationTransitionType t1, SynchronizationSituationType onProcessingStart,
            SynchronizationSituationType onSynchronizationStart, SynchronizationSituationType onSynchronizationEnd,
            SynchronizationExclusionReasonType exclusionReason) {
        return t1.getOnProcessingStart() == onProcessingStart &&
                t1.getOnSynchronizationStart() == onSynchronizationStart &&
                t1.getOnSynchronizationEnd() == onSynchronizationEnd &&
                t1.getExclusionReason() == exclusionReason;
    }
}
