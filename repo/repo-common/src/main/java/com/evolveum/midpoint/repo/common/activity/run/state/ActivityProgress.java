/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import java.util.List;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.IterativeActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.statistics.OutcomeKeyedCounterTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityProgressUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.Nullable;

/**
 * Takes care of reporting the activity progress.
 *
 * The counters are managed in memory, and written to the running task (in-memory + repo representation) in regular intervals.
 *
 * Although this is mostly relevant for iterative activities (see {@link IterativeActivityRun}), it has its
 * place also in non-standard activities that do their own iteration, like the cleanup activity.
 *
 * The distinction between open and closed items is relevant to activities with "commit points", like bucketed ones.
 */
public class ActivityProgress extends Initializable {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityProgress.class);

    @NotNull private final CurrentActivityState<?> activityState;

    /**
     * Current value. Guarded by: this.
     */
    @NotNull private final ActivityProgressType value = new ActivityProgressType();

    ActivityProgress(@NotNull CurrentActivityState<?> activityState) {
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
        LOGGER.trace("Incremented progress (counters: {}) to {} for {} in activity run {}",
                counters, newCount, outcome, getActivityRun());
    }

    /**
     * Moves "uncommitted" counters to "committed" state.
     */
    public synchronized void onCommitPoint() {
        assertInitialized();
        OutcomeKeyedCounterTypeUtil.addCounters(value.getCommitted(), value.getUncommitted());
        value.getUncommitted().clear();
        LOGGER.trace("Updated progress on commit point in activity run: {}", getActivityRun());
    }

    public synchronized void clearUncommitted() {
        assertInitialized();
        value.getUncommitted().clear();
    }

    public synchronized @Nullable Integer getExpectedTotal() {
        return value.getExpectedTotal();
    }

    public synchronized void setExpectedTotal(Integer expectedTotal) {
        value.setExpectedTotal(expectedTotal);
    }

    public synchronized void setExpectedInCurrentBucket(Integer expectedInCurrentBucket) {
        value.setExpectedInCurrentBucket(expectedInCurrentBucket);
    }

    /**
     * Returns a clone of the current value. The cloning is because of thread safety requirements.
     */
    public synchronized @NotNull ActivityProgressType getValueCopy() {
        return value.cloneWithoutId();
    }

    /**
     * Writes current values to the running task: into the memory and to repository.
     * Not synchronized, as it operates on value copy.
     */
    void writeToTaskAsPendingModification() throws ActivityRunException {
        assertInitialized();
        // TODO We should use the dynamic modification approach in order to provide most current values to the task
        //  (in case of update conflicts). But let's wait for the new repo with this.
        activityState.setItemRealValues(ActivityStateType.F_PROGRESS, getValueCopy());
    }

    private CommonTaskBeans getBeans() {
        return activityState.getBeans();
    }

    private @NotNull AbstractActivityRun<?, ?, ?> getActivityRun() {
        return activityState.getActivityRun();
    }

    synchronized long getLegacyValue() {
        return ActivityProgressUtil.getCurrentProgress(value);
    }

    public @NotNull ItemsProgressOverviewType getOverview() {
        return ActivityProgressUtil.getProgressOverview(
                getValueCopy());
    }

    public enum Counters {
        COMMITTED, UNCOMMITTED
    }
}
