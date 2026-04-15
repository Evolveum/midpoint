/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityReportingDefinition;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

import org.jetbrains.annotations.NotNull;

/**
 * The state of a virtual child activity.
 *
 * Virtual activities are used to provide state information for "activities" that do not exist in reality:
 * they have no definition, no run ({@link AbstractActivityRun}), and we only want to keep track of their state
 * and progress (e.g. to display that in the GUI).
 *
 * A typical reason for using such activities (and not regular ones) is the situation where the execution of
 * the the parent activity is tightly coupled with the execution of its children, so it's not practical
 * to divide them into separate {@link AbstractActivityRun} classes.
 *
 * Virtual activity state has no work state, and other items may be empty as well (e.g., bucketing).
 *
 * @param <WS> the work state of the parent activity run
 */
@Experimental
public class VirtualActivityState<WS extends AbstractActivityWorkStateType> extends CurrentActivityState<WS> {

    @NotNull private final ItemPath parentStateItemPath;
    @NotNull private final String childIdentifier;

    VirtualActivityState(
            @NotNull AbstractActivityRun<?, ?, WS> activityRun,
            @NotNull ItemPath parentStateItemPath,
            @NotNull String childIdentifier) {
        super(activityRun);
        this.parentStateItemPath = parentStateItemPath;
        this.childIdentifier = childIdentifier;
    }

    @Override
    @NotNull ItemPath findOrCreateActivityState(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        return findOrCreateChildActivityState(parentStateItemPath, childIdentifier, result);
    }

    @Override
    @NotNull ActivityReportingDefinition getReportingDefinition() {
        return ActivityReportingDefinition.create(null); // Virtual activities do not need reporting
    }

    @Override
    boolean shouldCreateWorkStateOnInitialization() {
        return false; // Virtual activities do not have work state
    }

    @Override
    boolean areRunRecordsSupported() {
        return false; // Virtual activities do not have run records
    }
}
