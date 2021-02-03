/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.task;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeItemProcessor;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Item processor for the propagation task.
 *
 * @author semancik
 */
public class PropagationItemProcessor
        extends AbstractSearchIterativeItemProcessor
        <ShadowType,
                PropagationTaskHandler,
                PropagationTaskHandler.TaskExecution,
                PropagationTaskPartExecution,
                PropagationItemProcessor> {

    public PropagationItemProcessor(PropagationTaskPartExecution taskExecution) {
        super(taskExecution);
    }

    @Override
    protected boolean processObject(PrismObject<ShadowType> shadow,
            ItemProcessingRequest<PrismObject<ShadowType>> request,
            RunningTask workerTask, OperationResult result)
            throws CommonException {
        try {
            taskHandler.getShadowCache().propagateOperations(partExecution.getResource(), shadow, workerTask, result);
            return true;
        } catch (GenericFrameworkException | EncryptionException e) {
            throw new SystemException("Generic provisioning framework error: " + e.getMessage(), e);
        }
    }
}
