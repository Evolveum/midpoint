/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows.task;

import java.util.List;

import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Execution specifics of a propagation activity.
 */
public class PropagationActivityExecution
        extends SearchBasedActivityExecution<
            ShadowType,
            PropagationWorkDefinition,
            PropagationActivityHandler,
            AbstractActivityWorkStateType> {

    /** Fetched resource object. */
    private PrismObject<ResourceType> resource;

    PropagationActivityExecution(
            @NotNull ExecutionInstantiationContext<PropagationWorkDefinition, PropagationActivityHandler> context) {
        super(context, "Propagation");
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return super.getDefaultReportingOptions()
                .enableActionsExecutedStatistics(true)
                .enableSynchronizationStatistics(false);
    }

    @Override
    public void beforeExecution(OperationResult result) throws CommonException {
        String resourceOid = MiscUtil.requireNonNull(
                getWorkDefinition().getResourceOid(),
                () -> "No resource specified");
        resource = getActivityHandler().provisioningService
                .getObject(ResourceType.class, resourceOid, null, getRunningTask(), result);
        setContextDescription("to " + resource);
    }

    @Override
    @NotNull
    public SearchSpecification<ShadowType> createCustomSearchSpecification(OperationResult result) {
        return new SearchSpecification<>(
                ShadowType.class,
                PrismContext.get().queryFor(ShadowType.class)
                        .item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
                        .and()
                        .exists(ShadowType.F_PENDING_OPERATION)
                        .build(),
                List.of(),
                true);
    }

    @Override
    public boolean processObject(@NotNull PrismObject<ShadowType> shadow,
            @NotNull ItemProcessingRequest<PrismObject<ShadowType>> request, RunningTask workerTask, OperationResult result)
            throws CommonException {
        try {
            getActivityHandler().shadowsFacade.propagateOperations(resource, shadow, workerTask, result);
            return true;
        } catch (GenericFrameworkException | EncryptionException e) {
            throw new SystemException("Generic provisioning framework error: " + e.getMessage(), e);
        }
    }
}
