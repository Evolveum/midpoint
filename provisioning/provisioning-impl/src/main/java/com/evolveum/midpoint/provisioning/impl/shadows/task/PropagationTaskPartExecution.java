/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.task;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeTaskPartExecution;
import com.evolveum.midpoint.repo.common.task.HandledObjectType;
import com.evolveum.midpoint.repo.common.task.ItemProcessorClass;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Execution of a propagation task. It has always a single part, so the resource can be stored here.
 */
@ItemProcessorClass(PropagationItemProcessor.class)
@HandledObjectType(ShadowType.class)
public class PropagationTaskPartExecution
        extends AbstractSearchIterativeTaskPartExecution
        <ShadowType,
                PropagationTaskHandler,
                PropagationTaskHandler.TaskExecution,
                PropagationTaskPartExecution,
                PropagationItemProcessor> {

    private PrismObject<ResourceType> resource;

    public PropagationTaskPartExecution(PropagationTaskHandler.TaskExecution taskExecution) {
        super(taskExecution);
        setContextDescription("to " + resource);
    }

    @Override
    protected void initialize(OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        String resourceOid = localCoordinatorTask.getObjectOid();
        if (resourceOid == null) {
            throw new SchemaException("No resource specified");
        }
        resource = taskHandler.provisioningService.getObject(ResourceType.class, resourceOid, null,
                localCoordinatorTask, opResult);
    }

    @Override
    protected ObjectQuery createQuery(OperationResult opResult) {
        return getPrismContext().queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
                .and()
                .exists(ShadowType.F_PENDING_OPERATION)
                .build();
    }

    public PrismObject<ResourceType> getResource() {
        return resource;
    }
}
