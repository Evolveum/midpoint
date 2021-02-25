/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.SyncSituationUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds aggregated synchronization information, typically for a given task.
 *
 * Thread safety: Just like EnvironmentalPerformanceInformation, instances of this class may be accessed from
 * more than one thread at once. Updates are invoked in the context of the thread executing the task.
 * Queries are invoked either from this thread, or from some observer (task manager or GUI thread).
 *
 * TODO
 */
public class SynchronizationInformation {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationInformation.class);

    /**
     * Number of transitions between states. This is the new way of counting. Synchronized on this.
     */
    @NotNull private final SynchronizationInformationType value;

    /**
     * Watches for (collects) computed synchronization situation for the shadow being processed.
     */
    @NotNull private final SituationTransitionCollector situationTransitionCollector = new SituationTransitionCollector();

    @NotNull private final PrismContext prismContext;

    public SynchronizationInformation(SynchronizationInformationType value, @NotNull PrismContext prismContext) {
        this.value = new SynchronizationInformationType(prismContext);
        this.prismContext = prismContext;
        addTo(this.value, value);
    }

    /**
     * @return Start value plus counters gathered during existence of this object (i.e. this task run).
     */
    public synchronized SynchronizationInformationType getValueCopy() {
        return value.clone();
    }

    public synchronized void onItemProcessingStart(@NotNull String processingIdentifier,
            @Nullable SynchronizationSituationType beforeOperation) {
        situationTransitionCollector.onItemProcessingStart(processingIdentifier, beforeOperation);
    }

    public synchronized void onSynchronizationStart(@Nullable String processingIdentifier, @Nullable String shadowOid,
            @Nullable SynchronizationSituationType situation) {
        situationTransitionCollector.onSynchronizationStart(processingIdentifier, shadowOid, situation);
    }

    public synchronized void onSynchronizationExclusion(@Nullable String processingIdentifier,
            @NotNull SynchronizationExclusionReasonType exclusionReason) {
        situationTransitionCollector.onSynchronizationExclusion(processingIdentifier, exclusionReason);
    }

    public synchronized void onSynchronizationSituationChange(@Nullable String processingIdentifier,
            @Nullable String shadowOid, @Nullable SynchronizationSituationType situation) {
        situationTransitionCollector.onSynchronizationSituationChange(processingIdentifier, shadowOid, situation);
    }

    public synchronized void onSyncItemProcessingEnd(@NotNull String processingIdentifier, @NotNull QualifiedItemProcessingOutcomeType outcome) {
        if (situationTransitionCollector.identifierMatches(processingIdentifier)) {
            record(situationTransitionCollector.onProcessingStart, situationTransitionCollector.onSynchronizationStart,
                    situationTransitionCollector.onSynchronizationEnd, situationTransitionCollector.exclusionReason, outcome);
        } else {
            LOGGER.warn("Couldn't record synchronization situation: processing identifier changed from {} to {}",
                    situationTransitionCollector.processingIdentifier, processingIdentifier);
        }
        situationTransitionCollector.reset();
    }

    private void record(SynchronizationSituationType onProcessingStart, SynchronizationSituationType onSynchronizationStart,
            SynchronizationSituationType onSynchronizationEnd, SynchronizationExclusionReasonType exclusionReason,
            @NotNull QualifiedItemProcessingOutcomeType outcome) {

        // Poor man's solution: We create artificial delta and reuse the summarization code.
        SynchronizationInformationType delta = new SynchronizationInformationType(prismContext)
                .beginTransition()
                    .onProcessingStart(onProcessingStart)
                    .onSynchronizationStart(onSynchronizationStart)
                    .onSynchronizationEnd(onSynchronizationEnd)
                    .exclusionReason(exclusionReason)
                    .beginCounter()
                        .outcome(outcome.clone())
                        .count(1)
                    .<SynchronizationSituationTransitionType>end()
                .end();

        addTo(this.value, delta);
    }

    public static void addTo(SynchronizationInformationType sum, @Nullable SynchronizationInformationType delta) {
        if (delta == null) {
            return;
        }
        addTransitions(sum, delta);
    }

    private static void addTransitions(SynchronizationInformationType sum, @NotNull SynchronizationInformationType delta) {
        for (SynchronizationSituationTransitionType deltaTransition : delta.getTransition()) {
            SynchronizationSituationTransitionType existingTransition = SyncSituationUtil.findMatchingTransition(sum,
                    deltaTransition.getOnProcessingStart(), deltaTransition.getOnSynchronizationStart(),
                    deltaTransition.getOnSynchronizationEnd(), deltaTransition.getExclusionReason());
            if (existingTransition != null) {
                addTo(existingTransition, deltaTransition);
            } else {
                sum.getTransition().add(deltaTransition.clone());
            }
        }
    }

    private static void addTo(@NotNull SynchronizationSituationTransitionType sum,
            @NotNull SynchronizationSituationTransitionType delta) {
        OutcomeKeyedCounterTypeUtil.addCounters(sum.getCounter(), delta.getCounter());
    }

    public static String format(@Nullable SynchronizationInformationType source) {
        return new SynchronizationInformationPrinter(source != null ? source : new SynchronizationInformationType(), null)
                .print();
    }

    /**
     * Watches for computed synchronization situation for a given shadow.
     */
    private static class SituationTransitionCollector {

        /**
         * Item processing for which we collect the synchronization situation changes.
         *
         * It is set at the beginning of item processing in the task.
         * We assume we are in the _worker_ task i.e. that this information will not be overwritten
         * by another processing [of other item] (only after it is collected and recorded).
         *
         * If null, no collecting is in place.
         */
        private String processingIdentifier;

        /**
         * OID of the shadow that we use to filter out situation update messages.
         */
        private String shadowOid;

        /**
         * Situation of the shadow at the beginning of item processing.
         */
        private SynchronizationSituationType onProcessingStart;

        /**
         * Situation at the beginning of the synchronization operation - i.e. after initial situation determination.
         */
        private SynchronizationSituationType onSynchronizationStart;

        private SynchronizationExclusionReasonType exclusionReason;

        /**
         * Situation at the end of the synchronization operation - i.e. either after the clockwork was executed, or just after
         * synchronization service finished.
         */
        private SynchronizationSituationType onSynchronizationEnd;

        private void onItemProcessingStart(@NotNull String processingIdentifier,
                @Nullable SynchronizationSituationType beforeOperation) {
            reset();
            this.processingIdentifier = processingIdentifier;
            this.onProcessingStart = beforeOperation;
        }

        private void onSynchronizationStart(@Nullable String processingIdentifier, @Nullable String shadowOid,
                @Nullable SynchronizationSituationType situation) {
            if (identifierMatches(processingIdentifier)) {
                this.onSynchronizationStart = situation;
                this.onSynchronizationEnd = situation; // This is an assumption. Valid until situation changes.
                this.shadowOid = shadowOid;
            }
        }

        private void onSynchronizationExclusion(@Nullable String processingIdentifier,
                @NotNull SynchronizationExclusionReasonType exclusionReason) {
            if (identifierMatches(processingIdentifier)) {
                this.exclusionReason = exclusionReason;
            }
        }

        private void onSynchronizationSituationChange(@Nullable String processingIdentifier,
                @Nullable String shadowOid, @Nullable SynchronizationSituationType situation) {
            if (identifierMatches(processingIdentifier) && shadowMatches(shadowOid)) {
                this.onSynchronizationEnd = situation;
            }
        }

        private boolean identifierMatches(String processingIdentifier) {
            return processingIdentifier != null && processingIdentifier.equals(this.processingIdentifier);
        }

        private boolean shadowMatches(String shadowOid) {
            return shadowOid != null && shadowOid.equals(this.shadowOid);
        }

        public void reset() {
            processingIdentifier = null;
            shadowOid = null;
            onProcessingStart = null;
            onSynchronizationStart = null;
            onSynchronizationEnd = null;
            exclusionReason = null;
        }
    }
}
