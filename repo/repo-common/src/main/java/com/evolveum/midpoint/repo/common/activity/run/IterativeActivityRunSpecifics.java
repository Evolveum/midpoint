/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityReportingDefinition;
import com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.ImplicitSegmentationResolver;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkSegmentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImplicitWorkSegmentationType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * This interface summarizes what should an implementor of {@link IterativeActivityRun}
 * (either plain or search-based) provide.
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
 * This "specifics" functionality is pulled out from {@link IterativeActivityRun} to precisely
 * describe the interface between the generic activity framework and specific activity implementation.
 */
@SuppressWarnings("RedundantThrows")
public interface IterativeActivityRunSpecifics extends ImplicitSegmentationResolver {

    /**
     * Called before the run.
     *
     * Note that e.g. for search-based activities the search specification is *not* known at this moment.
     *
     * @return true if the run should continue, false if it should be skipped.
     */
    default boolean beforeRun(OperationResult result) throws CommonException, ActivityRunException {
        return true; // true means that the run should continue
    }

    /**
     * Called after the run (but only if {@link #beforeRun(OperationResult)} returned `true`).
     */
    default void afterRun(OperationResult result) throws CommonException, ActivityRunException {
    }

    /**
     * Called before bucket is processed.
     *
     * (For search-based tasks the search specification is already prepared, including narrowing using bucket.)
     */
    default void beforeBucketProcessing(OperationResult result) throws ActivityRunException, CommonException {
    }

    /**
     * Called after bucket is processed.
     */
    default void afterBucketProcessing(OperationResult result) throws ActivityRunException, CommonException {
    }

    /**
     * @return Reporting characteristics of the activity run. They e.g. provide default values
     * for {@link ActivityReportingDefinition}.
     */
    @NotNull ActivityReportingCharacteristics createReportingCharacteristics();

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
