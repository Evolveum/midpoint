/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaCollectionsUtil;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Executes scripts asynchronously.
 */
@Component
public class AsynchronousScriptExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(AsynchronousScriptExecutor.class);

    private static final String OP_SUBMIT_SCRIPT = AsynchronousScriptExecutor.class.getName() + ".submitScript";

    @Autowired private PolicyRuleScriptExecutor policyRuleScriptExecutor;
    @Autowired private PrismContext prismContext;
    @Autowired private ModelService modelService;

    public void execute(ScriptExecutionPolicyActionType action, EvaluatedPolicyRuleImpl rule, LensContext<?> context,
            Task task, OperationResult parentResult) {
        AsynchronousScriptExecutionType asynchronousExecution = action.getAsynchronous();
        assert asynchronousExecution != null;

        if (action.getExecuteScript().size() != 1) {
            throw new IllegalArgumentException("Expected exactly one 'executeScript' element in policy action, got "
                    + action.getExecuteScript().size() + " in " + action);
        }

        OperationResult result = parentResult.createSubresult(OP_SUBMIT_SCRIPT); // cannot be minor because of background task OID
        try {
            ExecuteScriptType executeScript = action.getExecuteScript().get(0);
            TaskType newTask;
            switch (defaultIfNull(asynchronousExecution.getExecutionMode(), AsynchronousScriptExecutionModeType.ITERATIVE)) {
                case ITERATIVE:
                    newTask = new IterativeScriptingTaskCreator(policyRuleScriptExecutor, context)
                            .create(executeScript, action, task, result);
                    break;
//                case SINGLE_RUN:
//                    newTask = new SingleRunScriptingTaskCreator().create();
//                    break;
//                case SINGLE_RUN_NO_INPUT:
//                    newTask = new SingleRunScriptingTaskCreator().create();
//                    break;
                default:
                    throw new AssertionError(asynchronousExecution.getExecutionMode());
            }

            Set<ObjectDelta<? extends ObjectType>> deltas = singleton(DeltaFactory.Object.createAddDelta(newTask.asPrismObject()));
            ModelExecuteOptions options = new ModelExecuteOptions(prismContext).preAuthorized();
            Collection<ObjectDeltaOperation<? extends ObjectType>> operations = modelService.executeChanges(deltas, options, task, result);
            String oid = ObjectDeltaOperation.findAddDeltaOid(operations, newTask.asPrismObject());
            System.out.println("New task OID = " + oid);
            result.setAsynchronousOperationReference(oid);

        } catch (Throwable t) {
            result.recordFatalError(t);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't submit script for asynchronous execution: {}", t, action);
        } finally {
            result.computeStatusIfUnknown();
        }
    }
}
