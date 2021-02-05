/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.schema.util.SyncSituationUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Objects;
import java.util.function.Consumer;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

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
     * Value when the task started. See {@link #getAggregatedValue()}.
     */
    private final SynchronizationInformationType startValue;

    /**
     * Global counters for "before" state. This is the legacy way of counting. Synchronized on this.
     */
    private final LegacyCounters stateBefore = new LegacyCounters();

    /**
     * Global counters for "after" state. This is the legacy way of counting. Synchronized on this.
     */
    private final LegacyCounters stateAfter = new LegacyCounters();

    /**
     * Number of transitions between states. This is the new way of counting. Synchronized on this.
     */
    private final Transitions transitions = new Transitions();

    /**
     * Watches for (collects) computed synchronization situation for the shadow being processed.
     */
    private final SituationTransitionCollector situationTransitionCollector = new SituationTransitionCollector();

    public SynchronizationInformation(SynchronizationInformationType value) {
        startValue = value;
    }

    public SynchronizationInformation() {
        this(null);
    }

    /**
     * @return Start value plus counters gathered during existence of this object (i.e. this task run).
     */
    public synchronized SynchronizationInformationType getAggregatedValue() {
        SynchronizationInformationType delta = toSynchronizationInformationType();
        if (startValue == null) {
            return delta;
        } else {
            SynchronizationInformationType aggregated = new SynchronizationInformationType();
            addTo(aggregated, startValue);
            addTo(aggregated, delta);
            return aggregated;
        }
    }

    public static void addTo(SynchronizationInformationType sum, @Nullable SynchronizationInformationType delta) {
        if (delta == null) {
            return;
        }
        addLegacyBefore(sum, delta);
        addLegacyAfter(sum, delta);
        addTransitions(sum, delta);
    }

    private static void addLegacyBefore(SynchronizationInformationType sum, @NotNull SynchronizationInformationType delta) {
        sum.setCountProtected(add(sum.getCountProtected(), delta.getCountProtected()));
        sum.setCountNoSynchronizationPolicy(add(sum.getCountNoSynchronizationPolicy(), delta.getCountNoSynchronizationPolicy()));
        sum.setCountSynchronizationDisabled(add(sum.getCountSynchronizationDisabled(), delta.getCountSynchronizationDisabled()));
        sum.setCountNotApplicableForTask(add(sum.getCountNotApplicableForTask(), delta.getCountNotApplicableForTask()));
        sum.setCountDeleted(add(sum.getCountDeleted(), delta.getCountDeleted()));
        sum.setCountDisputed(add(sum.getCountDisputed(), delta.getCountDisputed()));
        sum.setCountLinked(add(sum.getCountLinked(), delta.getCountLinked()));
        sum.setCountUnlinked(add(sum.getCountUnlinked(), delta.getCountUnlinked()));
        sum.setCountUnmatched(add(sum.getCountUnmatched(), delta.getCountUnmatched()));
    }

    private static void addLegacyAfter(SynchronizationInformationType sum, @NotNull SynchronizationInformationType delta) {
        sum.setCountProtectedAfter(add(sum.getCountProtectedAfter(), delta.getCountProtectedAfter()));
        sum.setCountNoSynchronizationPolicyAfter(add(sum.getCountNoSynchronizationPolicyAfter(), delta.getCountNoSynchronizationPolicyAfter()));
        sum.setCountSynchronizationDisabledAfter(add(sum.getCountSynchronizationDisabledAfter(), delta.getCountSynchronizationDisabledAfter()));
        sum.setCountNotApplicableForTaskAfter(add(sum.getCountNotApplicableForTaskAfter(), delta.getCountNotApplicableForTaskAfter()));
        sum.setCountDeletedAfter(add(sum.getCountDeletedAfter(), delta.getCountDeletedAfter()));
        sum.setCountDisputedAfter(add(sum.getCountDisputedAfter(), delta.getCountDisputedAfter()));
        sum.setCountLinkedAfter(add(sum.getCountLinkedAfter(), delta.getCountLinkedAfter()));
        sum.setCountUnlinkedAfter(add(sum.getCountUnlinkedAfter(), delta.getCountUnlinkedAfter()));
        sum.setCountUnmatchedAfter(add(sum.getCountUnmatchedAfter(), delta.getCountUnmatchedAfter()));
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
        add(sum::setCountSuccess, sum.getCountSuccess(), delta.getCountSuccess());
        add(sum::setCountError, sum.getCountError(), delta.getCountError());
        add(sum::setCountSkip, sum.getCountSkip(), delta.getCountSkip());
    }

    // Here we intentionally return 0 if adding null values (backward compatibility)
    private static int add(Integer v1, Integer v2) {
        return or0(v1) + or0(v2);
    }

    // But here we intentionally set null if adding null values
    private static void add(Consumer<Integer> setter, Integer value1, Integer value2) {
        if (value1 == null) {
            setter.accept(value2);
        } else if (value2 == null) {
            setter.accept(value1);
        } else {
            setter.accept(value1 + value2);
        }
    }

    private SynchronizationInformationType toSynchronizationInformationType() {
        SynchronizationInformationType rv = new SynchronizationInformationType();
        toJaxb(rv);
        return rv;
    }

    private void toJaxb(SynchronizationInformationType rv) {
        rv.setCountProtected(stateBefore.countProtected);
        rv.setCountNoSynchronizationPolicy(stateBefore.countNoSynchronizationPolicy);
        rv.setCountSynchronizationDisabled(stateBefore.countSynchronizationDisabled);
        rv.setCountNotApplicableForTask(stateBefore.countNotApplicableForTask);
        rv.setCountDeleted(stateBefore.countDeleted);
        rv.setCountDisputed(stateBefore.countDisputed);
        rv.setCountLinked(stateBefore.countLinked);
        rv.setCountUnlinked(stateBefore.countUnlinked);
        rv.setCountUnmatched(stateBefore.countUnmatched);

        rv.setCountProtectedAfter(stateAfter.countProtected);
        rv.setCountNoSynchronizationPolicyAfter(stateAfter.countNoSynchronizationPolicy);
        rv.setCountSynchronizationDisabledAfter(stateAfter.countSynchronizationDisabled);
        rv.setCountNotApplicableForTaskAfter(stateAfter.countNotApplicableForTask);
        rv.setCountDeletedAfter(stateAfter.countDeleted);
        rv.setCountDisputedAfter(stateAfter.countDisputed);
        rv.setCountLinkedAfter(stateAfter.countLinked);
        rv.setCountUnlinkedAfter(stateAfter.countUnlinked);
        rv.setCountUnmatchedAfter(stateAfter.countUnmatched);

        transitions.toJaxb(rv);
    }

    public synchronized void onItemProcessingStart(@NotNull String processingIdentifier,
            @Nullable SynchronizationSituationType beforeOperation) {
        situationTransitionCollector.onItemProcessingStart(processingIdentifier, beforeOperation);
    }

    public void onSynchronizationStart(@Nullable String processingIdentifier, @Nullable String shadowOid,
            @Nullable SynchronizationSituationType situation) {
        situationTransitionCollector.onSynchronizationStart(processingIdentifier, shadowOid, situation);
    }

    public void onSynchronizationExclusion(@Nullable String processingIdentifier,
            @NotNull SynchronizationExclusionReasonType exclusionReason) {
        situationTransitionCollector.onSynchronizationExclusion(processingIdentifier, exclusionReason);
    }

    public synchronized void onSynchronizationSituationChange(@Nullable String processingIdentifier,
            @Nullable String shadowOid, @Nullable SynchronizationSituationType situation) {
        situationTransitionCollector.onSynchronizationSituationChange(processingIdentifier, shadowOid, situation);
    }

    public synchronized void onSyncItemProcessingEnd(@NotNull String processingIdentifier, @NotNull Status status) {
        if (situationTransitionCollector.identifierMatches(processingIdentifier)) {
            transitions.record(situationTransitionCollector.onProcessingStart, situationTransitionCollector.onSynchronizationStart,
                    situationTransitionCollector.onSynchronizationEnd, situationTransitionCollector.exclusionReason, status);
        } else {
            LOGGER.warn("Couldn't record synchronization situation: processing identifier changed from {} to {}",
                    situationTransitionCollector.processingIdentifier, processingIdentifier);
        }

        situationTransitionCollector.reset();
    }

    public synchronized void recordSynchronizationOperationLegacy(LegacyCounters originalStateIncrement, LegacyCounters newStateIncrement) {
        addToState(stateBefore, originalStateIncrement);
        addToState(stateAfter, newStateIncrement);
    }

    private void addToState(LegacyCounters state, LegacyCounters increment) {
        state.countProtected += increment.countProtected;
        state.countNoSynchronizationPolicy += increment.countNoSynchronizationPolicy;
        state.countSynchronizationDisabled += increment.countSynchronizationDisabled;
        state.countNotApplicableForTask += increment.countNotApplicableForTask;
        state.countDeleted += increment.countDeleted;
        state.countDisputed += increment.countDisputed;
        state.countLinked += increment.countLinked;
        state.countUnlinked += increment.countUnlinked;
        state.countUnmatched += increment.countUnmatched;
    }

    public static String format(SynchronizationInformationType source) {
        return formatLegacy(source) + "\n" + formatTransitions(source);
    }

    @NotNull
    private static String formatLegacy(SynchronizationInformationType source) {
        StringBuilder sb = new StringBuilder();
        appendHeaderLegacy(sb);
        SynchronizationInformationType i = source != null ? source : new SynchronizationInformationType();
        appendLineLegacy(sb, "Unmatched", i.getCountUnmatched(), i.getCountUnmatchedAfter());
        appendLineLegacy(sb, "Unlinked", i.getCountUnlinked(), i.getCountUnlinkedAfter());
        appendLineLegacy(sb, "Linked", i.getCountLinked(), i.getCountLinkedAfter());
        appendLineLegacy(sb, "Deleted", i.getCountDeleted(), i.getCountDeletedAfter());
        appendLineLegacy(sb, "Disputed", i.getCountDisputed(), i.getCountDisputedAfter());
        appendLineLegacy(sb, "Protected", i.getCountProtected(), i.getCountProtectedAfter());
        appendLineLegacy(sb, "No sync policy", i.getCountNoSynchronizationPolicy(), i.getCountNoSynchronizationPolicyAfter());
        appendLineLegacy(sb, "Sync disabled", i.getCountSynchronizationDisabled(), i.getCountSynchronizationDisabledAfter());
        appendLineLegacy(sb, "Not applicable", i.getCountNotApplicableForTask(), i.getCountNotApplicableForTaskAfter());
        return sb.toString();
    }

    private static void appendHeaderLegacy(StringBuilder sb) {
        sb.append(String.format("%20s%10s%10s%10s\n", "Kind", "Before", "After", "Delta"));
        sb.append(StringUtils.repeat('-', 60)).append("\n");
    }

    private static void appendLineLegacy(StringBuilder sb, String label, Integer before, Integer after) {
        int b = or0(before);
        int a = or0(after);
        sb.append(String.format("%20s%10d%10d%10d\n", label, b, a, a-b));
    }

    @NotNull
    private static String formatTransitions(SynchronizationInformationType source) {
        StringBuilder sb = new StringBuilder();
        appendHeaderTransitions(sb);
        List<SynchronizationSituationTransitionType> transitions = source != null ?
                new ArrayList<>(source.getTransition()) : new ArrayList<>();
        transitions.sort(createComparator());
        transitions.forEach(t -> appendLineTransitions(sb, t));
        return sb.toString();
    }

    private static Comparator<? super SynchronizationSituationTransitionType> createComparator() {
        return (o1, o2) ->
                ComparisonChain.start()
                        .compare(o1.getOnProcessingStart(), o2.getOnProcessingStart(), Ordering.natural().nullsLast())
                        .compare(o1.getOnSynchronizationStart(), o2.getOnSynchronizationStart(), Ordering.natural().nullsLast())
                        .compare(o1.getOnSynchronizationEnd(), o2.getOnSynchronizationEnd(), Ordering.natural().nullsLast())
                        .compare(o1.getExclusionReason(), o2.getExclusionReason(), Ordering.natural().nullsLast())
                        .result();
    }

    private static void appendHeaderTransitions(StringBuilder sb) {
        sb.append(String.format("%20s%20s%20s%20s%10s%10s%10s\n", "On Start", "On Sync Start", "On Sync End", "Excl. Reason", "Success", "Failure", "Skip"));
        sb.append(StringUtils.repeat('-', 110)).append("\n");
    }

    private static void appendLineTransitions(StringBuilder sb, SynchronizationSituationTransitionType transition) {
        sb.append(String.format("%20s%20s%20s%20s%10d%10d%10d\n",
                name(transition.getOnProcessingStart()), name(transition.getOnSynchronizationStart()),
                name(transition.getOnSynchronizationEnd()), name(transition.getExclusionReason()),
                or0(transition.getCountSuccess()), or0(transition.getCountError()), or0(transition.getCountSkip())));
    }

    private static int or0(Integer value) {
        return defaultIfNull(value, 0);
    }

    // TODO improve
    private static String name(SynchronizationSituationType situation) {
        return situation != null ? situation.value() : "";
    }

    private static String name(SynchronizationExclusionReasonType exclusionReason) {
        return exclusionReason != null ? exclusionReason.value() : "";
    }

    // TODO move to proper place
    public enum Status {
        SUCCESS, ERROR, SKIPPED
    }

    /**
     * Counters that represent:
     *
     * 1. the state (before / after) operation: all values should be 0 or 1
     * 2. the aggregated before / after state: values are arbitrary non-negative numbers
     *
     * This class does not need to be thread-safe. All access is synchronized externally.
     */
    public static class LegacyCounters {

        private int countProtected;
        private int countNoSynchronizationPolicy;
        private int countSynchronizationDisabled;
        private int countNotApplicableForTask;
        private int countDeleted;
        private int countDisputed;
        private int countLinked;
        private int countUnlinked;
        private int countUnmatched;

        public void setCountProtected(int countProtected) {
            this.countProtected = countProtected;
        }

        public void setCountNoSynchronizationPolicy(int countNoSynchronizationPolicy) {
            this.countNoSynchronizationPolicy = countNoSynchronizationPolicy;
        }

        public void setCountSynchronizationDisabled(int countSynchronizationDisabled) {
            this.countSynchronizationDisabled = countSynchronizationDisabled;
        }

        public void setCountNotApplicableForTask(int countNotApplicableForTask) {
            this.countNotApplicableForTask = countNotApplicableForTask;
        }

        public void setCountDeleted(int countDeleted) {
            this.countDeleted = countDeleted;
        }

        public void setCountDisputed(int countDisputed) {
            this.countDisputed = countDisputed;
        }

        public void setCountLinked(int countLinked) {
            this.countLinked = countLinked;
        }

        public void setCountUnlinked(int countUnlinked) {
            this.countUnlinked = countUnlinked;
        }

        public void setCountUnmatched(int countUnmatched) {
            this.countUnmatched = countUnmatched;
        }

        public static LegacyCounters createProtected() {
            LegacyCounters record = new LegacyCounters();
            record.setCountProtected(1);
            return record;
        }

        public static LegacyCounters createNotApplicable() {
            LegacyCounters record = new LegacyCounters();
            record.setCountNotApplicableForTask(1);
            return record;
        }
    }

    private static class Transitions {

        private static class Key {
            private final SynchronizationSituationType onProcessingStart;
            private final SynchronizationSituationType onSynchronizationStart;
            private final SynchronizationSituationType onSynchronizationEnd;
            private final SynchronizationExclusionReasonType exclusionReason;

            private Key(SynchronizationSituationType onProcessingStart, SynchronizationSituationType onSynchronizationStart,
                    SynchronizationSituationType onSynchronizationEnd, SynchronizationExclusionReasonType exclusionReason) {
                this.onProcessingStart = onProcessingStart;
                this.onSynchronizationStart = onSynchronizationStart;
                this.onSynchronizationEnd = onSynchronizationEnd;
                this.exclusionReason = exclusionReason;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o)
                    return true;
                if (!(o instanceof Key))
                    return false;
                Key key = (Key) o;
                return onProcessingStart == key.onProcessingStart && onSynchronizationStart == key.onSynchronizationStart
                        && onSynchronizationEnd == key.onSynchronizationEnd && exclusionReason == key.exclusionReason;
            }

            @Override
            public int hashCode() {
                return Objects.hash(onProcessingStart, onSynchronizationStart, onSynchronizationEnd, exclusionReason);
            }
        }

        private static class Value {
            private int countSuccess;
            private int countError;
            private int countSkip;

            public void add(@NotNull Status status) {
                switch (status) {
                    case SUCCESS:
                        countSuccess++;
                        break;
                    case ERROR:
                        countError++;
                        break;
                    case SKIPPED:
                        countSkip++;
                        break;
                    default:
                        throw new AssertionError(status);
                }
            }
        }

        private final Map<Key, Value> transitions = new HashMap<>();

        private void record(SynchronizationSituationType onProcessingStart,
                SynchronizationSituationType onSynchronizationStart,
                SynchronizationSituationType onSynchronizationEnd,
                SynchronizationExclusionReasonType exclusionReason,
                @NotNull Status status) {
            Key key = new Key(onProcessingStart, onSynchronizationStart, onSynchronizationEnd, exclusionReason);
            transitions.computeIfAbsent(key, k -> new Value())
                    .add(status);
        }

        public void toJaxb(SynchronizationInformationType bean) {
            transitions.forEach((key, value) -> bean.getTransition().add(toJaxb(key, value)));
        }

        private static SynchronizationSituationTransitionType toJaxb(Key key, Value value) {
            SynchronizationSituationTransitionType rv = new SynchronizationSituationTransitionType();
            rv.setOnProcessingStart(key.onProcessingStart);
            rv.setOnSynchronizationStart(key.onSynchronizationStart);
            rv.setOnSynchronizationEnd(key.onSynchronizationEnd);
            rv.setExclusionReason(key.exclusionReason);
            rv.setCountSuccess(value.countSuccess);
            rv.setCountError(value.countError);
            rv.setCountSkip(value.countSkip);
            return rv;
        }
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
