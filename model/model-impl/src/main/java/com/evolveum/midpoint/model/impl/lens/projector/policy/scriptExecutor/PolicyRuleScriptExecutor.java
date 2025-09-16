/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.util.ReferenceResolver;
import com.evolveum.midpoint.model.common.LinkManager;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.scripting.BulkActionsExecutor;
import com.evolveum.midpoint.model.impl.security.RunAsRunner;
import com.evolveum.midpoint.model.impl.security.RunAsRunnerFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.SecurityContextManager;

import com.evolveum.midpoint.util.exception.CommonException;

import com.evolveum.midpoint.util.logging.LoggingUtils;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.prism.PrismContext;
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
    @Autowired ReferenceResolver referenceResolver;
    @Autowired ExpressionFactory expressionFactory;
    @Autowired BulkActionsExecutor bulkActionsExecutor;
    @Autowired RunAsRunnerFactory runAsRunnerFactory;
    @Autowired LinkManager linkManager;

    public void execute(@NotNull LensContext<?> context, Task task, OperationResult parentResult)
            throws SchemaException {
        if (context.hasFocusContext()) {
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

        for (EvaluatedPolicyRuleImpl rule : context.getTriggeredObjectPolicyRules()) {
            collectRule(rules, rule);
        }

        return rules;
    }

    private void collectRule(List<EvaluatedPolicyRuleImpl> rules, EvaluatedPolicyRuleImpl rule) {
        if (rule.isTriggered() && rule.containsEnabledAction(ScriptExecutionPolicyActionType.class)) {
            rules.add(rule);
        }
    }

    private <O extends ObjectType> void executeScriptsFromCollectedRules(
            List<EvaluatedPolicyRuleImpl> rules,
            LensContext<O> context,
            Task task,
            OperationResult parentResult) {

        // Must not be minor because of background OID information.
        OperationResult result = parentResult.createSubresult(OP_EXECUTE_SCRIPTS_FROM_RULES);
        try (RunAsRunner runAsRunner = runAsRunnerFactory.runner()) {
            for (EvaluatedPolicyRuleImpl rule : rules) {
                var enabledActions = rule.getEnabledActions(ScriptExecutionPolicyActionType.class);
                LOGGER.trace("Rule {} has {} enabled script execution actions", rule, enabledActions.size());
                for (var action : enabledActions) {
                    ActionContext actx = new ActionContext(action, rule, context, task, this);
                    try {
                        // We should consider ordering actions to be executed by runAsRef to avoid unnecessary context switches.
                        runAsRunner.runAs(
                                () -> executeScriptingAction(actx, result),
                                actx.action.getRunAsRef(), // FIXME privileges!
                                result);
                    } catch (CommonException e) {
                        LoggingUtils.logUnexpectedException(
                                LOGGER, "Couldn't execute scripting action - continuing with others (if present)", e);
                    }
                }
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
            // This is really ugly hack (MID-6753). The operation result for the whole clockwork processing should not be
            // FATAL_ERROR just because of scripts execution failure. On the other hand, this particular operation failed
            // fatally. So, in theory, this fatal->partial switch should be done at the level of parent operation
            // i.e. clockwork click. The traditional way of doing this is treating that operation result as "composite".
            // However, we intentionally do not do it in that way, because it would change the whole error handling
            // as we are used to. So this hack is definitely the lesser evil for now.
            if (result.getStatus() == OperationResultStatus.FATAL_ERROR) {
                result.setStatus(OperationResultStatus.PARTIAL_ERROR);
            }
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
