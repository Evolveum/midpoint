/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.security.api.SecurityContextManager;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExecutionPolicyActionType;

/**
 * Executes scripts defined in scriptExecution policy action.
 * Designed to be called during FINAL stage, just like notification action.
 *
 * TODO We should merge equivalent policy rules so that e.g. recomputation is not done multiple times.
 */
@Component
public class PolicyRuleScriptExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleScriptExecutor.class);

    @Autowired PrismContext prismContext;
    @Autowired RelationRegistry relationRegistry;
    @Autowired private SynchronousScriptExecutor synchronousScriptExecutor;
    @Autowired private AsynchronousScriptExecutor asynchronousScriptExecutor;
    @Autowired @Qualifier("cacheRepositoryService") RepositoryService repositoryService;
    @Autowired SecurityContextManager securityContextManager;
    @Autowired ModelObjectResolver modelObjectResolver;

    public <O extends ObjectType> void execute(@NotNull LensContext<O> context, Task task, OperationResult result)
            throws SchemaException {
        LensFocusContext<?> focusContext = context.getFocusContext();
        if (focusContext != null) {
            context.recomputeFocus(); // Maybe not needed but we want to be sure (when computing linked objects)
            for (EvaluatedPolicyRuleImpl rule : focusContext.getPolicyRules()) {
                executeRuleScriptingActions(rule, context, task, result);
            }
            DeltaSetTriple<EvaluatedAssignmentImpl<?>> triple = context.getEvaluatedAssignmentTriple();
            if (triple != null) {
                // We need to apply rules from all the assignments - even those that were deleted.
                for (EvaluatedAssignmentImpl<?> assignment : triple.getAllValues()) {
                    for (EvaluatedPolicyRuleImpl rule : assignment.getAllTargetsPolicyRules()) {
                        executeRuleScriptingActions(rule, context, task, result);
                    }
                }
            }
        }
    }

    private void executeRuleScriptingActions(EvaluatedPolicyRuleImpl rule, LensContext<?> context, Task task, OperationResult result) {
        if (rule.isTriggered()) {
            for (ScriptExecutionPolicyActionType action : rule.getEnabledActions(ScriptExecutionPolicyActionType.class)) {
                executeScriptingAction(action, rule, context, task, result);
            }
        }
    }

    private void executeScriptingAction(ScriptExecutionPolicyActionType action, EvaluatedPolicyRuleImpl rule,
            LensContext<?> context, Task task, OperationResult parentResult) {
        LOGGER.debug("Executing policy action scripts ({}) in action: {}\non rule:{}",
                action.getExecuteScript().size(), action, rule.debugDumpLazily());
        if (action.getAsynchronous() != null) {
            asynchronousScriptExecutor.execute(action, rule, context, task, parentResult);
        } else {
            synchronousScriptExecutor.execute(action, rule, context, task, parentResult);
        }
    }
}
