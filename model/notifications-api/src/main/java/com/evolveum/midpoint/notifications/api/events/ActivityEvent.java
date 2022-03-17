/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * An event related to an execution of an {@link Activity}.
 *
 * Preliminary implementation!
 */
@Experimental
public interface ActivityEvent extends Event {

    /**
     * Returns the activity run that this event is related to.
     */
    @NotNull AbstractActivityRun<?, ?, ?> getActivityRun();

    /**
     * Task in context of which the activity will execute, executes, or was executed.
     */
    @NotNull RunningTask getRunningTask();
}
