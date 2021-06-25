/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import com.evolveum.midpoint.schema.statistics.OutcomeKeyedCounterTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.statistics.OutcomeKeyedCounterTypeUtil.getCounterFilter;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import static java.util.Collections.singleton;

public class ActivityProgressUtil {

    public static int getCurrentProgress(TaskType localTask, ActivityPath activityPath) {
        ActivityStateType state = ActivityStateUtil.getActivityState(localTask.getActivityState(), activityPath);
        return state != null ? getCurrentProgress(state.getProgress()) : 0;
    }

    public static int getCurrentProgress(ActivityProgressType progress) {
        if (progress != null) {
            return getCurrentProgressFromCollection(List.of(progress));
        } else {
            return 0;
        }
    }

    private static int getCurrentProgressFromCollection(List<ActivityProgressType> collection) {
        return getCounts(collection, c -> true, true) +
                getCounts(collection, c -> true, false);
    }

    public static int getCurrentProgress(@NotNull Collection<ActivityStateType> states) {
        return getCurrentProgressFromCollection(
                getProgressCollection(states));
    }

    public static int getProgressForOutcome(ActivityProgressType info, ItemProcessingOutcomeType outcome, boolean open) {
        if (info != null) {
            return ActivityProgressUtil.getCounts(singleton(info), getCounterFilter(outcome), open);
        } else {
            return 0;
        }
    }

    private static int getCounts(Collection<ActivityProgressType> activities,
            Predicate<OutcomeKeyedCounterType> counterFilter, boolean uncommitted) {
        return activities.stream()
                .flatMap(part -> (uncommitted ? part.getUncommitted() : part.getCommitted()).stream())
                .filter(Objects::nonNull)
                .filter(counterFilter)
                .mapToInt(p -> or0(p.getCount()))
                .sum();
    }

    public static void addTo(ActivityProgressType sum, ActivityProgressType delta) {
        OutcomeKeyedCounterTypeUtil.addCounters(sum.getCommitted(), delta.getCommitted());
        OutcomeKeyedCounterTypeUtil.addCounters(sum.getUncommitted(), delta.getUncommitted());
    }

    @NotNull
    private static List<ActivityProgressType> getProgressCollection(
            @NotNull Collection<ActivityStateType> states) {
        return states.stream()
                .map(ActivityProgressUtil::getProgress)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static ActivityProgressType getProgress(ActivityStateType state) {
        return state != null ? state.getProgress() : null;
    }
}
