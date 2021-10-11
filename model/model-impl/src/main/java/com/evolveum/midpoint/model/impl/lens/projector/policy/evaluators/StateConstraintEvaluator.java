/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.api.context.EvaluatedStateTrigger;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StatePolicyConstraintType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;

import java.util.*;

import static com.evolveum.midpoint.schema.constants.ExpressionConstants.VAR_OBJECT;
import static com.evolveum.midpoint.schema.constants.ExpressionConstants.VAR_RULE_EVALUATION_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType.ASSIGNMENT_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType.OBJECT_STATE;
import static java.util.Collections.emptyList;

@Component
public class StateConstraintEvaluator implements PolicyConstraintEvaluator<StatePolicyConstraintType> {

    private static final Trace LOGGER = TraceManager.getTrace(StateConstraintEvaluator.class);

    private static final String OP_EVALUATE = StateConstraintEvaluator.class.getName() + ".evaluate";

    private static final String OBJECT_CONSTRAINT_KEY_PREFIX = "objectState.";
    private static final String ASSIGNMENT_CONSTRAINT_KEY_PREFIX = "assignmentState.";
    private static final String KEY_NAMED = "named";
    private static final String KEY_UNNAMED = "unnamed";

    @Autowired private PrismContext prismContext;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired protected ExpressionFactory expressionFactory;
    @Autowired protected ConstraintEvaluatorHelper evaluatorHelper;
    @Autowired protected ScriptingExpressionEvaluator scriptingExpressionEvaluator;

