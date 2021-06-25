/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.statistics.OutcomeKeyedCounterTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

public class ActivityItemProcessingStatisticsUtil {

    public static int getItemsProcessedWithFailure(ActivityItemProcessingStatisticsType info) {
        if (info != null) {
            return getCounts(getOne(info), OutcomeKeyedCounterTypeUtil::isFailure);
        } else {
            return 0;
        }
    }

    public static int getItemsProcessedWithSuccess(ActivityItemProcessingStatisticsType info) {
        if (info != null) {
            return getCounts(getOne(info), OutcomeKeyedCounterTypeUtil::isSuccess);
        } else {
            return 0;
        }
    }

    public static int getItemsProcessedWithSkip(ActivityItemProcessingStatisticsType info) {
        if (info != null) {
            return getCounts(getOne(info), OutcomeKeyedCounterTypeUtil::isSkip);
        } else {
            return 0;
        }
    }

    public static int getItemsProcessedWithFailureShallow(ActivityItemProcessingStatisticsType info) {
        return getCounts(getOne(info), OutcomeKeyedCounterTypeUtil::isFailure);
    }

    public static int getItemsProcessedWithSuccessShallow(ActivityItemProcessingStatisticsType info) {
        return getCounts(getOne(info), OutcomeKeyedCounterTypeUtil::isSuccess);
    }

    static Collection<ActivityItemProcessingStatisticsType> getOne(ActivityItemProcessingStatisticsType info) {
        return info != null ? singleton(info) : emptySet();
    }

    static Collection<ActivityItemProcessingStatisticsType> getAll(ActivityItemProcessingStatisticsType root) {
        List<ActivityItemProcessingStatisticsType> all = new ArrayList<>();
        if (root != null) {
            traverseIterationInformation(root, (p, info) -> all.add(info));
        }
        return all;
    }

    public static int getItemsProcessedWithSkipShallow(ActivityItemProcessingStatisticsType info) {
        return getCounts(getOne(info), OutcomeKeyedCounterTypeUtil::isSkip);
    }

    public static int getItemsProcessedShallow(ActivityItemProcessingStatisticsType info) {
        if (info == null) {
            return 0;
        } else {
            return getCounts(singleton(info), set -> true);
        }
    }

    public static int getItemsProcessed(@NotNull Collection<ActivityStateType> states) {
        return getCounts(
                getItemProcessingStatisticsCollection(states),
                set -> true);
    }

    @NotNull
    static List<ActivityItemProcessingStatisticsType> getItemProcessingStatisticsCollection(
            @NotNull Collection<ActivityStateType> states) {
        return states.stream()
                .map(ActivityItemProcessingStatisticsUtil::getItemProcessingStatistics)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static ActivityItemProcessingStatisticsType getItemProcessingStatistics(ActivityStateType state) {
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
        ProcessedItemType lastSuccess = getAll(info).stream()
                .flatMap(component -> component.getProcessed().stream())
                .filter(itemSetFilter)
                .map(ProcessedItemSetType::getLastItem)
                .filter(Objects::nonNull)
                .max(Comparator.nullsFirst(Comparator.comparing(item -> XmlTypeConverter.toMillis(item.getEndTimestamp()))))
                .orElse(null);
        return lastSuccess != null ? lastSuccess.getName() : null;
    }

    public static void traverseIterationInformation(@NotNull ActivityItemProcessingStatisticsType root,
            @NotNull BiConsumer<ActivityPath, ActivityItemProcessingStatisticsType> consumer) {
        traverseIterationInformationInternal(ActivityPath.empty(), root, consumer);
    }

    private static void traverseIterationInformationInternal(
            @NotNull ActivityPath currentPath,
            @NotNull ActivityItemProcessingStatisticsType currentNode,
            @NotNull BiConsumer<ActivityPath, ActivityItemProcessingStatisticsType> consumer) {
        consumer.accept(currentPath, currentNode);
//        currentNode.getActivity()
//                .forEach(child -> traverseIterationInformationInternal(
//                        currentPath.append(child.getIdentifier()),
//                        child,
//                        consumer));
    }

    /**
     * Returns object that was last successfully processed by given task.
     *
     * TODO this should operate on a tree!
     */
    public static String getLastSuccessObjectName(ActivityItemProcessingStatisticsType stats) {
        return getLastProcessedObjectName(stats, OutcomeKeyedCounterTypeUtil::isSuccess);
    }

    public static long getWallClockTime(IterativeTaskPartItemsProcessingInformationType info) {
        return new WallClockTimeComputer(info.getExecution())
                .getSummaryTime();
    }

    public static Double toSeconds(Long time) {
        return time != null ? time / 1000.0 : null;
    }
}
