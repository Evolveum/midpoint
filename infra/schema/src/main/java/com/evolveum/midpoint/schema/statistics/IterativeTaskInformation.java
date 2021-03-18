/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import static java.util.Objects.requireNonNull;

/**
 * This is "live" iterative task information.
 *
 * BEWARE: When explicitly enabled, automatically updates also the structured progress when recording operation end.
 * This is somewhat experimental and should be reconsidered.
 *
 * Thread safety: Must be thread safe.
 *
 * 1. Updates are invoked in the context of the thread executing the task.
 * 2. But queries are invoked either from this thread, or from some observer (task manager or GUI thread).
 *
 * Implementation: Because the iterative task information grew to quite complex structure,
 * we no longer keep "native" form and "bean" form separately. Now we simply store the native
 * form, and provide the necessary synchronization.
 *
 * Also, we no longer distinguish start value and delta. Everything is kept in the {@link #value}.
 */
public class IterativeTaskInformation {

    private static final Trace LOGGER = TraceManager.getTrace(IterativeTaskInformation.class);

    private static final AtomicLong ID_COUNTER = new AtomicLong(0);

    @Deprecated
    public static final int LAST_FAILURES_KEPT = 30;

    /** Current value */
    @NotNull private final IterativeTaskInformationType value = new IterativeTaskInformationType();

    @Deprecated
    protected CircularFifoBuffer lastFailures = new CircularFifoBuffer(LAST_FAILURES_KEPT);

    @NotNull
    private final PrismContext prismContext;

