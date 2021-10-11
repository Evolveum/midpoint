/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.util;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Task handler for "reindex" task.
 * It simply executes empty modification delta on each repository object.
 *
 * TODO implement also for sub-objects, namely certification cases.
 */
@Component
public class ReindexTaskHandler extends AbstractSearchIterativeModelTaskHandler<ObjectType, ReindexResultHandler> {

    public static final String HANDLER_URI = ModelPublicConstants.REINDEX_TASK_HANDLER_URI;

    public ReindexTaskHandler() {
        super("Reindex", OperationConstants.REINDEX);
        setLogFinishInfo(true);
        setPreserveStatistics(false);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    protected ReindexResultHandler createHandler(TaskPartitionDefinitionType partition, TaskRunResult runResult,
            RunningTask coordinatorTask, OperationResult opResult) throws SchemaException, SecurityViolationException,
            ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        securityEnforcer.authorize(AuthorizationConstants.AUTZ_ALL_URL, null, AuthorizationParameters.EMPTY, null, coordinatorTask, opResult);
        return new ReindexResultHandler(coordinatorTask, ReindexTaskHandler.class.getName(),
                "reindex", "reindex", taskManager, repositoryService);
    }

    @Override
    protected Class<? extends ObjectType> getType(Task task) {
        return getTypeFromTask(task, ObjectType.class);
    }

    @Override
    protected boolean requiresDirectRepositoryAccess(ReindexResultHandler resultHandler, TaskRunResult runResult, Task coordinatorTask, OperationResult opResult) {
        return true;
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }
}
