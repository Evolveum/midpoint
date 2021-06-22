/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.sync.tasks.SynchronizationObjectsFilterImpl;
import com.evolveum.midpoint.model.impl.sync.tasks.ResourceObjectClassSpecification;
import com.evolveum.midpoint.model.impl.tasks.AbstractModelSearchActivityExecution;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Code common to all three reconciliation sub-activities: operation completion, resource reconciliation,
 * and remaining shadows reconciliation.
 *
 * @param <AE> specific sub-activity execution
 */
public class PartialReconciliationActivityExecution<AE extends PartialReconciliationActivityExecution<AE>>
        extends AbstractModelSearchActivityExecution
        <ShadowType,
                        ReconciliationWorkDefinition,
                        ReconciliationActivityHandler,
                        AE,
                        AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(PartialReconciliationActivityExecution.class);

    protected ResourceObjectClassSpecification objectClassSpec;
    protected SynchronizationObjectsFilterImpl objectsFilter;

    public PartialReconciliationActivityExecution(
            @NotNull ExecutionInstantiationContext<ReconciliationWorkDefinition, ReconciliationActivityHandler> context,
            @NotNull String shortNameCapitalized) {
        super(context, shortNameCapitalized);
    }

    @Override
    protected void initializeExecution(OperationResult opResult) throws CommonException, ActivityExecutionException {
        ResourceObjectSetType resourceObjectSet = getResourceObjectSet();

        objectClassSpec = getModelBeans().syncTaskHelper
                .createObjectClassSpec(resourceObjectSet, getRunningTask(), opResult);
        objectsFilter = objectClassSpec.getObjectFilter(resourceObjectSet);

        objectClassSpec.checkNotInMaintenance();

        setContextDescription(getActivityShortNameCapitalized() + " on " +
                objectClassSpec.getContextDescription()); // TODO?
    }

    protected @NotNull ResourceObjectSetType getResourceObjectSet() {
        return activity.getWorkDefinition().getResourceObjectSetSpecification();
    }
}
