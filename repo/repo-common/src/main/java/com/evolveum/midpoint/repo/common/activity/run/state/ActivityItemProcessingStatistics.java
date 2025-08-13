/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.Operation;
import com.evolveum.midpoint.schema.util.task.ActivityItemProcessingStatisticsUtil;

import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.logging.LoggingUtils;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.schema.statistics.IterativeOperationStartInfo;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * This is "live" iteration information.
 *
 * BEWARE: When explicitly enabled, automatically updates also the structured progress when recording operation end.
 * This is somewhat experimental and should be reconsidered.
 *
 * Thread safety: Must be thread safe.
 *
 * 1. Updates are invoked in the context of the thread executing the activity.
 * 2. But queries are invoked either from this thread, or from some observer (task manager or GUI thread).
 *
 * Implementation: Because the iteration information grew to quite complex structure,
 * we no longer keep "native" form and "bean" form separately. Now we simply store the bean
 * form, and provide the necessary synchronization.
 *
 * Also, we no longer distinguish start value and delta. Everything is kept in the {@link #value}.
 */
public class ActivityItemProcessingStatistics extends Initializable {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityItemProcessingStatistics.class);

    @Experimental
    private static final String OP_UPDATE_STATISTICS_FOR_SIMPLE_CLIENT = ActivityItemProcessingStatistics.class.getName() +
            ".updateStatisticsForSimpleClient";

    private static final AtomicLong ID_COUNTER = new AtomicLong(0);

    /** Current value. Guarded by this. */
    @NotNull private final ActivityItemProcessingStatisticsType value;

    /**
     * Reference to the containing activity state object.
     */
    @NotNull private final CurrentActivityState<?> activityState;

    /**
     * When we last updated activity statistics for the simple clients.
     * EXPERIMENTAL. UGLY HACK. PLEASE REMOVE.
     */
    @Experimental
    private long lastStatisticsUpdatedForSimpleClients = System.currentTimeMillis();

    @Experimental
    private static final long STATISTICS_UPDATE_INTERVAL = 3000L;

    ActivityItemProcessingStatistics(@NotNull CurrentActivityState<?> activityState) {
        this.activityState = activityState;
        this.value = new ActivityItemProcessingStatisticsType();
    }

    /**
     * For activities that aggregate operation statistics over significant periods of time (i.e. those that
     * do not start from zero) we would like to avoid run records to grow indefinitely.
     * As the easiest solution (for 4.3) is to simply stop collecting this information for such tasks.
     */
    private boolean areRunRecordsSupported() {
        return activityState.areRunRecordsSupported();
    }

    public void initialize(ActivityItemProcessingStatisticsType initialValue) {
        doInitialize(() -> {
            if (initialValue != null) {
                ActivityItemProcessingStatisticsUtil.addTo(this.value, initialValue);
            }
        });
    }

    /** Returns a current value of this statistics. It is copied because of thread safety issues. */
    public synchronized @NotNull ActivityItemProcessingStatisticsType getValueCopy() {
        assertInitialized();
        return value.cloneWithoutId();
    }

    /**
     * Records an operation that has been just started. Stores it into the list of current operations.
     * Returns an object that should receive the status of the operation, in order to record
     * the operation end.
     */
    public synchronized Operation recordOperationStart(IterativeOperationStartInfo startInfo) {
        assertInitialized();
        IterationItemInformation item = startInfo.getItem();
        ProcessedItemType processedItem = new ProcessedItemType()
                .name(item.getObjectName())
                .displayName(item.getObjectDisplayName())
                .type(item.getObjectType())
                .oid(item.getObjectOid())
                .startTimestamp(XmlTypeConverter.createXMLGregorianCalendar(startInfo.getStartTimeMillis()))
                .operationId(getNextOperationId());

        List<ProcessedItemType> currentList = value.getCurrent();
        currentList.add(processedItem);
        LOGGER.trace("Recorded current operation. Current list size: {}. Operation: {}", currentList.size(), startInfo);

        return new OperationImpl(startInfo, processedItem);
    }

    public synchronized void recordRunStart(long startTimestamp) {
        assertInitialized();
        updateMatchingRunRecord(startTimestamp);
    }

    public synchronized void recordRunEnd(long startTimestamp, long endTimestamp) {
        assertInitialized();
        updateMatchingRunRecord(startTimestamp, XmlTypeConverter.createXMLGregorianCalendar(endTimestamp));
    }

    private void updateMatchingRunRecord(long startTimestamp) {
        // We must not create an run record without end timestamp.
        // So, if the run is still going on, we use the current timestamp.
        updateMatchingRunRecord(startTimestamp, XmlTypeConverter.createXMLGregorianCalendar());
    }

    private void updateMatchingRunRecord(long startTimestamp, XMLGregorianCalendar xmlGregorianCalendar) {
        findOrCreateMatchingRunRecord(value.getRun(), startTimestamp)
                .setEndTimestamp(xmlGregorianCalendar);
    }

    private static ActivityRunRecordType findOrCreateMatchingRunRecord(List<ActivityRunRecordType> records,
            long partStartTimestamp) {
        XMLGregorianCalendar startAsGregorian = XmlTypeConverter.createXMLGregorianCalendar(partStartTimestamp);
        for (ActivityRunRecordType record : records) {
            if (startAsGregorian.equals(record.getStartTimestamp())) {
                return record;
            }
        }
        ActivityRunRecordType newRecord = new ActivityRunRecordType()
                .startTimestamp(startAsGregorian);
        records.add(newRecord);
        return newRecord;
    }

    /**
     * Records the operation end. Must be synchronized because it is called externally (through Operation interface).
     */
    private synchronized void recordOperationEnd(OperationImpl operation, QualifiedItemProcessingOutcomeType outcome,
            Throwable exception) {
        removeFromCurrentOperations(value, operation);
        addToProcessedItemSet(value, operation, outcome, exception);
        if (areRunRecordsSupported()) {
            updateMatchingRunRecord(getActivityRunStartTimestamp(), operation.getEndTimestamp());
        }
    }

    private long getActivityRunStartTimestamp() {
        return getActivityRun().getStartTimestampRequired();
    }

    private @NotNull AbstractActivityRun<?, ?, ?> getActivityRun() {
        return activityState.getActivityRun();
    }

    /** Updates the corresponding `processed` statistics. Creates and stores appropriate `lastItem` record. */
    private void addToProcessedItemSet(ActivityItemProcessingStatisticsType part, OperationImpl operation,
            QualifiedItemProcessingOutcomeType outcome, Throwable exception) {

        ProcessedItemSetType itemSet = findOrCreateProcessedItemSet(part, outcome);
        itemSet.setCount(or0(itemSet.getCount()) + 1);

        itemSet.setDuration(or0(itemSet.getDuration()) + operation.getDurationRounded());
        ProcessedItemType processedItemClone = operation.processedItem.cloneWithoutId(); // mainly to remove the parent
        processedItemClone.setEndTimestamp(XmlTypeConverter.createXMLGregorianCalendar(operation.endTimeMillis));
        if (exception != null) {
            processedItemClone.setMessage(exception.getMessage());
        }
        itemSet.setLastItem(processedItemClone);
    }

    /** Finds item set, creating _and adding to the list_ if necessary. */
    private ProcessedItemSetType findOrCreateProcessedItemSet(ActivityItemProcessingStatisticsType part,
            QualifiedItemProcessingOutcomeType outcome) {
        return part.getProcessed().stream()
                .filter(itemSet -> Objects.equals(itemSet.getOutcome(), outcome))
                .findFirst()
                .orElseGet(
                        () -> ActivityItemProcessingStatisticsUtil.add(
                                part.getProcessed(),
                                new ProcessedItemSetType()
                                        .outcome(outcome.cloneWithoutId())));
    }

    /** Removes operation (given by id) from current operations in given task part. */
    private void removeFromCurrentOperations(ActivityItemProcessingStatisticsType part, OperationImpl operation) {
        List<ProcessedItemType> currentOperations = part.getCurrent();
        boolean removed = currentOperations.removeIf(item -> Objects.equals(operation.operationId, item.getOperationId()));
        if (removed) {
            LOGGER.trace("Removed operation {} from the list of current operations. Remaining: {}",
                    operation, currentOperations.size());
        } else {
            LOGGER.warn("Couldn't remove operation {} from the list of current operations: {}",
                    operation, currentOperations);
        }
    }

    private static long getNextOperationId() {
        return ID_COUNTER.getAndIncrement();
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
        return ActivityItemProcessingStatisticsUtil.format(source, options);
    }

    public int getItemsProcessed() {
        return ActivityItemProcessingStatisticsUtil.getItemsProcessed(getValueCopy());
    }

    /**
     * Real implementation of the {@link Operation} interface.
     */
    public class OperationImpl implements Operation {

        /** Generated ID used for finding the operation among current operations. */
        private final long operationId;

        /** Client-supplied operation information. */
        @NotNull private final IterativeOperationStartInfo startInfo;

        /**
         * The processed item structure generated when recording operation start.
         * It is stored in the list of current operations.
         */
        @NotNull private final ProcessedItemType processedItem;

        private long endTimeMillis;
        private long endTimeNanos;

        OperationImpl(@NotNull IterativeOperationStartInfo startInfo, @NotNull ProcessedItemType processedItem) {
            // The processedItem is stored only in memory; so there is no way of having null here.
            this.operationId = requireNonNull(processedItem.getOperationId());
            this.startInfo = startInfo;
            this.processedItem = processedItem;
        }

        @Override
        public void done(QualifiedItemProcessingOutcomeType outcome, Throwable exception) {
            setEndTimes();
            recordOperationEnd(this, outcome, exception);
            if (startInfo.isSimpleCaller()) {
                activityState.getLiveProgress().increment(outcome, ActivityProgress.Counters.COMMITTED);
                updateStatisticsForSimpleClients(false);
            }
        }

        private void setEndTimes() {
            endTimeMillis = System.currentTimeMillis();
            endTimeNanos = System.nanoTime();
        }

        @Override
        public double getDurationRounded() {
            if (endTimeNanos == 0) {
                throw new IllegalStateException("Operation has not finished yet");
            } else {
                double tensOfMicroseconds = Math.round((endTimeNanos - startInfo.getStartTimeNanos()) / 10000.0);
                return tensOfMicroseconds / 100.0;
            }
        }

        @Override
        public long getEndTimeMillis() {
            return endTimeMillis;
        }

        @Override
        public @NotNull IterationItemInformation getIterationItemInformation() {
            return startInfo.getItem();
        }

        @Override
        public @NotNull IterativeOperationStartInfo getStartInfo() {
            return startInfo;
        }
    }

    /**
     * Very ugly hack. We create our own operation result (!!).
     */
    @Experimental
    public void updateStatisticsForSimpleClients(boolean forced) {
        try {
            activityState.updateProgressAndStatisticsNoCommit();
            if (forced || System.currentTimeMillis() > lastStatisticsUpdatedForSimpleClients + STATISTICS_UPDATE_INTERVAL) {
                lastStatisticsUpdatedForSimpleClients = System.currentTimeMillis();
                activityState.flushPendingTaskModificationsChecked(new OperationResult(OP_UPDATE_STATISTICS_FOR_SIMPLE_CLIENT));
            }
        } catch (ActivityRunException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't update statistics for a simple client in {}", e, this);
        }
    }
}
