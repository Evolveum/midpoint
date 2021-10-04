/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.state;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.schema.util.task.ActivityItemProcessingStatisticsUtil;

import org.apache.commons.collections.buffer.CircularFifoBuffer; // TODO FIXME
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter;
import com.evolveum.midpoint.schema.statistics.IterationInformationPrinter;
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

    private static final AtomicLong ID_COUNTER = new AtomicLong(0);

    @Deprecated
    public static final int LAST_FAILURES_KEPT = 30;

    /** Current value. Guarded by this. */
    @NotNull private final ActivityItemProcessingStatisticsType value;

    @Deprecated
    protected CircularFifoBuffer lastFailures = new CircularFifoBuffer(LAST_FAILURES_KEPT);

    @NotNull private final PrismContext prismContext;

    /**
     * For activities that aggregate operation statistics over significant periods of time (i.e. those that
     * do not start from zero) we would like to avoid execution records to grow indefinitely.
     * As the easiest solution (for 4.3) is to simply stop collecting this information for such tasks.
     */
    private final boolean collectExecutions;

    ActivityItemProcessingStatistics(@NotNull CurrentActivityState<?> activityState) {
        this.prismContext = activityState.getBeans().prismContext;
        this.value = new ActivityItemProcessingStatisticsType(prismContext);
        this.collectExecutions = true; // TODO
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
        ProcessedItemType processedItem = new ProcessedItemType(prismContext)
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

    public synchronized void recordExecutionStart(long startTimestamp) {
        assertInitialized();
        findOrCreateMatchingExecutionRecord(value.getExecution(), startTimestamp);
    }

    public synchronized void recordExecutionEnd(long startTimestamp, long endTimestamp) {
        assertInitialized();
        findOrCreateMatchingExecutionRecord(value.getExecution(), startTimestamp)
                .setEndTimestamp(XmlTypeConverter.createXMLGregorianCalendar(endTimestamp));
    }

    private static ActivityExecutionRecordType findOrCreateMatchingExecutionRecord(List<ActivityExecutionRecordType> records,
            long partStartTimestamp) {
        XMLGregorianCalendar startAsGregorian = XmlTypeConverter.createXMLGregorianCalendar(partStartTimestamp);
        for (ActivityExecutionRecordType record : records) {
            if (startAsGregorian.equals(record.getStartTimestamp())) {
                return record;
            }
        }
        ActivityExecutionRecordType newRecord = new ActivityExecutionRecordType(PrismContext.get())
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
                        () -> ActivityItemProcessingStatisticsUtil.add(part.getProcessed(), new ProcessedItemSetType(prismContext).outcome(outcome.cloneWithoutId())));
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Deprecated
    public List<String> getLastFailures() {
        return new ArrayList<>(lastFailures);
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
        return collectExecutions;
    }

    public int getItemsProcessed() {
        return ActivityItemProcessingStatisticsUtil.getItemsProcessed(getValueCopy());
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
            QualifiedItemProcessingOutcomeType qualifiedOutcome =
                    new QualifiedItemProcessingOutcomeType(PrismContext.get())
                            .outcome(outcome);
            done(qualifiedOutcome, exception);
        }

        void done(QualifiedItemProcessingOutcomeType outcome, Throwable exception);

        double getDurationRounded();

        long getEndTimeMillis();

        /** Returns the item characterization for this operation. */
        @NotNull IterationItemInformation getIterationItemInformation();

        /** Returns start info for this operation. */
        @NotNull IterativeOperationStartInfo getStartInfo();
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
//            System.out.println("DONE: " + startInfo);
            setEndTimes();
            recordOperationEnd(this, outcome, exception);
//            StructuredProgressCollector progressCollector = startInfo.getStructuredProgressCollector();
//            if (progressCollector != null) {
//                ActivityPath activityPath = startInfo.getActivityPath();
//                progressCollector.incrementStructuredProgress("TODO", outcome); // TODO
//            }
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
}
