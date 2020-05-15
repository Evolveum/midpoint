/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.policy;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
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

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired private ModelService modelService;
    @Autowired private PrismContext prismContext;
    @Autowired private ScriptingExpressionEvaluator scriptingExpressionEvaluator;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    public <O extends ObjectType> void execute(@NotNull LensContext<O> context, Task task, OperationResult result) {
        LensFocusContext<?> focusContext = context.getFocusContext();
        if (focusContext != null) {
            for (EvaluatedPolicyRule rule : focusContext.getPolicyRules()) {
                executeRuleScriptingActions(rule, context, task, result);
            }
            DeltaSetTriple<EvaluatedAssignmentImpl<?>> triple = context.getEvaluatedAssignmentTriple();
            if (triple != null) {
                // We need to apply rules from all the assignments - even those that were deleted.
                for (EvaluatedAssignment<?> assignment : triple.getAllValues()) {
                    for (EvaluatedPolicyRule rule : assignment.getAllTargetsPolicyRules()) {
                        executeRuleScriptingActions(rule, context, task, result);
                    }
                }
            }
        }
    }

    private void executeRuleScriptingActions(EvaluatedPolicyRule rule, LensContext<?> context, Task task, OperationResult result) {
        if (rule.isTriggered()) {
            for (ScriptExecutionPolicyActionType action : rule.getEnabledActions(ScriptExecutionPolicyActionType.class)) {
                executeScriptingAction(action, rule, context, task, result);
            }
        }
    }

    private void executeScriptingAction(ScriptExecutionPolicyActionType action, EvaluatedPolicyRule rule, LensContext<?> context, Task task, OperationResult parentResult) {
        LOGGER.debug("Executing policy action scripts ({}) in action: {}\non rule:{}",
                action.getExecuteScript().size(), action, rule.debugDumpLazily());
        List<ExecuteScriptType> executeScript = action.getExecuteScript();
        for (ExecuteScriptType executeScriptBean : executeScript) {
            executeScript(action, rule, context, task, parentResult, executeScriptBean);
        }
    }

    private void executeScript(ScriptExecutionPolicyActionType action, EvaluatedPolicyRule rule, LensContext<?> context,
            Task task, OperationResult parentResult, ExecuteScriptType specifiedExecuteScriptBean) {
        OperationResult result = parentResult.createSubresult(OP_EXECUTE_SCRIPT);
        try {
            ExecuteScriptType realExecuteScriptBean;
            if (specifiedExecuteScriptBean.getInput() == null && context.getFocusContext() != null) {
                ValueListType input = createScriptInput(action, rule, context, context.getFocusContext(), task, result);
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

    private ValueListType createScriptInput(ScriptExecutionPolicyActionType action, EvaluatedPolicyRule rule,
            LensContext<?> context, LensFocusContext<?> focusContext, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        ScriptExecutionObjectType object = action.getObject();
        if (object == null) {
            return createInput(MiscUtil.singletonOrEmptyList(focusContext.getObjectAny()));
        } else {
            Map<String, PrismObject<?>> objectsMap = new HashMap<>(); // use OID-keyed map to avoid duplicates
            if (!object.getAssigned().isEmpty()) {
                List<PrismObject<?>> assigned = getAssigned(context);
                LOGGER.trace("Assigned objects (all): {}", assigned);
                List<PrismObject<?>> filtered = filterObjects(assigned, object.getAssigned());
                LOGGER.trace("Assigned objects (filtered on selectors): {}", filtered);
                addObjects(objectsMap, filtered);
            }
            if (!object.getAssignedMatchingPolicyConstraints().isEmpty()) {
                List<PrismObject<?>> assignedMatchingConstraints = getAssignedMatchingConstraints(rule);
                LOGGER.trace("Assigned objects matching policy constraints (all): {}", assignedMatchingConstraints);
                List<PrismObject<?>> filtered = filterObjects(assignedMatchingConstraints, object.getAssignedMatchingPolicyConstraints());
                LOGGER.trace("Assigned objects matching policy constraints (filtered on selectors): {}", filtered);
                addObjects(objectsMap, filtered);
            }
            if (!object.getAssignedOnPath().isEmpty()) {
                List<PrismObject<?>> assignedOnPath = getAssignedOnPath(rule);
                LOGGER.trace("Assigned objects on respective assignment path (all): {}", assignedOnPath);
                List<PrismObject<?>> filtered = filterObjects(assignedOnPath, object.getAssignedOnPath());
                LOGGER.trace("Assigned objects on respective assignment path (filtered on selectors): {}", filtered);
                addObjects(objectsMap, filtered);
            }
            if (!object.getAssignee().isEmpty()) {
                List<PrismObject<?>> assignees = getAssignees(focusContext.getOid(), task, result);
                LOGGER.trace("Assignee objects (all): {}", assignees);
                List<PrismObject<?>> filtered = filterObjects(assignees, object.getAssignee());
                LOGGER.trace("Assignee objects (filtered on selectors): {}", filtered);
                addObjects(objectsMap, filtered);
            }
            return createInput(objectsMap.values());
        }
    }

    private List<PrismObject<?>> getAssignees(String focusOid, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        if (focusOid == null) {
            LOGGER.warn("No focus object OID, no assignees can be found");
            return Collections.emptyList();
        } else {
            ObjectQuery query = prismContext.queryFor(AssignmentHolderType.class)
                    .item(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF).ref(focusOid)
                    .build();
            //noinspection unchecked
            return (List) modelService.searchObjects(AssignmentHolderType.class, query, null, task, result);
        }
    }

    private void addObjects(Map<String, PrismObject<?>> objectsMap, List<PrismObject<?>> objects) {
        objects.forEach(o -> objectsMap.put(o.getOid(), o));
    }

    private List<PrismObject<?>> filterObjects(List<PrismObject<?>> objects, List<ObjectSelectorType> selectors)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        List<PrismObject<?>> all = new ArrayList<>();
        for (ObjectSelectorType selector : selectors) {
            all.addAll(filterObjects(objects, selector));
        }
        return all;
    }

    private List<PrismObject<?>> filterObjects(List<PrismObject<?>> objects, ObjectSelectorType selector)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        List<PrismObject<?>> matching = new ArrayList<>();
        for (PrismObject<?> object : objects) {
            //noinspection unchecked
            if (repositoryService.selectorMatches(selector, (PrismObject<? extends ObjectType>) object,
                    null, LOGGER, "script object evaluation")) {
                matching.add(object);
            }
        }
        return matching;
    }

    private List<PrismObject<?>> getAssignedOnPath(EvaluatedPolicyRule rule) {
        AssignmentPath assignmentPath = rule.getAssignmentPath();
        if (assignmentPath != null) {
            return assignmentPath.getFirstOrderChain().stream()
                    .map(ObjectType::asPrismObject)
                    .collect(Collectors.toList());
        } else {
            LOGGER.warn("No assignment path for {} (but assignedOnPath object specification is present)", rule);
            return Collections.emptyList();
        }
    }

    private List<PrismObject<?>> getAssignedMatchingConstraints(EvaluatedPolicyRule rule) {
        List<PrismObject<?>> rv = new ArrayList<>();
        addFromTriggers(rv, rule.getAllTriggers());
        return rv;
    }

    private void addFromTriggers(List<PrismObject<?>> rv, @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
            rv.addAll(trigger.getTargetObjects());
            addFromTriggers(rv, trigger.getInnerTriggers());
        }
    }

    private List<PrismObject<?>> getAssigned(LensContext<?> context) {
        List<PrismObject<?>> rv = new ArrayList<>();
        for (EvaluatedAssignmentImpl<?> evaluatedAssignment : context.getEvaluatedAssignmentTriple().getAllValues()) {
            CollectionUtils.addIgnoreNull(rv, evaluatedAssignment.getTarget());
        }
        return rv;
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
