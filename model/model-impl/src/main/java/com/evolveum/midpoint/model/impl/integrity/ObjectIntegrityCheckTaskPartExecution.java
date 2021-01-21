/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.tasks.AbstractSearchIterativeModelTaskPartExecution;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.task.DefaultHandledObjectType;
import com.evolveum.midpoint.repo.common.task.ResultHandlerClass;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@ResultHandlerClass(ObjectIntegrityCheckResultHandler.class)
@DefaultHandledObjectType(ObjectType.class)
public class ObjectIntegrityCheckTaskPartExecution
        extends AbstractSearchIterativeModelTaskPartExecution
        <ObjectType,
                ObjectIntegrityCheckTaskHandler,
                ObjectIntegrityCheckTaskHandler.TaskExecution,
                ObjectIntegrityCheckTaskPartExecution, ObjectIntegrityCheckResultHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectIntegrityCheckTaskPartExecution.class);

    ObjectIntegrityCheckTaskPartExecution(ObjectIntegrityCheckTaskHandler.TaskExecution taskExecution) {
        super(taskExecution);
    }

    @Override
    protected ObjectQuery createQuery(OperationResult opResult) throws SchemaException {
        ObjectQuery query = createQueryFromTask();
        LOGGER.info("Using query:\n{}", query.debugDump());
        return query;
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptions(
            OperationResult opResult) {
        Collection<SelectorOptions<GetOperationOptions>> optionsFromTask = createSearchOptionsFromTask();
        return SelectorOptions.updateRootOptions(optionsFromTask, opt -> opt.setAttachDiagData(true), GetOperationOptions::new);
    }

    @Override
    protected boolean requiresDirectRepositoryAccess(OperationResult opResult) {
        return true;
    }
}