    public IterativeTaskInformation(@NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public IterativeTaskInformation(IterativeTaskInformationType value, @NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
        if (value != null) {
            addTo(this.value, value);
        }
    }

    /** Returns a current value of this statistics. It is copied because of thread safety issues. */
    public synchronized IterativeTaskInformationType getValueCopy() {
        return value.clone();
    }

    /**
     * Records an operation that has been just started. Stores it into the list of current operations.
     * Returns an object that should receive the status of the operation, in order to record
     * the operation end.
     */
    public synchronized Operation recordOperationStart(IterativeOperationStartInfo operation) {
        IterationItemInformation item = operation.getItem();
        ProcessedItemType processedItem = new ProcessedItemType(prismContext)
                .name(item.getObjectName())
                .displayName(item.getObjectDisplayName())
                .type(item.getObjectType())
                .oid(item.getObjectOid())
                .startTimestamp(XmlTypeConverter.createXMLGregorianCalendar(operation.getStartTimestamp()))
                .operationId(getNextOperationId());

        IterativeTaskPartItemsProcessingInformationType matchingPart =
                findOrCreateMatchingPart(value.getPart(), operation.getPartUri());
        List<ProcessedItemType> currentList = matchingPart.getCurrent();

        currentList.add(processedItem);
        LOGGER.trace("Recorded current operation. Current list size: {}. Operation: {}", currentList.size(), operation);
        return new OperationImpl(operation, processedItem);
    }

    /**
     * Records the operation end. It is private because it is called externally (through Operation interface).
     * Must be synchronized because of this external access.
     */
    private synchronized void recordOperationEnd(IterativeOperationStartInfo operation, long operationId,
                                                 ProcessedItemType processedItem, QualifiedItemProcessingOutcomeType outcome, Throwable exception) {
        Optional<IterativeTaskPartItemsProcessingInformationType> matchingPartOptional =
                findMatchingPart(value.getPart(), operation.getPartUri());
        if (matchingPartOptional.isPresent()) {
            recordToPart(matchingPartOptional.get(), operation, operationId, processedItem, outcome, exception);
        } else {
            LOGGER.warn("Couldn't record operation end. Task part {} was not found for {}",
                    operation.getPartUri(), operation);
        }
    }

    /** The actual recording of operation end. */
    private void recordToPart(IterativeTaskPartItemsProcessingInformationType part, IterativeOperationStartInfo operation,
            long operationId, ProcessedItemType processedItem, QualifiedItemProcessingOutcomeType outcome, Throwable exception) {
        removeFromCurrentOperations(part, operationId);
        addToProcessedItemSet(part, operation, processedItem, outcome, exception);
    }

    /** Updates the corresponding `processed` statistics. Creates and stores appropriate `lastItem` record. */
    private void addToProcessedItemSet(IterativeTaskPartItemsProcessingInformationType part, IterativeOperationStartInfo operation,
            ProcessedItemType processedItem, QualifiedItemProcessingOutcomeType outcome, Throwable exception) {
        ProcessedItemSetType itemSet = findOrCreateProcessedItemSet(part, outcome);
        itemSet.setCount(or0(itemSet.getCount()) + 1);
        long endTimestamp = System.currentTimeMillis();
        long duration = endTimestamp - operation.getStartTimestamp();
        itemSet.setDuration(or0(itemSet.getDuration()) + duration);
        ProcessedItemType processedItemClone = processedItem.clone(); // to remove the parent
        processedItemClone.setEndTimestamp(XmlTypeConverter.createXMLGregorianCalendar(endTimestamp));
        if (exception != null) {
            processedItemClone.setMessage(exception.getMessage());
        }
        itemSet.setLastItem(processedItemClone);
    }

    /** Finds item set, creating _and adding to the list_ if necessary. */
    private ProcessedItemSetType findOrCreateProcessedItemSet(IterativeTaskPartItemsProcessingInformationType part,
            QualifiedItemProcessingOutcomeType outcome) {
        return part.getProcessed().stream()
                .filter(itemSet -> Objects.equals(itemSet.getOutcome(), outcome))
                .findFirst()
                .orElseGet(
                        () -> add(part.getProcessed(), new ProcessedItemSetType(prismContext).outcome(outcome.clone())));
    }

    /** Like {@link List#add(Object)} but returns the value. */
    private static <T> T add(List<T> list, T value) {
        list.add(value);
        return value;
    }

    /** Removes operation (given by id) from current operations in given task part. */
    private void removeFromCurrentOperations(IterativeTaskPartItemsProcessingInformationType part, long operationId) {
        boolean removed = part.getCurrent().removeIf(item -> Objects.equals(operationId, item.getOperationId()));
        if (!removed) {
            LOGGER.warn("Couldn't remove operation {} from the list of current operations: {}", operationId, part.getCurrent());
        }
    }

    private static long getNextOperationId() {
        return ID_COUNTER.getAndIncrement();
    }

    /** Updates specified summary with given delta. */
    public static void addTo(@NotNull IterativeTaskInformationType sum, @Nullable IterativeTaskInformationType delta) {
        if (delta != null) {
            addMatchingParts(sum.getPart(), delta.getPart());
        }
    }

    /** Looks for matching parts (created if necessary) and adds them. */
    private static void addMatchingParts(List<IterativeTaskPartItemsProcessingInformationType> sumParts,
            List<IterativeTaskPartItemsProcessingInformationType> deltaParts) {
        for (IterativeTaskPartItemsProcessingInformationType deltaPart : deltaParts) {
            IterativeTaskPartItemsProcessingInformationType matchingPart =
                    findOrCreateMatchingPart(sumParts, deltaPart.getPartUri());
            addPartInformation(matchingPart, deltaPart);
        }
    }

    private static IterativeTaskPartItemsProcessingInformationType findOrCreateMatchingPart(
            @NotNull List<IterativeTaskPartItemsProcessingInformationType> list, String partUri) {
        return findMatchingPart(list, partUri)
                .orElseGet(
                        () -> add(list, new IterativeTaskPartItemsProcessingInformationType().partUri(partUri)));
    }

    private static Optional<IterativeTaskPartItemsProcessingInformationType> findMatchingPart(
            @NotNull List<IterativeTaskPartItemsProcessingInformationType> list, String partUri) {
        return list.stream()
                .filter(item -> Objects.equals(item.getPartUri(), partUri))
                .findFirst();
    }

    /** Adds two "part information" */
    private static void addPartInformation(@NotNull IterativeTaskPartItemsProcessingInformationType sum,
            @NotNull IterativeTaskPartItemsProcessingInformationType delta) {
        addProcessed(sum.getProcessed(), delta.getProcessed());
        addCurrent(sum.getCurrent(), delta.getCurrent());
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
        sum.addAll(CloneUtil.cloneCollectionMembers(delta)); // to avoid problems with parent
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Deprecated
    public List<String> getLastFailures() {
        return new ArrayList<>(lastFailures);
    }

    public static String format(IterativeTaskInformationType source) {
        return format(source, null);
    }

    // TODO reconsider
    public static String format(List<IterativeTaskInformationType> sources) {
        StringBuilder sb = new StringBuilder();
        for (IterativeTaskInformationType source : sources) {
            sb.append(format(source));
        }
        return sb.toString();
    }

    /** Formats the information. */
    public static String format(IterativeTaskInformationType source, AbstractStatisticsPrinter.Options options) {
        IterativeTaskInformationType information = source != null ? source : new IterativeTaskInformationType();
        return new IterativeTaskInformationPrinter(information, options).print();
    }

    /**
     * Operation being recorded: represents an object to which the client reports the end of the operation.
     * It is called simply {@link Operation} to avoid confusing the clients.
     */
    public interface Operation {

        void done(ItemProcessingOutcomeType outcome, Throwable exception);

        void done(QualifiedItemProcessingOutcomeType outcome, Throwable exception);

        default void succeeded() {
            done(ItemProcessingOutcomeType.SUCCESS, null);
        }

        default void skipped() {
            done(ItemProcessingOutcomeType.SKIP, null);
        }

        default void failed(Throwable t) {
            done(ItemProcessingOutcomeType.FAILURE, t);
        }

    }

    /**
     * Real implementation of the {@link Operation} interface.
     */
    public class OperationImpl implements Operation {

        /** Generated ID used for finding the operation among current operations. */
        private final long operationId;

        /** Client-supplied operation information. */
        @NotNull private final IterativeOperationStartInfo operation;

        /**
         * The processed item structure generated when recording operation start.
         * It is stored in the list of current operations.
         */
        @NotNull private final ProcessedItemType processedItem;

        OperationImpl(@NotNull IterativeOperationStartInfo operation, @NotNull ProcessedItemType processedItem) {
            // The processedItem is stored only in memory; so there is no way of having null here.
            this.operationId = requireNonNull(processedItem.getOperationId());
            this.operation = operation;
            this.processedItem = processedItem;
        }

        public void done(ItemProcessingOutcomeType simpleOutcome, Throwable exception) {
            QualifiedItemProcessingOutcomeType qualifiedOutcome =
                    new QualifiedItemProcessingOutcomeType(prismContext)
                            .outcome(simpleOutcome);
            done(qualifiedOutcome, exception);
        }

        @Override
        public void done(QualifiedItemProcessingOutcomeType outcome, Throwable exception) {
            recordOperationEnd(operation, operationId, processedItem, outcome, exception);
            StructuredProgressCollector progressCollector = operation.getStructuredProgressCollector();
            if (progressCollector != null) {
                progressCollector.incrementStructuredProgress(operation.getPartUri(), outcome);
            }
        }
    }
}
