/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil.toConstraintsList;

@Component
public class PolicyConstraintsEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyConstraintsEvaluator.class);

    @Autowired private AssignmentModificationConstraintEvaluator assignmentConstraintEvaluator;
    @Autowired private HasAssignmentConstraintEvaluator hasAssignmentConstraintEvaluator;
    @Autowired private ExclusionConstraintEvaluator exclusionConstraintEvaluator;
    @Autowired private MultiplicityConstraintEvaluator multiplicityConstraintEvaluator;
    @Autowired private PolicySituationConstraintEvaluator policySituationConstraintEvaluator;
    @Autowired private CustomConstraintEvaluator customConstraintEvaluator;
    @Autowired private ObjectModificationConstraintEvaluator modificationConstraintEvaluator;
    @Autowired private StateConstraintEvaluator stateConstraintEvaluator;
    @Autowired private AlwaysTrueConstraintEvaluator alwaysTrueConstraintEvaluator;
    @Autowired private OrphanedConstraintEvaluator orphanedConstraintEvaluator;
    @Autowired private CompositeConstraintEvaluator compositeConstraintEvaluator;
    @Autowired private TransitionConstraintEvaluator transitionConstraintEvaluator;

    // returns non-empty list if the constraints evaluated to true (if allMustApply, all of the constraints must apply; otherwise, at least one must apply)
    @SuppressWarnings("unchecked")
    @NotNull <O extends ObjectType> List<EvaluatedPolicyRuleTrigger<?>> evaluateConstraints(
            PolicyConstraintsType constraints, boolean allMustApply, PolicyRuleEvaluationContext<O> ctx, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        if (constraints == null) {
            return Collections.emptyList();
        }
        List<EvaluatedPolicyRuleTrigger<?>> triggers = new ArrayList<>();
        for (JAXBElement<AbstractPolicyConstraintType> constraint : toConstraintsList(constraints, false, false)) {
            PolicyConstraintEvaluator<AbstractPolicyConstraintType, ?> evaluator =
                    (PolicyConstraintEvaluator<AbstractPolicyConstraintType, ?>) getConstraintEvaluator(constraint);
            Collection<? extends EvaluatedPolicyRuleTrigger<?>> newTriggers = evaluator.evaluate(constraint, ctx, result);
            LOGGER.trace("Evaluated policy rule triggers: {}", newTriggers);
            traceConstraintEvaluationResult(constraint, ctx, newTriggers);
            if (!newTriggers.isEmpty()) {
                triggers.addAll(newTriggers);
            } else {
                if (allMustApply) {
                    return List.of(); // constraint that does not apply => skip this rule
                }
            }
        }
        return triggers;
    }

    private <O extends ObjectType> void traceConstraintEvaluationResult(
            JAXBElement<AbstractPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<O> ctx,
            Collection<? extends EvaluatedPolicyRuleTrigger<?>> newTriggers) throws SchemaException {
        if (!LOGGER.isTraceEnabled()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n---[ POLICY CONSTRAINT ");
        if (!newTriggers.isEmpty()) {
            newTriggers.forEach(t -> sb.append("#"));
            sb.append(" ");
        }
        AbstractPolicyConstraintType constraint = constraintElement.getValue();
        if (constraint.getName() != null) {
            sb.append("'").append(constraint.getName()).append("'");
        }
        sb.append(" (").append(constraintElement.getName().getLocalPart()).append(")");
        sb.append(" for ");
        sb.append(ctx.getShortDescription());
        sb.append(" (").append(ctx.lensContext.getState()).append(")");
        sb.append("]---------------------------");
        sb.append("\nConstraint:\n");
        sb.append(PrismContext.get()
                .serializerFor(DebugUtil.getPrettyPrintBeansAs(PrismContext.LANG_XML))
                .serialize(constraintElement));
        //sb.append("\nContext: ").append(ctx.debugDump());
        sb.append("\nRule: ").append(ctx.policyRule.toShortString());
        sb.append("\nResult: ").append(DebugUtil.debugDump(newTriggers));
        LOGGER.trace("{}", sb);
    }

    private PolicyConstraintEvaluator<?, ?> getConstraintEvaluator(JAXBElement<AbstractPolicyConstraintType> constraint) {
        if (constraint.getValue() instanceof AssignmentModificationPolicyConstraintType) {
            return assignmentConstraintEvaluator;
        } else if (constraint.getValue() instanceof HasAssignmentPolicyConstraintType) {
            return hasAssignmentConstraintEvaluator;
        } else if (constraint.getValue() instanceof ExclusionPolicyConstraintType) {
            return exclusionConstraintEvaluator;
        } else if (constraint.getValue() instanceof MultiplicityPolicyConstraintType) {
            return multiplicityConstraintEvaluator;
        } else if (constraint.getValue() instanceof PolicySituationPolicyConstraintType) {
            return policySituationConstraintEvaluator;
        } else if (constraint.getValue() instanceof CustomPolicyConstraintType) {
            return customConstraintEvaluator;
        } else if (constraint.getValue() instanceof ModificationPolicyConstraintType) {
            return modificationConstraintEvaluator;
        } else if (constraint.getValue() instanceof StatePolicyConstraintType) {
            return stateConstraintEvaluator;
        } else if (constraint.getValue() instanceof PolicyConstraintsType) {
            return compositeConstraintEvaluator;
        } else if (constraint.getValue() instanceof TransitionPolicyConstraintType) {
            return transitionConstraintEvaluator;
        } else if (constraint.getValue() instanceof AlwaysTruePolicyConstraintType) {
            return alwaysTrueConstraintEvaluator;
        } else if (constraint.getValue() instanceof OrphanedPolicyConstraintType) {
            return orphanedConstraintEvaluator;
        } else {
            throw new IllegalArgumentException(
                    String.format("Unknown policy constraint: '%s' having value of %s",
                            constraint.getName(), constraint.getValue().getClass()));
        }
    }

}
