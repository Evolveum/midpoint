/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;

import javax.xml.namespace.QName;

/**
 * @author Pavol Mederly
 */
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

    };

    private final Record stateBefore = new Record();
    private final Record stateAfter = new Record();

    public SynchronizationInformation(SynchronizationInformationType value) {
        startValue = value;
    }

    public SynchronizationInformation() {
        this(null);
    }

    public SynchronizationInformationType getStartValue() {
        return (SynchronizationInformationType) startValue;
    }

    public synchronized SynchronizationInformationType getDeltaValue() {
        SynchronizationInformationType rv = toSynchronizationInformationType();
        return rv;
    }

    public synchronized SynchronizationInformationType getAggregatedValue() {
        SynchronizationInformationType delta = toSynchronizationInformationType();
        SynchronizationInformationType rv = aggregate(startValue, delta);
        return rv;
    }

    private SynchronizationInformationType aggregate(SynchronizationInformationType startValue, SynchronizationInformationType delta) {
        if (startValue == null) {
            return delta;
        }
        SynchronizationInformationType rv = new SynchronizationInformationType();
        addTo(rv, startValue);
        addTo(rv, delta);
        return rv;
    }

    public static void addTo(SynchronizationInformationType sum, SynchronizationInformationType delta) {
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

	protected void addToState(Record state, Record increment) {
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

	public void recordSynchronizationOperationStart(String objectName, String objectDisplayName, QName objectType, String objectOid) {
        // noop
    }

}
