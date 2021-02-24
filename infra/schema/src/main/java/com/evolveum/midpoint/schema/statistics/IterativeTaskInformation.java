/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static com.evolveum.midpoint.util.MiscUtil.or0;

/**
 * This is "live" iterative task information.
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
            addToKeepingParts(this.value, value);
        }
    }

    public synchronized IterativeTaskInformationType getValueCopy() {
        return value.clone();
    }

    public synchronized Operation recordOperationStart(IterativeOperation operation) {
        IterationItemInformation item = operation.getItem();
        ProcessedItemType processedItem = new ProcessedItemType(prismContext)
                .name(item.getObjectName())
                .displayName(item.getObjectDisplayName())
                .type(item.getObjectType())
                .oid(item.getObjectOid())
                .startTimestamp(XmlTypeConverter.createXMLGregorianCalendar(operation.getStartTimestamp()))
                .operationId(getNextOperationId());

        IterativeTaskPartItemsProcessingInformationType matchingPart =
                findOrCreateMatchingPart(value.getPart(), operation.getTaskPartNumber());
        List<ProcessedItemType> currentList = matchingPart.getCurrent();

        currentList.add(processedItem);
        LOGGER.trace("Recorded current operation. Current list size: {}. Operation: {}", currentList.size(), operation);
        return new OperationImpl(operation, processedItem);
    }

    /** Must be synchronized because it is called externally (through Operation interface) */
    private synchronized void recordOperationEnd(IterativeOperation operation, long operationId,
            ProcessedItemType processedItem, QualifiedItemProcessingOutcomeType outcome, Throwable exception) {
        Optional<IterativeTaskPartItemsProcessingInformationType> matchingPartOptional =
                findMatchingPart(value.getPart(), operation.getTaskPartNumber());
        if (matchingPartOptional.isPresent()) {
            recordToPart(matchingPartOptional.get(), operation, operationId, processedItem, outcome, exception);
        } else {
            LOGGER.warn("Couldn't record operation end. Task part {} was not found for {}",
                    operation.getTaskPartNumber(), operation);
        }
    }

    private void recordToPart(IterativeTaskPartItemsProcessingInformationType part, IterativeOperation operation,
            long operationId, ProcessedItemType processedItem, QualifiedItemProcessingOutcomeType outcome, Throwable exception) {
        removeFromCurrentOperations(part, operationId);
        addToProcessedItemSet(part, operation, processedItem, outcome, exception);
    }

    private void addToProcessedItemSet(IterativeTaskPartItemsProcessingInformationType part, IterativeOperation operation,
            ProcessedItemType processedItem, QualifiedItemProcessingOutcomeType outcome, Throwable exception) {
        ProcessedItemSetType itemSet = findOrCreateProcessedItemSet(part, outcome);
        itemSet.setCount(or0(itemSet.getCount()) + 1);
        long endTimestamp = System.currentTimeMillis();
        long duration = endTimestamp - operation.getStartTimestamp();
        itemSet.setDuration(or0(itemSet.getDuration()) + duration);
        ProcessedItemType processedItemClone = processedItem.clone(); // to remove the parent
        processedItemClone.setEndTimestamp(XmlTypeConverter.createXMLGregorianCalendar(endTimestamp));
        itemSet.setLastItem(processedItemClone);
    }

    private ProcessedItemSetType findOrCreateProcessedItemSet(IterativeTaskPartItemsProcessingInformationType part,
            QualifiedItemProcessingOutcomeType outcome) {
        return part.getProcessed().stream()
                .filter(itemSet -> Objects.equals(itemSet.getOutcome(), outcome))
                .findFirst()
                .orElseGet(() -> new ProcessedItemSetType(prismContext).outcome(outcome.clone()));
    }

    private void removeFromCurrentOperations(IterativeTaskPartItemsProcessingInformationType part, long operationId) {
        boolean removed = part.getCurrent().removeIf(item -> Objects.equals(operationId, item.getOperationId()));
        if (!removed) {
            LOGGER.warn("Couldn't remove operation {} from the list of current operations: {}", operationId, part.getCurrent());
        }
    }

    private static long getNextOperationId() {
        return ID_COUNTER.getAndIncrement();
    }

    /**
     * Does the following:
     *
     * - sum.parts += delta.parts (matching)
     * - sum.summary += delta.summary (just for sure; setting summary only if present in delta)
     */
    public static void addToKeepingParts(@NotNull IterativeTaskInformationType sum, @NotNull IterativeTaskInformationType delta) {
        addMatchingParts(sum.getPart(), delta.getPart());
        addToSummary(sum, delta.getSummary());
    }

    /**
     * Does the following:
     *
     * - sum.summary += delta.parts
     * - sum.summary += delta.summary
     *
     * Typically because the part numbering is not compatible among sum/deltas.
     * */
    public static void addToMergingParts(@NotNull IterativeTaskInformationType sum,
            @NotNull IterativeTaskInformationType delta) {
        delta.getPart().forEach(part -> addToSummary(sum, part));
        addToSummary(sum, delta.getSummary());
    }

    private static void addToSummary(@NotNull IterativeTaskInformationType sum, IterativeItemsProcessingInformationType deltaInfo) {
        if (deltaInfo == null) {
            return;
        }
        if (sum.getSummary() == null) {
            sum.setSummary(new IterativeItemsProcessingInformationType());
        }
        addInternal(sum.getSummary(), deltaInfo);
    }

    private static void addMatchingParts(List<IterativeTaskPartItemsProcessingInformationType> sumParts,
            List<IterativeTaskPartItemsProcessingInformationType> deltaParts) {
        for (IterativeTaskPartItemsProcessingInformationType deltaPart : deltaParts) {
            IterativeTaskPartItemsProcessingInformationType matchingPart =
                    findOrCreateMatchingPart(sumParts, deltaPart.getPartNumber());
            addInternal(matchingPart, deltaPart);
        }
    }

    private static IterativeTaskPartItemsProcessingInformationType findOrCreateMatchingPart(
            @NotNull List<IterativeTaskPartItemsProcessingInformationType> list, Integer partNumber) {
        return findMatchingPart(list, partNumber)
                .orElseGet(() -> new IterativeTaskPartItemsProcessingInformationType().partNumber(partNumber));
    }

    private static Optional<IterativeTaskPartItemsProcessingInformationType> findMatchingPart(
            @NotNull List<IterativeTaskPartItemsProcessingInformationType> list, Integer partNumber) {
        return list.stream()
                .filter(item -> Objects.equals(item.getPartNumber(), partNumber))
                .findFirst();
    }

    private static void addInternal(@NotNull IterativeItemsProcessingInformationType sum,
            @NotNull IterativeItemsProcessingInformationType delta) {
        addProcessed(sum.getProcessed(), delta.getProcessed());
        addCurrent(sum.getCurrent(), delta.getCurrent());
    }

    private static void addProcessed(@NotNull List<ProcessedItemSetType> sumSets, @NotNull List<ProcessedItemSetType> deltaSets) {
        for (ProcessedItemSetType deltaSet : deltaSets) {
            ProcessedItemSetType matchingSet =
                    findOrCreateMatchingSet(sumSets, deltaSet.getOutcome());
            addCounters(matchingSet, deltaSet);
        }
    }

    private static ProcessedItemSetType findOrCreateMatchingSet(
            @NotNull List<ProcessedItemSetType> list, QualifiedItemProcessingOutcomeType outcome) {
        return list.stream()
                .filter(item -> Objects.equals(item.getOutcome(), outcome))
                .findFirst()
                .orElseGet(() -> new ProcessedItemSetType().outcome(outcome));
    }

    private static void addCurrent(List<ProcessedItemType> sum, List<ProcessedItemType> delta) {
        sum.addAll(delta);
    }

    private static void addCounters(@NotNull ProcessedItemSetType sum, @NotNull ProcessedItemSetType delta) {
        sum.setCount(or0(sum.getCount()) + or0(delta.getCount()));
        sum.setDuration(or0(sum.getDuration()) + or0(delta.getDuration()));
        if (delta.getLastItem() != null) {
            if (sum.getLastItem() == null ||
                    XmlTypeConverter.isAfterNullLast(delta.getLastItem().getEndTimestamp(), sum.getLastItem().getEndTimestamp())) {
                sum.setLastItem(delta.getLastItem());
            }
        }
    }

    @Deprecated
    public List<String> getLastFailures() {
        //noinspection unchecked
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

    public static String format(IterativeTaskInformationType source, AbstractStatisticsPrinter.Options options) {
        IterativeTaskInformationType information = source != null ? source : new IterativeTaskInformationType();
        return new IterativeTaskInformationPrinter(information, null).print();
    }

    public class OperationImpl implements Operation {

        private final long operationId;
        @NotNull private final IterativeOperation operation;
        @NotNull private final ProcessedItemType processedItem;

        public OperationImpl(@NotNull IterativeOperation operation, @NotNull ProcessedItemType processedItem) {
            this.operationId = processedItem.getOperationId();
            this.operation = operation;
            this.processedItem = processedItem;
        }

        public void done(ItemProcessingOutcomeType outcome, Throwable exception) {
            QualifiedItemProcessingOutcomeType qualifiedOutcome =
                    new QualifiedItemProcessingOutcomeType(prismContext)
                            .outcome(outcome);
            recordOperationEnd(operation, operationId, processedItem, qualifiedOutcome, exception);
        }
    }

    private static class NoneOperation implements Operation {
        public static final Operation INSTANCE = new NoneOperation();

        @Override
        public void done(ItemProcessingOutcomeType outcome, Throwable exception) {
            // no op
        }
    }

    public interface Operation {

        void done(ItemProcessingOutcomeType outcome, Throwable exception);

        default void succeeded() {
            done(ItemProcessingOutcomeType.SUCCESS, null);
        }

        default void skipped() {
            done(ItemProcessingOutcomeType.SKIP, null);
        }

        default void failed(Throwable t) {
            done(ItemProcessingOutcomeType.FAILURE, null);
        }

        static Operation none() {
            return NoneOperation.INSTANCE;
        }
    }
}
