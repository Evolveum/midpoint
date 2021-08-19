/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.repo.common.task.work.segmentation.ImplicitSegmentationResolver;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkSegmentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImplicitWorkSegmentationType;

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
 * Main responsibilities (at this level of abstraction):
 *
 * 1. provides custom code to be executed before/after the real execution (if needed),
 * 2. provides custom code to be executed before/after individual buckets are executed (if needed),
 * 3. provides default reporting configuration,
 * 4. indicates what activity state should be used to keep threshold counters - TODO to be reconsidered,
 * 5. interprets implicit work segmentation configuration.
 *
 * The real "meat" (e.g. query formulation, item processing, etc) is in subtypes, though.
 *
 * This "specifics" functionality is separated from generic {@link IterativeActivityExecution} and its subclasses
 * in order to provide relatively simple and clean interface for activity implementors. (Originally the implementors
 * had to subclass these generic classes, leading to confusion about what exact functionality has to be provided.)
 */
@SuppressWarnings("RedundantThrows")
public interface IterativeActivityExecutionSpecifics extends ImplicitSegmentationResolver {

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
     * Called before bucket is executed.
     *
     * (For search-based tasks the search specification is already prepared, including narrowing using bucket.)
     */
    default void beforeBucketExecution(OperationResult result) throws ActivityExecutionException, CommonException {
    }

    /**
     * Called after bucket is executed.
     */
    default void afterBucketExecution(OperationResult result) throws ActivityExecutionException, CommonException {
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

    @Override
    default AbstractWorkSegmentationType resolveImplicitSegmentation(@NotNull ImplicitWorkSegmentationType segmentation) {
        throw new UnsupportedOperationException("Implicit work segmentation configuration is not available in this activity");
    }
}
