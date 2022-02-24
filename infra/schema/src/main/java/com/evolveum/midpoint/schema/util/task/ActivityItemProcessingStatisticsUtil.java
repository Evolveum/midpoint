/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter;
import com.evolveum.midpoint.schema.statistics.ActivityItemProcessingStatisticsPrinter;
import com.evolveum.midpoint.schema.statistics.OutcomeKeyedCounterTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityTreeUtil.ActivityStateInContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

public class ActivityItemProcessingStatisticsUtil {

    @SuppressWarnings("unused")
    public static int getItemsProcessedWithFailure(ActivityItemProcessingStatisticsType info) {
        if (info != null) {
            return getCounts(toCollection(info), OutcomeKeyedCounterTypeUtil::isFailure);
        } else {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static int getItemsProcessedWithSuccess(ActivityItemProcessingStatisticsType info) {
        if (info != null) {
            return getCounts(toCollection(info), OutcomeKeyedCounterTypeUtil::isSuccess);
        } else {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static int getItemsProcessedWithSkip(ActivityItemProcessingStatisticsType info) {
        if (info != null) {
            return getCounts(toCollection(info), OutcomeKeyedCounterTypeUtil::isSkip);
        } else {
            return 0;
        }
    }

    public static int getItemsProcessedWithFailureShallow(ActivityItemProcessingStatisticsType info) {
        return getCounts(toCollection(info), OutcomeKeyedCounterTypeUtil::isFailure);
    }

    public static int getItemsProcessedWithSuccessShallow(ActivityItemProcessingStatisticsType info) {
        return getCounts(toCollection(info), OutcomeKeyedCounterTypeUtil::isSuccess);
    }

    static Collection<ActivityItemProcessingStatisticsType> toCollection(ActivityItemProcessingStatisticsType info) {
        return info != null ? singleton(info) : emptySet();
    }

    public static int getItemsProcessedWithSkipShallow(ActivityItemProcessingStatisticsType info) {
        return getCounts(toCollection(info), OutcomeKeyedCounterTypeUtil::isSkip);
    }

    public static int getItemsProcessedShallow(ActivityItemProcessingStatisticsType info) {
        if (info == null) {
            return 0;
        } else {
            return getCounts(singleton(info), set -> true);
        }
    }

    public static int getItemsProcessed(ActivityItemProcessingStatisticsType itemProcessingStatistics) {
        return itemProcessingStatistics != null ?
                getCounts(List.of(itemProcessingStatistics), set -> true) :
                0;
    }

    public static int getItemsProcessed(@NotNull Collection<ActivityStateType> states) {
        return getCounts(
                getItemProcessingStatisticsCollection(states),
                set -> true);
    }

    public static int getItemsProcessedWithFailure(@NotNull Collection<ActivityStateType> states) {
        return getCounts(
                getItemProcessingStatisticsCollection(states),
                OutcomeKeyedCounterTypeUtil::isFailure);
    }

    @NotNull
    static List<ActivityItemProcessingStatisticsType> getItemProcessingStatisticsCollection(
            @NotNull Collection<ActivityStateType> states) {
        return states.stream()
                .map(ActivityItemProcessingStatisticsUtil::getItemProcessingStatistics)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static ActivityItemProcessingStatisticsType getItemProcessingStatistics(ActivityStateType state) {
        return state != null && state.getStatistics() != null ?
                state.getStatistics().getItemProcessing() : null;
    }

    public static int getErrorsShallow(ActivityItemProcessingStatisticsType info) {
        if (info == null) {
            return 0;
        } else {
            return getCounts(singleton(info), OutcomeKeyedCounterTypeUtil::isFailure);
        }
    }

    public static int getErrors(Collection<ActivityStateType> states) {
        return getCounts(
                getItemProcessingStatisticsCollection(states),
                OutcomeKeyedCounterTypeUtil::isFailure);
    }

    public static double getProcessingTime(ActivityItemProcessingStatisticsType info) {
        if (info != null) {
            return getProcessingTime(singleton(info), set -> true);
        } else {
            return 0;
        }
    }

    public static double getProcessingTime(@NotNull Collection<ActivityStateType> states) {
        return getProcessingTime(
                getItemProcessingStatisticsCollection(states),
                set -> true);
    }

    /**
     * Returns sum of `count` values from processing information conforming to given predicate.
     */
    static int getCounts(Collection<ActivityItemProcessingStatisticsType> activities,
            Predicate<ProcessedItemSetType> itemSetFilter) {
        return activities.stream()
                .flatMap(component -> component.getProcessed().stream())
                .filter(Objects::nonNull)
                .filter(itemSetFilter)
                .mapToInt(p -> or0(p.getCount()))
                .sum();
    }

    private static double getProcessingTime(Collection<ActivityItemProcessingStatisticsType> parts,
            Predicate<ProcessedItemSetType> itemSetFilter) {
        return parts.stream()
                .flatMap(component -> component.getProcessed().stream())
                .filter(Objects::nonNull)
                .filter(itemSetFilter)
                .mapToDouble(p -> or0(p.getDuration()))
                .sum();
    }

    /**
     * Returns object that was last processed by given task in item set defined by the filter.
     *
     * TODO this should operate on a tree!
     */
    public static String getLastProcessedObjectName(ActivityItemProcessingStatisticsType info,
            Predicate<ProcessedItemSetType> itemSetFilter) {
        ProcessedItemType last = getLastProcessedObject(info, itemSetFilter);
        return last != null ? last.getName() : null;
    }

    /**
     * Returns message about the item that was last processed by given task in item set defined by the filter.
     *
     * TODO this should operate on a tree!
     */
    public static String getLastProcessedItemMessage(ActivityItemProcessingStatisticsType info,
            Predicate<ProcessedItemSetType> itemSetFilter) {
        ProcessedItemType last = getLastProcessedObject(info, itemSetFilter);
        return last != null ? last.getMessage() : null;
    }

    /**
     * Returns object OID that was last processed by given task in item set defined by the filter.
     *
     * TODO this should operate on a tree!
     */
    public static String getLastProcessedObjectOid(ActivityItemProcessingStatisticsType info,
            Predicate<ProcessedItemSetType> itemSetFilter) {
        ProcessedItemType lastSuccess = getLastProcessedObject(info, itemSetFilter);
        return lastSuccess != null ? lastSuccess.getOid() : null;
    }

    @Nullable
    private static ProcessedItemType getLastProcessedObject(ActivityItemProcessingStatisticsType info, Predicate<ProcessedItemSetType> itemSetFilter) {
        return toCollection(info).stream()
                .flatMap(component -> component.getProcessed().stream())
                .filter(itemSetFilter)
                .map(ProcessedItemSetType::getLastItem)
                .filter(Objects::nonNull)
                .max(Comparator.nullsFirst(Comparator.comparing(item -> XmlTypeConverter.toMillis(item.getEndTimestamp()))))
                .orElse(null);
    }

    /**
     * Returns object that was last successfully processed by given physical task.
     *
     * TODO optimize (avoid full summarization)
     */
    public static String getLastSuccessObjectName(@NotNull TaskType task) {
        return getLastSuccessObjectName(
                getSummarizedStatistics(task.getActivityState()));
    }

    public static @NotNull ActivityItemProcessingStatisticsType getSummarizedStatistics(
            @Nullable TaskActivityStateType taskActivityState) {
        if (taskActivityState != null) {
            return summarize(
                    ActivityTreeUtil.getAllLocalStates(taskActivityState).stream()
                            .map(ActivityItemProcessingStatisticsUtil::getItemProcessingStatistics)
                            .filter(Objects::nonNull));

        } else {
            return new ActivityItemProcessingStatisticsType(PrismContext.get());
        }
    }

    public static String getLastSuccessObjectName(ActivityItemProcessingStatisticsType stats) {
        return getLastProcessedObjectName(stats, OutcomeKeyedCounterTypeUtil::isSuccess);
    }

    public static Double toSeconds(Long time) {
        return time != null ? time / 1000.0 : null;
    }

    /** Like {@link List#add(Object)} but returns the value. */
    public static <T> T add(List<T> list, T value) {
        list.add(value);
        return value;
    }

    public static List<ActivityItemProcessingStatisticsType> getItemProcessingStatisticsFromStates(
            @NotNull Collection<ActivityStateType> states) {
        return states.stream()
                .map(ActivityItemProcessingStatisticsUtil::getItemProcessingStatistics)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static @NotNull ActivityItemProcessingStatisticsType summarize(
            @NotNull Collection<ActivityItemProcessingStatisticsType> deltas) {
        return summarize(deltas.stream());
    }

    public static @NotNull ActivityItemProcessingStatisticsType summarize(
            @NotNull Stream<ActivityItemProcessingStatisticsType> deltas) {
        ActivityItemProcessingStatisticsType sum = new ActivityItemProcessingStatisticsType(PrismContext.get());
        deltas.forEach(delta -> addTo(sum, delta));
        return sum;
    }

    /** Updates specified summary with given delta. */
    public static void addTo(@NotNull ActivityItemProcessingStatisticsType sum,
            @Nullable ActivityItemProcessingStatisticsType delta) {
        if (delta != null) {
            addToNotNull(sum, delta);
        }
    }

    /** Adds two "part information" */
    private static void addToNotNull(@NotNull ActivityItemProcessingStatisticsType sum,
            @NotNull ActivityItemProcessingStatisticsType delta) {
        addProcessed(sum.getProcessed(), delta.getProcessed());
        addCurrent(sum.getCurrent(), delta.getCurrent());
        addRunRecords(sum, delta);
    }

    private static void addRunRecords(@NotNull ActivityItemProcessingStatisticsType sum,
            @NotNull ActivityItemProcessingStatisticsType delta) {
        List<ActivityRunRecordType> nonOverlappingRecords =
                new WallClockTimeComputer(sum.getRun(), delta.getRun())
                        .getNonOverlappingRecords();
        sum.getRun().clear();
        nonOverlappingRecords.sort(Comparator.comparing(r -> XmlTypeConverter.toMillis(r.getStartTimestamp())));
        sum.getRun().addAll(CloneUtil.cloneCollectionMembersWithoutIds(nonOverlappingRecords));
    }

    /** Adds `processed` items information */
    private static void addProcessed(@NotNull List<ProcessedItemSetType> sumSets, @NotNull List<ProcessedItemSetType> deltaSets) {
        for (ProcessedItemSetType deltaSet : deltaSets) {
            ProcessedItemSetType matchingSet =
                    findOrCreateMatchingSet(sumSets, deltaSet.getOutcome());
            addMatchingProcessedItemSets(matchingSet, deltaSet);
        }
    }

    private static ProcessedItemSetType findOrCreateMatchingSet(
            @NotNull List<ProcessedItemSetType> list, QualifiedItemProcessingOutcomeType outcome) {
        return list.stream()
                .filter(item -> Objects.equals(item.getOutcome(), outcome))
                .findFirst()
                .orElseGet(
                        () -> add(list, new ProcessedItemSetType().outcome(outcome)));
    }

    /** Adds matching processed item sets: adds counters and determines the latest `lastItem`. */
    private static void addMatchingProcessedItemSets(@NotNull ProcessedItemSetType sum, @NotNull ProcessedItemSetType delta) {
        sum.setCount(or0(sum.getCount()) + or0(delta.getCount()));
        sum.setDuration(or0(sum.getDuration()) + or0(delta.getDuration()));
        if (delta.getLastItem() != null) {
            if (sum.getLastItem() == null ||
                    XmlTypeConverter.isAfterNullLast(delta.getLastItem().getEndTimestamp(), sum.getLastItem().getEndTimestamp())) {
                sum.setLastItem(delta.getLastItem());
            }
        }
    }

    /** Adds `current` items information (simply concatenates the lists). */
    private static void addCurrent(List<ProcessedItemType> sum, List<ProcessedItemType> delta) {
        sum.addAll(CloneUtil.cloneCollectionMembersWithoutIds(delta)); // to avoid problems with parent and IDs
    }

    public static boolean hasItemProcessingInformation(@NotNull ActivityStateInContext cState) {
        if (cState.getWorkerStates() != null) {
            return cState.getWorkerStates().stream()
                    .anyMatch(ActivityItemProcessingStatisticsUtil::hasItemProcessingInformation);
        } else {
            return hasItemProcessingInformation(cState.getActivityState());
        }
    }

    public static boolean hasItemProcessingInformation(@Nullable ActivityStateType state) {
        return state != null && state.getStatistics() != null && state.getStatistics().getItemProcessing() != null;
    }

    public static String format(@Nullable ActivityItemProcessingStatisticsType source) {
        return format(source, null);
    }

    /** Formats the information. */
    public static String format(@Nullable ActivityItemProcessingStatisticsType source,
            AbstractStatisticsPrinter.Options options) {
        ActivityItemProcessingStatisticsType information = source != null ? source : new ActivityItemProcessingStatisticsType();
        return new ActivityItemProcessingStatisticsPrinter(information, options).print();
    }
}
