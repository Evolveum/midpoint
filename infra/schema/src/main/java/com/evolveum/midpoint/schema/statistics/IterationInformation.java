/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.task.WallClockTimeComputer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * TODO reuse relevant parts and delete the class
 *
 */
@Deprecated
public class IterationInformation {

    @Deprecated
    public static final int LAST_FAILURES_KEPT = 30;

    public IterationInformation(@NotNull PrismContext prismContext) {
    }

    public IterationInformation(ActivityItemProcessingStatisticsType value, boolean collectExecutions,
                                @NotNull PrismContext prismContext) {
    }

    /** Returns a current value of this statistics. It is copied because of thread safety issues. */
    public synchronized ActivityItemProcessingStatisticsType getValueCopy() {
        throw new UnsupportedOperationException();
    }

    /** Like {@link List#add(Object)} but returns the value. */
    private static <T> T add(List<T> list, T value) {
        list.add(value);
        return value;
    }

    /** Updates specified summary with given delta. */
    public static void addTo(@NotNull ActivityItemProcessingStatisticsType sum, @Nullable ActivityItemProcessingStatisticsType delta) {
        if (delta != null) {
            addPartInformation(sum, delta);
//            addMatchingSubActivities(sum.getActivity(), delta.getActivity());
        }
    }

//    /** Looks for matching parts (created if necessary) and adds them. */
//    private static void addMatchingSubActivities(List<ActivityIterationInformationType> sumInfos,
//            List<ActivityIterationInformationType> deltaInfos) {
//        for (ActivityIterationInformationType deltaInfo : deltaInfos) {
//            ActivityIterationInformationType matchingInfo =
//                    findOrCreateMatchingInfo(sumInfos, deltaInfo.getIdentifier(), true);
//            addPartInformation(matchingInfo, deltaInfo);
//        }
//    }

    /** Adds two "part information" */
    private static void addPartInformation(@NotNull ActivityItemProcessingStatisticsType sum,
            @NotNull ActivityItemProcessingStatisticsType delta) {
        addProcessed(sum.getProcessed(), delta.getProcessed());
        addCurrent(sum.getCurrent(), delta.getCurrent());
        addExecutionRecords(sum, delta);
    }

    private static void addExecutionRecords(@NotNull ActivityItemProcessingStatisticsType sum,
            @NotNull ActivityItemProcessingStatisticsType delta) {
        List<ActivityExecutionRecordType> nonOverlappingRecords =
                new WallClockTimeComputer(sum.getExecution(), delta.getExecution())
                        .getNonOverlappingRecords();
        sum.getExecution().clear();
        nonOverlappingRecords.sort(Comparator.comparing(r -> XmlTypeConverter.toMillis(r.getStartTimestamp())));
        sum.getExecution().addAll(CloneUtil.cloneCollectionMembersWithoutIds(nonOverlappingRecords));
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

    @Deprecated
    public List<String> getLastFailures() {
        return List.of();
    }

    public static String format(ActivityItemProcessingStatisticsType source) {
        return format(source, null);
    }

    // TODO reconsider
    public static String format(List<ActivityItemProcessingStatisticsType> sources) {
        StringBuilder sb = new StringBuilder();
        for (ActivityItemProcessingStatisticsType source : sources) {
            sb.append(format(source));
        }
        return sb.toString();
    }

    /** Formats the information. */
    public static String format(ActivityItemProcessingStatisticsType source, AbstractStatisticsPrinter.Options options) {
        ActivityItemProcessingStatisticsType information = source != null ? source : new ActivityItemProcessingStatisticsType();
        return new IterationInformationPrinter(information, options).print();
    }

    public boolean isCollectExecutions() {
        return false;
    }

    /**
     * Operation being recorded: represents an object to which the client reports the end of the operation.
     * It is called simply {@link Operation} to avoid confusing the clients.
     */
    public interface Operation {

        default void succeeded() {
            done(ItemProcessingOutcomeType.SUCCESS, null);
        }

        default void skipped() {
            done(ItemProcessingOutcomeType.SKIP, null);
        }

        default void failed(Throwable t) {
            done(ItemProcessingOutcomeType.FAILURE, t);
        }

        default void done(ItemProcessingOutcomeType outcome, Throwable exception) {
        }

        default void done(QualifiedItemProcessingOutcomeType outcome, Throwable exception) {
            // no-op
        }

        double getDurationRounded();

        long getEndTimeMillis();
    }
}
