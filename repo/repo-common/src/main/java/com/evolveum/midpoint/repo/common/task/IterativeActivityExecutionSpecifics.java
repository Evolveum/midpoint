/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.state.ActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Contains execution logic (and sometimes also execution state) related to given specific iterative-style activity
 * - either plain iterative or search-based.
 *
 * Main responsibilities (at this abstract level):
 *
 * 1. provides custom code for initialization and finalization of the execution (if needed),
 * 2. provides default reporting configuration,
 * 3. indicates what activity state should be used to keep threshold counters - TODO to be reconsidered.
 *
 * The real "meat" (e.g. query formulation, item processing, etc) is in subtypes, though.
 *
 * This "specifics" functionality is separated from generic {@link IterativeActivityExecution} and its subclasses
 * in order to provide relatively simple and clean interface for activity implementors. (Originally the implementors
 * had to subclass these generic classes, leading to confusion about what exact functionality has to be provided.)
 */
public interface IterativeActivityExecutionSpecifics {

    /**
     * Called before the execution.
     *
     * Note that e.g. for search-based activities the search specification is *not* known at this moment.
     */
    default void beforeExecution(OperationResult opResult) throws CommonException, ActivityExecutionException {
    }

    /**
     * Called after the execution.
     */
    default void afterExecution(OperationResult opResult) throws CommonException, ActivityExecutionException {
    }

    /**
     * @return Default reporting options to be used during activity execution. These may be partially
     * overridden by the activity configuration (definition).
     */
    default ActivityReportingOptions getDefaultReportingOptions() {
        return new ActivityReportingOptions();
    }

    /**
     * Should we use activity state other than the state of the current activity when keeping the counters (e.g. for
     * thresholds)?
     */
    @Experimental
    default ActivityState useOtherActivityStateForCounters(@NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return null;
    }
}
