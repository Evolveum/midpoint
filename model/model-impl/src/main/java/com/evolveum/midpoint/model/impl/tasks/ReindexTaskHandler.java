/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks;

import static java.util.Collections.emptyList;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.tasks.simple.ExecutionContext;
import com.evolveum.midpoint.model.impl.tasks.simple.Processing;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleIterativeTaskHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

/**
 * Task handler for "reindex" task.
 * It simply executes empty modification delta on each repository object.
 *
 * TODO implement also for sub-objects, namely certification cases.
 */
@Component
public class ReindexTaskHandler
        extends SimpleIterativeTaskHandler
        <ObjectType,
                ReindexTaskHandler.MyExecutionContext,
                ReindexTaskHandler.MyProcessing> {

    public static final String HANDLER_URI = ModelPublicConstants.REINDEX_TASK_HANDLER_URI;

    public ReindexTaskHandler() {
        super("Reindex", OperationConstants.REINDEX);
        reportingOptions.setPreserveStatistics(false);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    protected MyExecutionContext createExecutionContext() {
        return new MyExecutionContext();
    }

    @Override
    protected MyProcessing createProcessing(MyExecutionContext ctx) {
        return new MyProcessing(ctx);
    }

    public class MyExecutionContext extends ExecutionContext {

        @Override
        protected void initialize(OperationResult opResult) throws CommunicationException, ObjectNotFoundException,
                SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

            securityEnforcer.authorizeAll(getLocalCoordinationTask(), opResult);
        }
    }

    public class MyProcessing extends Processing<ObjectType, MyExecutionContext> {

        private MyProcessing(MyExecutionContext ctx) {
            super(ctx);
        }

        @Override
        protected void handleObject(PrismObject<ObjectType> object, RunningTask workerTask, OperationResult result)
                throws CommonException, PreconditionViolationException {

            repositoryService.modifyObject(object.asObjectable().getClass(), object.getOid(), emptyList(),
                    RepoModifyOptions.createExecuteIfNoChanges(), result);
        }
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
