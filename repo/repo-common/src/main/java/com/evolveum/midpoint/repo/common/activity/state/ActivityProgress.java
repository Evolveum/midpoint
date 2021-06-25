/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.state;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.task.AbstractIterativeActivityExecution;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.statistics.OutcomeKeyedCounterTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityProgressUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityProgressType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OutcomeKeyedCounterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.QualifiedItemProcessingOutcomeType;

/**
 * Takes care of reporting the activity progress.
 *
 * The counters are managed in memory, and written to the running task (in-memory + repo representation) in regular intervals.
 *
 * Although this is mostly relevant for iterative activities (see {@link AbstractIterativeActivityExecution}), it has its
 * place also in non-standard activities that do their own iteration, like the cleanup activity.
 *
 * The distinction between open and closed items is relevant to activities with "commit points", like bucketed ones.
 */
public class ActivityProgress extends Initializable {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityProgress.class);

    @NotNull private final ActivityState<?> activityState;

    /**
     * Current value. Guarded by: this.
     */
    @NotNull private final ActivityProgressType value = new ActivityProgressType(PrismContext.get());

    ActivityProgress(@NotNull ActivityState<?> activityState) {
        this.activityState = activityState;
    }

    public void initialize(ActivityProgressType initialValue) {
        doInitialize(() -> {
            if (initialValue != null) {
                ActivityProgressUtil.addTo(this.value, initialValue);
            }
        });
    }

    /**
     * Increments the progress.
     */
    public synchronized void increment(QualifiedItemProcessingOutcomeType outcome, @NotNull Counters counters) {
        assertInitialized();
        List<OutcomeKeyedCounterType> counter = counters == Counters.COMMITTED ? value.getCommitted() : value.getUncommitted();
        int newCount = OutcomeKeyedCounterTypeUtil.incrementCounter(counter, outcome, getBeans().prismContext);
        LOGGER.trace("Incremented progress (counters: {}) to {} for {} in activity execution {}",
                counters, newCount, outcome, getActivityExecution());
    }

    /**
     * Moves "uncommitted" counters to "committed" state.
     */
    public synchronized void onCommitPoint() {
        assertInitialized();
        OutcomeKeyedCounterTypeUtil.addCounters(value.getCommitted(), value.getUncommitted());
        value.getUncommitted().clear();
        LOGGER.trace("Updated progress on commit point in activity execution: {}", getActivityExecution());
    }

    public synchronized void clearUncommitted() {
        assertInitialized();
        value.getUncommitted().clear();
    }

    /**
     * Returns a clone of the current value. The cloning is because of thread safety requirements.
     */
    public synchronized ActivityProgressType getValueCopy() {
        return value.cloneWithoutId();
    }

    /**
     * Writes current values to the running task: into the memory and to repository.
     * Not synchronized, as it operates on value copy.
     */
    public void writeToTaskAsPendingModification() throws ActivityExecutionException {
        assertInitialized();
        // TODO We should use the dynamic modification approach in order to provide most current values to the task
        //  (in case of update conflicts). But let's wait for the new repo with this.
        activityState.setItemRealValues(ActivityStateType.F_PROGRESS, getValueCopy());
    }

    private CommonTaskBeans getBeans() {
        return activityState.getBeans();
    }

    private @NotNull AbstractActivityExecution<?, ?, ?> getActivityExecution() {
        return activityState.getActivityExecution();
    }

    public synchronized long getLegacyValue() {
        return ActivityProgressUtil.getCurrentProgress(value);
    }

    public enum Counters {
        COMMITTED, UNCOMMITTED
    }
}
