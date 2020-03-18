/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public class SynchronizationInformation {

    /*
     * Thread safety: Just like EnvironmentalPerformanceInformation, instances of this class may be accessed from
     * more than one thread at once. Updates are invoked in the context of the thread executing the task.
     * Queries are invoked either from this thread, or from some observer (task manager or GUI thread).
     */

    private final SynchronizationInformationType startValue;

    // Record is part of the interface, simplifying it a bit
    // It does *not* have to be thread-safe
    public static class Record {

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

        public static Record createProtected() {
            Record record = new Record();
            record.setCountProtected(1);
            return record;
        }

        public static Record createNotApplicable() {
            Record record = new Record();
            record.setCountNotApplicableForTask(1);
            return record;
        }
    }

    private final Record stateBefore = new Record();
    private final Record stateAfter = new Record();

    public SynchronizationInformation(SynchronizationInformationType value) {
        startValue = value;
    }

    public SynchronizationInformation() {
        this(null);
    }

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
        sum.setCountProtected(sum.getCountProtected() + delta.getCountProtected());
        sum.setCountNoSynchronizationPolicy(sum.getCountNoSynchronizationPolicy() + delta.getCountNoSynchronizationPolicy());
        sum.setCountSynchronizationDisabled(sum.getCountSynchronizationDisabled() + delta.getCountSynchronizationDisabled());
        sum.setCountNotApplicableForTask(sum.getCountNotApplicableForTask() + delta.getCountNotApplicableForTask());
        sum.setCountDeleted(sum.getCountDeleted() + delta.getCountDeleted());
        sum.setCountDisputed(sum.getCountDisputed() + delta.getCountDisputed());
        sum.setCountLinked(sum.getCountLinked() + delta.getCountLinked());
        sum.setCountUnlinked(sum.getCountUnlinked() + delta.getCountUnlinked());
        sum.setCountUnmatched(sum.getCountUnmatched() + delta.getCountUnmatched());

        sum.setCountProtectedAfter(sum.getCountProtectedAfter() + delta.getCountProtectedAfter());
        sum.setCountNoSynchronizationPolicyAfter(sum.getCountNoSynchronizationPolicyAfter() + delta.getCountNoSynchronizationPolicyAfter());
        sum.setCountSynchronizationDisabledAfter(sum.getCountSynchronizationDisabledAfter() + delta.getCountSynchronizationDisabledAfter());
        sum.setCountNotApplicableForTaskAfter(sum.getCountNotApplicableForTaskAfter() + delta.getCountNotApplicableForTaskAfter());
        sum.setCountDeletedAfter(sum.getCountDeletedAfter() + delta.getCountDeletedAfter());
        sum.setCountDisputedAfter(sum.getCountDisputedAfter() + delta.getCountDisputedAfter());
        sum.setCountLinkedAfter(sum.getCountLinkedAfter() + delta.getCountLinkedAfter());
        sum.setCountUnlinkedAfter(sum.getCountUnlinkedAfter() + delta.getCountUnlinkedAfter());
        sum.setCountUnmatchedAfter(sum.getCountUnmatchedAfter() + delta.getCountUnmatchedAfter());
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
    }

    public synchronized void recordSynchronizationOperationEnd(String objectName, String objectDisplayName, QName objectType, String objectOid,
            long started, Throwable exception, Record originalStateIncrement, Record newStateIncrement) {
        addToState(stateBefore, originalStateIncrement);
        addToState(stateAfter, newStateIncrement);
    }

    private void addToState(Record state, Record increment) {
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
        StringBuilder sb = new StringBuilder();
        appendHeader(sb);
        SynchronizationInformationType i = source != null ? source : new SynchronizationInformationType();
        append(sb, "Unmatched", i.getCountUnmatched(), i.getCountUnmatchedAfter());
        append(sb, "Unlinked", i.getCountUnlinked(), i.getCountUnlinkedAfter());
        append(sb, "Linked", i.getCountLinked(), i.getCountLinkedAfter());
        append(sb, "Deleted", i.getCountDeleted(), i.getCountDeletedAfter());
        append(sb, "Disputed", i.getCountDisputed(), i.getCountDisputedAfter());
        append(sb, "Protected", i.getCountProtected(), i.getCountProtectedAfter());
        append(sb, "No sync policy", i.getCountNoSynchronizationPolicy(), i.getCountNoSynchronizationPolicyAfter());
        append(sb, "Sync disabled", i.getCountSynchronizationDisabled(), i.getCountSynchronizationDisabledAfter());
        append(sb, "Not applicable", i.getCountNotApplicableForTask(), i.getCountNotApplicableForTaskAfter());
        return sb.toString();
    }

    private static void appendHeader(StringBuilder sb) {
        sb.append(String.format("%20s%10s%10s%10s\n", "Kind", "Before", "After", "Delta"));
        sb.append(StringUtils.repeat('-', 60)).append("\n");
    }

    private static void append(StringBuilder sb, String label, int before, int after) {
        sb.append(String.format("%20s%10d%10d%10d\n", label, before, after, after-before));
    }

    private static float div(long duration, int count) {
        return count != 0 ? (float) duration / count : 0;
    }
}
