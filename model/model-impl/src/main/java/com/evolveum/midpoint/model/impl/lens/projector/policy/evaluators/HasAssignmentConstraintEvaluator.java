/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import com.evolveum.midpoint.model.api.context.EvaluatedHasAssignmentTrigger;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentTargetImpl;
import com.evolveum.midpoint.model.impl.lens.projector.AssignmentOrigin;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectState;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptySet;

@Component
public class HasAssignmentConstraintEvaluator implements PolicyConstraintEvaluator<HasAssignmentPolicyConstraintType> {

    private static final String OP_EVALUATE = HasAssignmentConstraintEvaluator.class.getName() + ".evaluate";

    private static final String CONSTRAINT_KEY_POSITIVE = "hasAssignment";
    private static final String CONSTRAINT_KEY_NEGATIVE = "hasNoAssignment";

    @Autowired private ConstraintEvaluatorHelper evaluatorHelper;
    @Autowired private PrismContext prismContext;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;

    @Override
    public <AH extends AssignmentHolderType> EvaluatedHasAssignmentTrigger evaluate(@NotNull JAXBElement<HasAssignmentPolicyConstraintType> constraintElement,
            @NotNull PolicyRuleEvaluationContext<AH> ctx, OperationResult parentResult) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            boolean shouldExist = QNameUtil.match(constraintElement.getName(), PolicyConstraintsType.F_HAS_ASSIGNMENT);
            HasAssignmentPolicyConstraintType constraint = constraintElement.getValue();
            if (constraint.getTargetRef() == null) {
                throw new SchemaException("No targetRef in hasAssignment constraint");
            }

            DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = ctx.lensContext.getEvaluatedAssignmentTriple();
            if (evaluatedAssignmentTriple == null) {
                return createTriggerIfShouldNotExist(shouldExist, constraintElement, ctx, result);
            }
            boolean allowMinus = ctx.state == ObjectState.BEFORE;
            boolean allowZero = true;
            boolean allowPlus = ctx.state == ObjectState.AFTER;
            boolean allowDirect = !Boolean.FALSE.equals(constraint.isDirect());
            boolean allowIndirect = !Boolean.TRUE.equals(constraint.isDirect());
            boolean allowEnabled = !Boolean.FALSE.equals(constraint.isEnabled());
            boolean allowDisabled = !Boolean.TRUE.equals(constraint.isEnabled());

