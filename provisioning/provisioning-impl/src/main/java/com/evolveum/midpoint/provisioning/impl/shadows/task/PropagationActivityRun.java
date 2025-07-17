/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows.task;

import java.util.List;

import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
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
public final class PropagationActivityRun
        extends SearchBasedActivityRun<
            ShadowType,
            PropagationWorkDefinition,
            PropagationActivityHandler,
            AbstractActivityWorkStateType> {

    /** Fetched resource object. */
    private ResourceType resource;

    PropagationActivityRun(
            @NotNull ActivityRunInstantiationContext<PropagationWorkDefinition, PropagationActivityHandler> context) {
        super(context, "Propagation");
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .actionsExecutedStatisticsSupported(true)
                .synchronizationStatisticsSupported(false);
    }

    @Override
    public boolean beforeRun(OperationResult result) throws CommonException, ActivityRunException {
        if (!super.beforeRun(result)) {
            return false;
        }

        ensureNoPreviewNorDryRun();
        String resourceOid = MiscUtil.requireNonNull(
                getWorkDefinition().getResourceOid(),
                () -> "No resource specified");
        resource =
                getActivityHandler().provisioningService
                        .getObject(ResourceType.class, resourceOid, null, getRunningTask(), result)
                        .asObjectable();
        setContextDescription("to " + resource);

        return true;
    }

    @Override
    protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
        return ObjectTypeUtil.createObjectRef(resource);
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
    public boolean processItem(
            @NotNull ShadowType repoShadow,
            @NotNull ItemProcessingRequest<ShadowType> request,
            @NotNull RunningTask workerTask,
            @NotNull OperationResult result)
            throws CommonException {
        try {
            getActivityHandler().shadowsFacade.propagateOperations(resource, repoShadow, workerTask, result);
            return true;
        } catch (GenericFrameworkException | EncryptionException e) {
            throw new SystemException("Generic provisioning framework error: " + e.getMessage(), e);
        }
    }
}
