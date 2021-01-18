/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.scripting;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.model.api.ScriptExecutionResult;
import com.evolveum.midpoint.model.api.ScriptingService;
import com.evolveum.midpoint.model.impl.tasks.simple.Processing;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleIterativeTaskHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ValueListType;

@Component
public class IterativeScriptExecutionTaskHandler
        extends SimpleIterativeTaskHandler
        <ObjectType,
                IterativeScriptExecutionTaskHandler.MyExecutionContext,
                IterativeScriptExecutionTaskHandler.MyProcessing> {

    @Autowired private TaskManager taskManager;
    @Autowired private ScriptingService scriptingService;

    private static final Trace LOGGER = TraceManager.getTrace(IterativeScriptExecutionTaskHandler.class);

    public IterativeScriptExecutionTaskHandler() {
        super("Execute script", OperationConstants.EXECUTE_SCRIPT);
        setPreserveStatistics(false);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(ModelPublicConstants.ITERATIVE_SCRIPT_EXECUTION_TASK_HANDLER_URI, this);
    }

    @Override
    protected MyExecutionContext createExecutionContext() {
        return new MyExecutionContext();
    }

    @Override
    protected MyProcessing createProcessing(MyExecutionContext ctx) {
        return new MyProcessing(ctx);
    }

    public static class MyExecutionContext extends com.evolveum.midpoint.model.impl.tasks.simple.ExecutionContext {

        private ExecuteScriptType executeScriptRequestTemplate;

        @Override
        protected void initialize(OperationResult opResult) {
            RunningTask localCoordinatorTask = getLocalCoordinationTask();
            executeScriptRequestTemplate = getExecuteScriptRequest(localCoordinatorTask);
            if (executeScriptRequestTemplate.getInput() != null && !executeScriptRequestTemplate.getInput().getValue().isEmpty()) {
                LOGGER.warn("Ignoring input values in executeScript data in task {}", localCoordinatorTask);
            }
        }
    }

    public class MyProcessing extends Processing<ObjectType, MyExecutionContext> {

        private MyProcessing(MyExecutionContext ctx) {
            super(ctx);
        }

        @Override
        protected void handleObject(PrismObject<ObjectType> object, RunningTask workerTask, OperationResult result)
                throws CommonException, PreconditionViolationException, ScriptExecutionException {
            ExecuteScriptType executeScriptRequest = ctx.executeScriptRequestTemplate.clone();
            executeScriptRequest.setInput(new ValueListType().value(object.asObjectable()));
            ScriptExecutionResult executionResult = scriptingService.evaluateExpression(executeScriptRequest,
                    VariablesMap.emptyMap(), false, workerTask, result);
            LOGGER.debug("Execution output: {} item(s)", executionResult.getDataOutput().size());
            LOGGER.debug("Execution result:\n{}", executionResult.getConsoleOutput());
            result.computeStatus();
        }
    }

    static ExecuteScriptType getExecuteScriptRequest(RunningTask coordinatorTask) {
        PrismProperty<ExecuteScriptType> executeScriptProperty = coordinatorTask.getExtensionPropertyOrClone(SchemaConstants.SE_EXECUTE_SCRIPT);
        if (executeScriptProperty != null && executeScriptProperty.getValue().getValue() != null &&
                executeScriptProperty.getValue().getValue().getScriptingExpression() != null) {
            return executeScriptProperty.getValue().getValue();
        } else {
            throw new IllegalStateException("There's no script to be run in task " + coordinatorTask + " (property " + SchemaConstants.SE_EXECUTE_SCRIPT + ")");
        }
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.BULK_ACTIONS;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_ITERATIVE_BULK_ACTION_TASK.value();
    }

    @Override
    public String getDefaultChannel() {
        return null; // The channel URI should be provided by the task creator.
    }
}
