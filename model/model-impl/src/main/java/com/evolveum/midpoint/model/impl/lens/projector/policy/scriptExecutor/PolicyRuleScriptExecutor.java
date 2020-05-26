/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ValueListType;

/**
 * Executes scripts defined in scriptExecution policy action.
 * Designed to be called during FINAL stage, just like notification action.
 *
 * HIGHLY EXPERIMENTAL
 */
@Component
public class PolicyRuleScriptExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleScriptExecutor.class);

    private static final String OP_EXECUTE_SCRIPT = PolicyRuleScriptExecutor.class.getName() + ".executeScript";

    @Autowired PrismContext prismContext;
    @Autowired RelationRegistry relationRegistry;
    @Autowired private ScriptingExpressionEvaluator scriptingExpressionEvaluator;
    @Autowired @Qualifier("cacheRepositoryService") RepositoryService repositoryService;

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

    private void executeScriptingAction(ScriptExecutionPolicyActionType action, EvaluatedPolicyRuleImpl rule, LensContext<?> context, Task task, OperationResult parentResult) {
        LOGGER.debug("Executing policy action scripts ({}) in action: {}\non rule:{}",
                action.getExecuteScript().size(), action, rule.debugDumpLazily());
        List<ExecuteScriptType> executeScript = action.getExecuteScript();
        for (ExecuteScriptType executeScriptBean : executeScript) {
            executeScript(action, rule, context, task, parentResult, executeScriptBean);
        }
    }

    private void executeScript(ScriptExecutionPolicyActionType action, EvaluatedPolicyRuleImpl rule, LensContext<?> context,
            Task task, OperationResult parentResult, ExecuteScriptType specifiedExecuteScriptBean) {
        OperationResult result = parentResult.createSubresult(OP_EXECUTE_SCRIPT);
        try {
            ExecuteScriptType realExecuteScriptBean;
            if (specifiedExecuteScriptBean.getInput() == null && context.getFocusContext() != null) {
                ValueListType input = createScriptInput(action, rule, context, context.getFocusContext(), result);
                realExecuteScriptBean = specifiedExecuteScriptBean.clone().input(input);
            } else {
                realExecuteScriptBean = specifiedExecuteScriptBean;
            }
            VariablesMap initialVariables = createInitialVariables(action, rule, context);
            scriptingExpressionEvaluator.evaluateExpression(realExecuteScriptBean, initialVariables, false, task, result);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't execute script policy action: " + t.getMessage(), t);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute script with id={} in scriptExecution policy action '{}' (rule '{}'): {}",
                    t, action.getId(), action.getName(), rule.getName(), t.getMessage());
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private ValueListType createScriptInput(ScriptExecutionPolicyActionType action, EvaluatedPolicyRuleImpl rule,
            LensContext<?> context, LensFocusContext<?> focusContext, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        ScriptExecutionObjectType object = action.getObject();
        if (object == null) {
            return createInput(MiscUtil.singletonOrEmptyList(focusContext.getObjectAny()));
        } else {
            Map<String, PrismObject<?>> objectsMap = new HashMap<>(); // using OID-keyed map to avoid duplicates
            if (object.getCurrentObject() != null) {
                PrismObject<?> current = focusContext.getObjectAny();
                if (matches(current, object.getCurrentObject())) {
                    objectsMap.put(current.getOid(), current);
                }
            }
            if (!object.getLinkTarget().isEmpty()) {
                try (LinkTargetFinder targetFinder = new LinkTargetFinder(this, context, rule, result)) {
                    for (LinkTargetObjectSelectorType linkTargetSelector : object.getLinkTarget()) {
                        addObjects(objectsMap, targetFinder.getTargets(linkTargetSelector));
                    }
                }
            }
            if (!object.getLinkSource().isEmpty()) {
                try (LinkSourceFinder sourceFinder = new LinkSourceFinder(this, context, result)) {
                    addObjects(objectsMap, sourceFinder.getSources(object.getLinkSource()));
                }
            }
            return createInput(objectsMap.values());
        }
    }

    private boolean matches(PrismObject<?> object, ObjectSelectorType selector) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        //noinspection unchecked
        return repositoryService.selectorMatches(selector, (PrismObject) object, null, LOGGER, "current object");
    }

    private void addObjects(Map<String, PrismObject<?>> objectsMap, List<PrismObject<? extends ObjectType>> objects) {
        objects.forEach(o -> objectsMap.put(o.getOid(), o));
    }

    private ValueListType createInput(Collection<PrismObject<?>> objects) {
        ValueListType input = new ValueListType();
        objects.forEach(o -> input.getValue().add(o.getValue().clone()));
        return input;
    }

    private VariablesMap createInitialVariables(ScriptExecutionPolicyActionType action, EvaluatedPolicyRule rule,
            LensContext<?> context) {
        VariablesMap rv = new VariablesMap();
        rv.put(ExpressionConstants.VAR_POLICY_ACTION, action, ScriptExecutionPolicyActionType.class);
        rv.put(ExpressionConstants.VAR_POLICY_RULE, rule, EvaluatedPolicyRule.class);
        rv.put(ExpressionConstants.VAR_MODEL_CONTEXT, context, ModelContext.class);
        return rv;
    }
}
