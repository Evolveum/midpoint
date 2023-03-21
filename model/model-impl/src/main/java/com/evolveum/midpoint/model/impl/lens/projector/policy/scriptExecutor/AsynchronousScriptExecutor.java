/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Executes scripts asynchronously.
 */
class AsynchronousScriptExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(AsynchronousScriptExecutor.class);

    private static final String OP_SUBMIT_SCRIPT = AsynchronousScriptExecutor.class.getName() + ".submitScript";

    @NotNull private final ActionContext actx;
    @NotNull private final ScriptingTaskCreator taskCreator;

    AsynchronousScriptExecutor(@NotNull ActionContext actx) {
        this.actx = actx;
        AsynchronousScriptExecutionType asynchronousExecution = actx.action.getAsynchronousExecution();
        AsynchronousScriptExecutionModeType mode = defaultIfNull(asynchronousExecution.getExecutionMode(), AsynchronousScriptExecutionModeType.ITERATIVE);
        this.taskCreator = createTaskCreator(actx, mode);
    }

    private ScriptingTaskCreator createTaskCreator(ActionContext actx, AsynchronousScriptExecutionModeType mode) {
        switch(mode) {
            case ITERATIVE:
                return new IterativeScriptingTaskCreator(actx);
            case SINGLE_RUN:
                return new SingleRunTaskCreator(actx);
            case SINGLE_RUN_NO_INPUT:
                return new SingleRunNoInputTaskCreator(actx);
            default:
                throw new AssertionError(mode);
        }
    }

    void submitScripts(OperationResult parentResult) {
        for (ExecuteScriptType executeScript : actx.action.getExecuteScript()) {
            submitScript(executeScript, parentResult);
        }
    }

    private void submitScript(ExecuteScriptType executeScript, OperationResult parentResult) {
        // The subresult cannot be minor because we need to preserve background task OID
        OperationResult result = parentResult.createSubresult(OP_SUBMIT_SCRIPT);
        try {
            TaskType newTask = taskCreator.createTask(executeScript, result);
            submitTask(result, newTask);
        } catch (Throwable t) {
            result.recordFatalError(t);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't submit script for asynchronous execution: {}", t, actx.action);
            // Should not re-throw the exception in order to submit other scripts
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void submitTask(OperationResult result, TaskType newTask)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        Set<ObjectDelta<? extends ObjectType>> deltas = singleton(DeltaFactory.Object.createAddDelta(newTask.asPrismObject()));
        ModelExecuteOptions options = ModelExecuteOptions.create().preAuthorized();
        Collection<ObjectDeltaOperation<? extends ObjectType>> operations =
                actx.beans.modelService.executeChanges(deltas, options, actx.task, result);
        String oid = ObjectDeltaOperation.findAddDeltaOid(operations, newTask.asPrismObject());
        result.setBackgroundTaskOid(oid);
    }
}
