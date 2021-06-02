/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.task;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeActivityExecution;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.HandledObjectType;
import com.evolveum.midpoint.repo.common.task.ItemProcessor;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Execution of a propagation task. It has always a single part, so the resource can be stored here.
 */
@HandledObjectType(ShadowType.class)
public class PropagationActivityExecution
        extends AbstractSearchIterativeActivityExecution
        <ShadowType,
                PropagationWorkDefinition,
                PropagationActivityHandler,
                PropagationActivityExecution> {

    private static final String SHORT_NAME = "Propagation";

    private PrismObject<ResourceType> resource;

    PropagationActivityExecution(
            @NotNull ExecutionInstantiationContext<PropagationWorkDefinition, PropagationActivityHandler> context) {
        super(context, SHORT_NAME);
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        ActivityReportingOptions options = new ActivityReportingOptions();
        options.setPreserveStatistics(false);
        options.setEnableSynchronizationStatistics(false);
        return options;
    }

    @Override
    protected void initializeExecution(OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        String resourceOid = MiscUtil.requireNonNull(
                activity.getWorkDefinition().getResourceOid(),
                () -> "No resource specified");
        resource = activity.getHandler().provisioningService
                .getObject(ResourceType.class, resourceOid, null, getTask(), opResult);
        setContextDescription("to " + resource);
    }

    @Override
    protected ObjectQuery createQuery(OperationResult opResult) {
        return getPrismContext().queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
                .and()
                .exists(ShadowType.F_PENDING_OPERATION)
                .build();
    }

    @Override
    protected @NotNull ItemProcessor<PrismObject<ShadowType>> createItemProcessor(OperationResult opResult) {
        return createDefaultItemProcessor(
                (shadow, request, workerTask, result) -> {
                    try {
                        activity.getHandler().shadowsFacade.propagateOperations(resource, shadow, workerTask, result);
                        return true;
                    } catch (GenericFrameworkException | EncryptionException e) {
                        throw new SystemException("Generic provisioning framework error: " + e.getMessage(), e);
                    }
                }
        );
    }
}
