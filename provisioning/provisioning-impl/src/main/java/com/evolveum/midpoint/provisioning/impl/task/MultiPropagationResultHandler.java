/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.task;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Object handler for the multi-propagation task.
 *
 * @author semancik
 */
public class MultiPropagationResultHandler
        extends AbstractSearchIterativeResultHandler
        <ResourceType,
                MultiPropagationTaskHandler,
                MultiPropagationTaskHandler.TaskExecution,
                MultiPropagationTaskPartExecution,
                MultiPropagationResultHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(MultiPropagationResultHandler.class);

    public MultiPropagationResultHandler(MultiPropagationTaskPartExecution taskExecution) {
        super(taskExecution, "propagation", "multipropagation");
    }

    @Override
    protected boolean handleObject(PrismObject<ResourceType> resource, RunningTask workerTask, OperationResult taskResult)
            throws CommonException {

        LOGGER.trace("Propagating provisioning operations on {}", resource);

        ObjectQuery query = resource.getPrismContext().queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
                .and()
                .exists(ShadowType.F_PENDING_OPERATION)
            .build();

        ResultHandler<ShadowType> handler =
                (shadow, result) -> {
                    propagateShadowOperations(resource, shadow, workerTask, result);
                    return true;
                };

        taskHandler.getRepositoryService()
                .searchObjectsIterative(ShadowType.class, query, handler, null, true, taskResult);

        LOGGER.trace("Propagation of {} done", resource);

        return true;
    }

    private void propagateShadowOperations(PrismObject<ResourceType> resource, PrismObject<ShadowType> shadow,
            Task workerTask, OperationResult result) {
        try {
            taskHandler.getShadowCache().propagateOperations(resource, shadow, workerTask, result);
        } catch (CommonException | GenericFrameworkException | EncryptionException e) {
            throw new SystemException("Generic provisioning framework error: " + e.getMessage(), e);
        }
    }
}