            List<PrismObject<?>> matchingTargets = new ArrayList<>();
            for (EvaluatedAssignmentImpl<?> evaluatedAssignment : evaluatedAssignmentTriple.getNonNegativeValues()) {
                AssignmentOrigin origin = evaluatedAssignment.getOrigin();
                boolean assignmentIsAdded = origin.isBeingAdded();
                boolean assignmentIsDeleted = origin.isBeingDeleted();
                boolean assignmentIsModified = origin.isBeingModified();
                DeltaSetTriple<EvaluatedAssignmentTargetImpl> targetsTriple = evaluatedAssignment.getRoles();
                for (EvaluatedAssignmentTargetImpl target : targetsTriple.getNonNegativeValues()) {
                    if (!target.appliesToFocus()) {
                        continue;
                    }
                    if (!(allowDirect && target.isDirectlyAssigned() || allowIndirect && !target.isDirectlyAssigned())) {
                        continue;
                    }
                    if (!(allowEnabled && target.isValid() || allowDisabled && !target.isValid())) {
                        continue;
                    }
                    if (!relationMatches(constraint.getTargetRef().getRelation(), constraint.getRelation(),
                            target.getAssignment())) {
                        continue;
                    }
                    boolean targetIsInPlusSet = targetsTriple.presentInPlusSet(target);
                    boolean targetIsInZeroSet = targetsTriple.presentInZeroSet(target);
                    boolean targetIsInMinusSet = targetsTriple.presentInMinusSet(target);
                    // TODO check these computations
                    boolean isPlus = assignmentIsAdded || assignmentIsModified && targetIsInPlusSet;
                    boolean isZero = assignmentIsModified && targetIsInZeroSet;
                    boolean isMinus = assignmentIsDeleted || assignmentIsModified && targetIsInMinusSet;
                    if (!(allowPlus && isPlus || allowZero && isZero || allowMinus && isMinus)) {
                        continue;
                    }
                    if (ExclusionConstraintEvaluator
                            .oidMatches(constraint.getTargetRef(), target, prismContext, matchingRuleRegistry,
                                    "hasAssignment constraint")) {
                        // TODO more specific trigger, containing information on matching assignment; see ExclusionConstraintEvaluator
                        matchingTargets.add(target.getTarget());
                    }
                }
            }
            if (!matchingTargets.isEmpty()) {
                if (shouldExist) {
                    PrismObject<?> anyTargetObject = matchingTargets.get(0);
                    return new EvaluatedHasAssignmentTrigger(PolicyConstraintKindType.HAS_ASSIGNMENT, constraint, matchingTargets,
                            createPositiveMessage(constraintElement, ctx, anyTargetObject, result),
                            createPositiveShortMessage(constraintElement, ctx, anyTargetObject, result));
                } else {
                    // we matched something but the constraint was "has no assignment"
                    return null;
                }
            } else {
                return createTriggerIfShouldNotExist(shouldExist, constraintElement, ctx, result);
            }
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private <AH extends AssignmentHolderType> LocalizableMessage createPositiveMessage(JAXBElement<HasAssignmentPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<AH> ctx, PrismObject<?> target, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY_POSITIVE)
                .arg(ObjectTypeUtil.createDisplayInformation(target, false))
                .arg(evaluatorHelper.createBeforeAfterMessage(ctx))
                .build();
        return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
    }

    private <AH extends AssignmentHolderType> LocalizableMessage createPositiveShortMessage(JAXBElement<HasAssignmentPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<AH> ctx, PrismObject<?> target, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY_POSITIVE)
                .arg(ObjectTypeUtil.createDisplayInformation(target, false))
                .arg(evaluatorHelper.createBeforeAfterMessage(ctx))
                .build();
        return evaluatorHelper.createLocalizableShortMessage(constraintElement, ctx, builtInMessage, result);
    }

    private <AH extends AssignmentHolderType> LocalizableMessage createNegativeMessage(JAXBElement<HasAssignmentPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<AH> ctx, QName targetType, String targetOid, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY_NEGATIVE)
                .arg(ObjectTypeUtil.createTypeDisplayInformation(targetType, false))
                .arg(targetOid)
                .arg(evaluatorHelper.createBeforeAfterMessage(ctx))
                .build();
        return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
    }

    private <AH extends AssignmentHolderType> LocalizableMessage createNegativeShortMessage(JAXBElement<HasAssignmentPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<AH> ctx, QName targetType, String targetOid, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY_NEGATIVE)
                .arg(ObjectTypeUtil.createTypeDisplayInformation(targetType, false))
                .arg(targetOid)
                .arg(evaluatorHelper.createBeforeAfterMessage(ctx))
                .build();
        return evaluatorHelper.createLocalizableShortMessage(constraintElement, ctx, builtInMessage, result);
    }

    private EvaluatedHasAssignmentTrigger createTriggerIfShouldNotExist(boolean shouldExist,
            JAXBElement<HasAssignmentPolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<?> ctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        HasAssignmentPolicyConstraintType constraint = constraintElement.getValue();
        if (shouldExist) {
            return null;
        } else {
            return new EvaluatedHasAssignmentTrigger(PolicyConstraintKindType.HAS_NO_ASSIGNMENT, constraint, emptySet(),
                    createNegativeMessage(constraintElement, ctx, constraint.getTargetRef().getType(), constraint.getTargetRef().getOid(), result),
                    createNegativeShortMessage(constraintElement, ctx, constraint.getTargetRef().getType(), constraint.getTargetRef().getOid(), result));
            // targetName seems to be always null, even if specified in the policy rule
        }
    }

    private boolean relationMatches(QName primaryRelationToMatch, List<QName> secondaryRelationsToMatch, AssignmentType assignment) {
        if (assignment == null || assignment.getTargetRef() == null) {
            return false;           // shouldn't occur
        }
        List<QName> relationsToMatch = primaryRelationToMatch != null
                ? Collections.singletonList(primaryRelationToMatch)
                : new ArrayList<>(secondaryRelationsToMatch);
        if (relationsToMatch.isEmpty()) {
            relationsToMatch.add(prismContext.getDefaultRelation());
        }
        return prismContext.relationMatches(relationsToMatch, assignment.getTargetRef().getRelation());
    }
}
