/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.task;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.impl.ShadowCache;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 */
public class PropagationResultHandler extends AbstractSearchIterativeResultHandler<ShadowType> {

    private final ShadowCache shadowCache;
    private final PrismObject<ResourceType> resource;

    public PropagationResultHandler(RunningTask coordinatorTask, String taskOperationPrefix, TaskManager taskManager, ShadowCache shadowCache, PrismObject<ResourceType> resource) {
        super(coordinatorTask, taskOperationPrefix, "propagation", "to "+resource, null, taskManager);
        this.shadowCache = shadowCache;
        this.resource = resource;
    }

    protected PrismObject<ResourceType> getResource() {
        return resource;
    }

    @Override
    protected boolean handleObject(PrismObject<ShadowType> shadow, RunningTask workerTask, OperationResult result)
            throws CommonException {
        try {
            shadowCache.propagateOperations(resource, shadow, workerTask, result);
        } catch (GenericFrameworkException | EncryptionException e) {
            throw new SystemException("Generic provisioning framework error: " + e.getMessage(), e);
        }
        return true;
    }

}
