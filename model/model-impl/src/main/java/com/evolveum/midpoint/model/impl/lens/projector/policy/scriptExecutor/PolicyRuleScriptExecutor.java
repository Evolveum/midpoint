/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.security.api.SecurityContextManager;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Executes scripts defined in scriptExecution policy action.
 * Designed to be called during FINAL stage, just like notification action.
 *
 * TODO We should merge equivalent policy rules so that e.g. recomputation is not done multiple times.
 */
@Component
public class PolicyRuleScriptExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleScriptExecutor.class);

    private static final String OP_EXECUTE_SCRIPTS_FROM_RULES = PolicyRuleScriptExecutor.class + ".executeScriptsFromRules";

    @Autowired PrismContext prismContext;
    @Autowired RelationRegistry relationRegistry;
    @Autowired @Qualifier("cacheRepositoryService") RepositoryService repositoryService;
    @Autowired ModelService modelService;
    @Autowired SecurityContextManager securityContextManager;
    @Autowired ModelObjectResolver modelObjectResolver;
    @Autowired ExpressionFactory expressionFactory;
    @Autowired ScriptingExpressionEvaluator scriptingExpressionEvaluator;

    public void execute(@NotNull LensContext<?> context, Task task, OperationResult parentResult)
            throws SchemaException {
        if (context.hasFocusContext()) {
            context.recomputeFocus(); // Maybe not needed but we want to be sure (when computing linked objects)
            List<EvaluatedPolicyRuleImpl> rules = collectRelevantPolicyRules(context);
            if (!rules.isEmpty()) {
                executeScriptsFromCollectedRules(rules, context, task, parentResult);
            } else {
                LOGGER.trace("No relevant policy rules found");
            }
        } else {
            LOGGER.trace("No focus context, no 'scriptExecution' policy actions");
        }
    }

    private List<EvaluatedPolicyRuleImpl> collectRelevantPolicyRules(LensContext<?> context) {
        List<EvaluatedPolicyRuleImpl> rules = new ArrayList<>();
        collectFromFocus(rules, context);
        collectFromAssignments(rules, context);
        return rules;
    }

    private void collectFromFocus(List<EvaluatedPolicyRuleImpl> rules, LensContext<?> context) {
        for (EvaluatedPolicyRuleImpl rule : context.getFocusContext().getPolicyRules()) {
            collectRule(rules, rule);
        }
    }

    private <O extends ObjectType> void collectFromAssignments(List<EvaluatedPolicyRuleImpl> rules, LensContext<O> context) {
        DeltaSetTriple<EvaluatedAssignmentImpl<?>> triple = context.getEvaluatedAssignmentTriple();
        if (triple != null) {
            // We need to apply rules from all the assignments - even those that were deleted.
            for (EvaluatedAssignmentImpl<?> assignment : triple.getAllValues()) {
                for (EvaluatedPolicyRuleImpl rule : assignment.getAllTargetsPolicyRules()) {
                    collectRule(rules, rule);
                }
            }
        }
    }

    private void collectRule(List<EvaluatedPolicyRuleImpl> rules, EvaluatedPolicyRuleImpl rule) {
        if (rule.isTriggered() && rule.containsEnabledAction(ScriptExecutionPolicyActionType.class)) {
            rules.add(rule);
        }
    }

    private <O extends ObjectType> void executeScriptsFromCollectedRules(List<EvaluatedPolicyRuleImpl> rules,
            LensContext<O> context, Task task, OperationResult parentResult) {

        // Must not be minor because of background OID information.
        OperationResult result = parentResult.createSubresult(OP_EXECUTE_SCRIPTS_FROM_RULES);
        try {
            for (EvaluatedPolicyRuleImpl rule : rules) {
                List<ScriptExecutionPolicyActionType> enabledActions = rule.getEnabledActions(ScriptExecutionPolicyActionType.class);
                LOGGER.trace("Rule {} has {} enabled script execution actions", rule, enabledActions.size());
                for (ScriptExecutionPolicyActionType action : enabledActions) {
                    ActionContext actx = new ActionContext(action, rule, context, task, this);
                    executeScriptingAction(actx, result);
                }
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void executeScriptingAction(ActionContext actx, OperationResult parentResult) {
        LOGGER.debug("Executing policy action scripts ({}) in action: {}\non rule:{}",
                actx.action.getExecuteScript().size(), actx.action, actx.rule.debugDumpLazily());
        if (actx.action.getAsynchronousExecution() != null) {
            new AsynchronousScriptExecutor(actx).submitScripts(parentResult);
        } else {
            new SynchronousScriptExecutor(actx).executeScripts(parentResult);
        }
    }
}
