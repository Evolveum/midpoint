/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity;

import com.evolveum.midpoint.model.impl.tasks.AbstractSearchIterativeModelTaskPartExecution;
import com.evolveum.midpoint.repo.common.task.HandledObjectType;
import com.evolveum.midpoint.repo.common.task.ResultHandlerClass;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@ResultHandlerClass(ShadowIntegrityCheckResultHandler.class)
@HandledObjectType(ShadowType.class)
public class ShadowIntegrityCheckTaskPartExecution
        extends AbstractSearchIterativeModelTaskPartExecution
        <ShadowType,
                ShadowIntegrityCheckTaskHandler,
                ShadowIntegrityCheckTaskHandler.TaskExecution,
                ShadowIntegrityCheckTaskPartExecution,
                ShadowIntegrityCheckResultHandler> {

    public ShadowIntegrityCheckTaskPartExecution(ShadowIntegrityCheckTaskHandler.TaskExecution taskExecution) {
        super(taskExecution);
    }

    @Override
    protected boolean requiresDirectRepositoryAccess(OperationResult opResult) {
        return true;
    }
}
