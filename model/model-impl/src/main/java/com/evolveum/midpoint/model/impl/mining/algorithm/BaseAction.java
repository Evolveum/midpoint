/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemProcessingOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.QualifiedItemProcessingOutcomeType;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract (base) action class for clustering and pattern detection.
 * Assumes the execution within an activity!
 */
public abstract class BaseAction {

    /** This is the corresponding "activity run" object that gives us all the context. */
    @NotNull protected final AbstractActivityRun<?, ?, ?> activityRun;

    /**
     * Constructs a new `BaseAction` with the given activity run context.
     *
     * @param activityRun The abstract activity run within which the action is executed.
     */
    protected BaseAction(@NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        this.activityRun = activityRun;
    }
    /**
     * Increments the progress of the action and updates the outcome. This method is called during
     * the execution of the action to track the progress.
     */
    protected void incrementProgress() {
        activityRun.incrementProgress(
                new QualifiedItemProcessingOutcomeType()
                        .outcome(ItemProcessingOutcomeType.SUCCESS));
        // This is brutal hack as it generates its own operation result. TODO rework
        activityRun.getActivityState().getLiveItemProcessingStatistics().updateStatisticsForSimpleClients(true);
    }
}
