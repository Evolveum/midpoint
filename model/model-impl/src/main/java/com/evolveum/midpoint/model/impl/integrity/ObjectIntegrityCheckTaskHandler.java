/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.util.AbstractSearchIterativeModelTaskHandler;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;

/**
 * Task handler for "Object integrity check" task.
 *
 * The purpose of this task is to detect and optionally fix anomalies in repository objects.
 *
 * However, currently its only function is to display information about objects size.
 */
@Component
public class ObjectIntegrityCheckTaskHandler extends AbstractSearchIterativeModelTaskHandler<ObjectType, ObjectIntegrityCheckResultHandler> {

    public static final String HANDLER_URI = ModelPublicConstants.OBJECT_INTEGRITY_CHECK_TASK_HANDLER_URI;

    // WARNING! This task handler is efficiently singleton!
     // It is a spring bean and it is supposed to handle all search task instances
     // Therefore it must not have task-specific fields. It can only contain fields specific to
     // all tasks of a specified type

    @Autowired private SystemObjectCache systemObjectCache;

    private static final Trace LOGGER = TraceManager.getTrace(ObjectIntegrityCheckTaskHandler.class);

    public ObjectIntegrityCheckTaskHandler() {
        super("Object integrity check", OperationConstants.CHECK_OBJECT_INTEGRITY);
        setLogFinishInfo(true);
        setPreserveStatistics(false);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    protected ObjectIntegrityCheckResultHandler createHandler(TaskPartitionDefinitionType partition, TaskRunResult runResult, RunningTask coordinatorTask, OperationResult opResult) {
        return new ObjectIntegrityCheckResultHandler(coordinatorTask, ObjectIntegrityCheckTaskHandler.class.getName(),
                "check object integrity", "check object integrity", taskManager, prismContext,
                repositoryService, systemObjectCache, opResult);
    }

    @Override
    protected Class<? extends ObjectType> getType(Task task) {
        return ObjectType.class;
    }

    @Override
    protected ObjectQuery createQuery(ObjectIntegrityCheckResultHandler handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {
        ObjectQuery query = createQueryFromTask(handler, runResult, task, opResult);
        LOGGER.info("Using query:\n{}", query.debugDump());
        return query;
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptions(
            ObjectIntegrityCheckResultHandler resultHandler,
            TaskRunResult runResult, Task coordinatorTask, OperationResult opResult) {
        Collection<SelectorOptions<GetOperationOptions>> optionsFromTask = createSearchOptionsFromTask(resultHandler,
                runResult, coordinatorTask, opResult);
        return SelectorOptions.updateRootOptions(optionsFromTask, opt -> opt.setAttachDiagData(true), GetOperationOptions::new);
    }

    @Override
    protected boolean requiresDirectRepositoryAccess(ObjectIntegrityCheckResultHandler resultHandler, TaskRunResult runResult, Task coordinatorTask, OperationResult opResult) {
        return true;
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }
}