    @Override
    public <AH extends AssignmentHolderType> EvaluatedStateTrigger evaluate(@NotNull JAXBElement<StatePolicyConstraintType> constraint,
            @NotNull PolicyRuleEvaluationContext<AH> rctx, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            if (QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_ASSIGNMENT_STATE)) {
                if (!(rctx instanceof AssignmentPolicyRuleEvaluationContext)) {
                    return null;            // assignment state can be evaluated only in the context of an assignment
                } else {
                    return evaluateForAssignment(constraint, (AssignmentPolicyRuleEvaluationContext<AH>) rctx, result);
                }
            } else if (QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_OBJECT_STATE)) {
                return evaluateForObject(constraint, rctx, result);
            } else {
                throw new AssertionError("unexpected state constraint " + constraint.getName());
            }
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private <AH extends AssignmentHolderType> EvaluatedStateTrigger evaluateForObject(JAXBElement<StatePolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<AH> ctx, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        StatePolicyConstraintType constraint = constraintElement.getValue();
        int count =
                (constraint.getFilter() != null ? 1 : 0)
                + (constraint.getExpression() != null ? 1 : 0)
                + (constraint.getMessageExpression() != null ? 1 : 0)
                + (constraint.getExecuteScript() != null ? 1 : 0);

        if (count != 1) {
            throw new SchemaException("Exactly one of filter, expression, messageExpression, executeScript element must be present.");
        }

        PrismObject<AH> object = ctx.getObject();
        if (object == null) {
            return null;
        }
        if (constraint.getFilter() != null) {
            ObjectFilter filter = prismContext.getQueryConverter().parseFilter(constraint.getFilter(), object.asObjectable().getClass());
            if (!filter.match(object.getValue(), matchingRuleRegistry)) {
                return null;
            }
        }
        if (constraint.getExecuteScript() != null) {
            VariablesMap variables = new VariablesMap();
            variables.put(VAR_OBJECT, object, object.getDefinition());
            variables.put(VAR_RULE_EVALUATION_CONTEXT, ctx, PolicyRuleEvaluationContext.class);
            ExecutionContext resultingContext;
            try {
                resultingContext = scriptingExpressionEvaluator.evaluateExpressionPrivileged(constraint.getExecuteScript(), variables, ctx.task, result);
            } catch (ScriptExecutionException e) {
                throw new SystemException(e);       // TODO
            }
            PipelineData output = resultingContext.getFinalOutput();
            LOGGER.trace("Scripting expression returned {} item(s); console output is:\n{}",
                    output != null ? output.getData().size() : null, resultingContext.getConsoleOutput());
            List<PipelineItem> items = output != null ? output.getData() : emptyList();
            if (items.isEmpty()) {
                return null;
            }
            // TODO retrieve localization messages from output
        }

        if (constraint.getMessageExpression() != null) {
            LocalizableMessageType messageBean = evaluatorHelper
                    .evaluateLocalizableMessageType(constraint.getMessageExpression(), evaluatorHelper.createExpressionVariables(ctx, constraintElement),
                            "message expression in object state constraint " + constraint.getName() + " (" + ctx.state + ")", ctx.task,
                            result);
            if (messageBean == null) {
                return null;
            } else {
                LocalizableMessage message = LocalizationUtil.toLocalizableMessage(messageBean);
                return new EvaluatedStateTrigger(OBJECT_STATE, constraint, message, message);
            }
        }

        if (constraint.getExpression() != null) {
            if (!evaluatorHelper.evaluateBoolean(constraint.getExpression(), evaluatorHelper.createExpressionVariables(ctx, constraintElement),
                    "expression in object state constraint " + constraint.getName() + " (" + ctx.state + ")", ctx.task, result)) {
                return null;
            }
        }

        return new EvaluatedStateTrigger(OBJECT_STATE, constraint,
                    createMessage(OBJECT_CONSTRAINT_KEY_PREFIX, constraintElement, ctx, false, result),
                    createShortMessage(OBJECT_CONSTRAINT_KEY_PREFIX, constraintElement, ctx, false, result));
    }

    private <AH extends AssignmentHolderType> EvaluatedStateTrigger evaluateForAssignment(JAXBElement<StatePolicyConstraintType> constraintElement,
            AssignmentPolicyRuleEvaluationContext<AH> ctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        StatePolicyConstraintType constraint = constraintElement.getValue();
        if (constraint.getFilter() != null) {
            throw new UnsupportedOperationException("Filter is not supported for assignment state constraints yet.");
        }
        if (constraint.getExpression() == null) {
            return null;        // shouldn't occur
        }
        if (!ctx.isApplicableToState()) {
            return null;
        }
        boolean match = evaluatorHelper.evaluateBoolean(constraint.getExpression(), evaluatorHelper.createExpressionVariables(ctx, constraintElement),
                "expression in assignment state constraint " + constraint.getName() + " (" + ctx.state + ")", ctx.task, result);
        if (match) {
            return new EvaluatedStateTrigger(ASSIGNMENT_STATE, constraint,
                    createMessage(ASSIGNMENT_CONSTRAINT_KEY_PREFIX, constraintElement, ctx, true, result),
                    createShortMessage(ASSIGNMENT_CONSTRAINT_KEY_PREFIX, constraintElement, ctx, true, result));
        }
        return null;
    }

    @NotNull
    private <AH extends AssignmentHolderType> LocalizableMessage createMessage(String constraintKeyPrefix,
            JAXBElement<StatePolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<AH> ctx, boolean assignmentTarget, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = createBuiltInMessage(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + constraintKeyPrefix, constraintElement, ctx, assignmentTarget, result);
        return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
    }

    @NotNull
    private <AH extends AssignmentHolderType> LocalizableMessage createBuiltInMessage(String keyPrefix,
            JAXBElement<StatePolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<AH> ctx,
            boolean assignmentTarget, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        StatePolicyConstraintType constraint = constraintElement.getValue();
        List<Object> args = new ArrayList<>();
        args.add(evaluatorHelper.createBeforeAfterMessage(ctx));
        if (assignmentTarget) {
            addAssignmentTargetArgument(args, ctx);
        }
        String keySuffix;
        if (constraint.getName() != null) {
            args.add(constraint.getName());
            keySuffix = KEY_NAMED;
        } else {
            keySuffix = KEY_UNNAMED;
        }
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(keyPrefix + keySuffix)
                .args(args)
                .build();
        return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
    }

    private <AH extends AssignmentHolderType> void addAssignmentTargetArgument(List<Object> args, PolicyRuleEvaluationContext<AH> ctx) {
        if (!(ctx instanceof AssignmentPolicyRuleEvaluationContext)) {
            args.add("");
        } else {
            AssignmentPolicyRuleEvaluationContext<AH> actx = (AssignmentPolicyRuleEvaluationContext<AH>) ctx;
            args.add(ObjectTypeUtil.createDisplayInformation(actx.evaluatedAssignment.getTarget(), false));
        }
    }

    @NotNull
    private <AH extends AssignmentHolderType> LocalizableMessage createShortMessage(String constraintKeyPrefix,
            JAXBElement<StatePolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<AH> ctx, boolean assignmentTarget, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = createBuiltInMessage(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + constraintKeyPrefix, constraintElement, ctx, assignmentTarget, result);
        return evaluatorHelper.createLocalizableShortMessage(constraintElement, ctx, builtInMessage, result);
    }
}
