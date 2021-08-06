/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.tasks.ResourceObjectClassSpecification;
import com.evolveum.midpoint.model.impl.sync.tasks.SynchronizationObjectsFilterImpl;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.state.ActivityState;
import com.evolveum.midpoint.repo.common.task.BaseSearchBasedExecutionSpecificsImpl;
import com.evolveum.midpoint.repo.common.task.SearchBasedActivityExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReconciliationWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Code common to all three reconciliation sub-activities: operation completion, resource reconciliation,
 * and remaining shadows reconciliation.
 */
public abstract class PartialReconciliationActivityExecutionSpecifics
        extends BaseSearchBasedExecutionSpecificsImpl
        <ShadowType,
                ReconciliationWorkDefinition,
                ReconciliationActivityHandler> {

    ResourceObjectClassSpecification objectClassSpec;
    SynchronizationObjectsFilterImpl objectsFilter;

    PartialReconciliationActivityExecutionSpecifics(@NotNull SearchBasedActivityExecution<ShadowType,
            ReconciliationWorkDefinition, ReconciliationActivityHandler, ?> activityExecution) {
        super(activityExecution);
    }

    @Override
    public void beforeExecution(OperationResult opResult) throws CommonException, ActivityExecutionException {
        ResourceObjectSetType resourceObjectSet = getResourceObjectSet();

        objectClassSpec = getModelBeans().syncTaskHelper
                .createObjectClassSpec(resourceObjectSet, getRunningTask(), opResult);
        objectsFilter = objectClassSpec.getObjectFilter(resourceObjectSet);

        objectClassSpec.checkNotInMaintenance();

        activityExecution.setContextDescription(activityExecution.getShortName() + " on " +
                objectClassSpec.getContextDescription()); // TODO?
    }

    protected @NotNull ResourceObjectSetType getResourceObjectSet() {
        return getWorkDefinition().getResourceObjectSetSpecification();
    }

    @Override
    public ActivityState useOtherActivityStateForCounters(@NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return getActivityState().getParentActivityState(ReconciliationWorkStateType.COMPLEX_TYPE, result);
    }

    protected @NotNull ModelBeans getModelBeans() {
        return getActivityHandler().getModelBeans();
    }
}
