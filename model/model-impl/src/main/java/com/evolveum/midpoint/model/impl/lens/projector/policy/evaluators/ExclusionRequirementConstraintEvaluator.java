/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentTargetImpl;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Evaluates exclusion and requirement policy constraints.
 */
@Component
public class ExclusionRequirementConstraintEvaluator
        implements PolicyConstraintEvaluator<ExclusionPolicyConstraintType, EvaluatedExclusionRequirementTrigger> {

    private static final Trace LOGGER = TraceManager.getTrace(ExclusionRequirementConstraintEvaluator.class);

    private static final String OP_EVALUATE = ExclusionRequirementConstraintEvaluator.class.getName() + ".evaluate";

    private static final String CONSTRAINT_KEY_EXCLUSION = "exclusion";
    private static final String CONSTRAINT_KEY_REQUIREMENT = "requirement";

    @Autowired private ConstraintEvaluatorHelper evaluatorHelper;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private ModelObjectResolver objectResolver;
    @Autowired private ArchetypeManager archetypeManager;

    @Override
    public @NotNull <O extends ObjectType> Collection<EvaluatedExclusionRequirementTrigger> evaluate(
            @NotNull JAXBElement<ExclusionPolicyConstraintType> constraint,
            @NotNull PolicyRuleEvaluationContext<O> rctx,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();

        boolean isExclusion = QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_EXCLUSION);

        try {
            LOGGER.trace("Evaluating {} constraint {} on {}",
                    isExclusion ? "exclusion" : "requirement",
                    lazy(() -> PolicyRuleTypeUtil.toShortString(constraint)), rctx);
            if (!(rctx instanceof AssignmentPolicyRuleEvaluationContext)) {
                return List.of();
            }

            AssignmentPolicyRuleEvaluationContext<?> ctx = (AssignmentPolicyRuleEvaluationContext<?>) rctx;
            if (!ctx.isAdded && !ctx.isKept) {
                LOGGER.trace("Assignment not being added nor kept, skipping evaluation.");
                return List.of();
            }

            if (sourceOrderConstraintsDoNotMatch(constraint, ctx)) {
                // logged in the called method body
                return List.of();
            }

            /*
             * Now let us check the exclusions/requirements.
             *
             * Assignment A is the current evaluated assignment. It has directly or indirectly attached the exclusion/requirement policy rule.
             * We now go through all other assignments B and check the exclusions/requirements.
             */

            List<OrderConstraintsType> targetOrderConstraints = defaultIfEmpty(constraint.getValue().getTargetOrderConstraint());
            List<EvaluatedAssignmentTargetImpl> nonNegativeTargetsA = ctx.evaluatedAssignment.getNonNegativeTargets();
            ConstraintReferenceMatcher<?> refMatcher = new ConstraintReferenceMatcher<>(
                    ctx, constraint.getValue().getTargetRef(), constraint.getValue().getTargetArchetypeRef(), expressionFactory, archetypeManager, result, LOGGER);

            boolean requirementMet = false;
            List<EvaluatedExclusionRequirementTrigger> triggers = new ArrayList<>();
            targetA:
            for (EvaluatedAssignmentImpl<?> assignmentB : ctx.evaluatedAssignmentTriple.getNonNegativeValues()) { // MID-6403
                if (assignmentB == ctx.evaluatedAssignment) { // currently there is no other way of comparing the evaluated assignments
                    continue;
                }
                targetB:
                for (EvaluatedAssignmentTargetImpl targetB : assignmentB.getNonNegativeTargets()) {
                    if (!pathMatches(targetB.getAssignmentPath(), targetOrderConstraints)) {
                        LOGGER.trace("Skipping considering exclusion/requirement target {} because it does not match target path constraints."
                                + " Path={}, constraints={}", targetB, targetB.getAssignmentPath(), targetOrderConstraints);
                        continue;
                    }
                    if (!refMatcher.refMatchesTarget(targetB.getTarget(), "exclusion/requirement constraint")) {
                        LOGGER.trace("Target {} OID does not match exclusion/requirement filter", targetB);
                        continue;
                    }
                    // To avoid false positives let us check if this target is not already covered by assignment being evaluated
                    for (EvaluatedAssignmentTargetImpl targetA : nonNegativeTargetsA) {
                        if (targetIsAlreadyCovered(targetB, targetA)) {
                            continue targetB;
                        }
                    }

                    // We have a match.

                    if (isExclusion) {
                        // If this is exclusion, we should report it, by creating a trigger.
                        triggers.add(
                                createExclusionTrigger(ctx.evaluatedAssignment, assignmentB, targetB, constraint, ctx, result));
                    } else {
                        // If this is requirement, just mark that it is met and we are done.
                        requirementMet = true;
                        break targetA;
                    }
                }
            }

            if (!isExclusion && !requirementMet) {
                triggers.add(
                        createRequirementTrigger(ctx.evaluatedAssignment, constraint, ctx, result));
            }

            result.addArbitraryObjectCollectionAsReturn(
                    "trigger",
                    triggers.stream()
                            .map(EvaluatedExclusionRequirementTrigger::toDiagShortcut)
                            .collect(Collectors.toList()));

            return triggers;
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private boolean targetIsAlreadyCovered(EvaluatedAssignmentTargetImpl targetB, EvaluatedAssignmentTargetImpl targetA) {
        if (!targetA.appliesToFocus()) {
            return false;
        }
        if (targetA.getOid() == null || !targetA.getOid().equals(targetB.getOid())) {
            return false;
        }
        EvaluationOrder orderA = targetA.getAssignmentPath().last().getEvaluationOrder();
        EvaluationOrder orderB = targetB.getAssignmentPath().last().getEvaluationOrder();
        if (ordersAreCompatible(orderA, orderB)) {
            LOGGER.trace("Target {} is covered by the assignment considered, skipping", targetB);
            return true;
        } else {
            LOGGER.trace("Target B of {} is covered by assignment A considered, but with different order (A={} vs B={})."
                    + " Not skipping.", targetB, orderA, orderB);
            return false;
        }
    }

    private boolean ordersAreCompatible(EvaluationOrder orderA, EvaluationOrder orderB) {
        Map<QName, Integer> diff = orderA.diff(orderB);
        for (Map.Entry<QName, Integer> diffEntry : diff.entrySet()) {
            QName relation = diffEntry.getKey();
            if (relationRegistry.isDelegation(relation)) {
                continue;
            }
            if (diffEntry.getValue() == 0) {
                continue;
            }
            LOGGER.trace("Difference in relation: {}", diffEntry);
            return false;
        }
        return true;
    }

    private <AH extends AssignmentHolderType> boolean sourceOrderConstraintsDoNotMatch(
            @NotNull JAXBElement<ExclusionPolicyConstraintType> constraint, AssignmentPolicyRuleEvaluationContext<AH> ctx) {
        List<OrderConstraintsType> sourceOrderConstraints = defaultIfEmpty(constraint.getValue().getOrderConstraint());
        AssignmentPath assignmentPath = ctx.policyRule.getAssignmentPathRequired();
        if (ctx.policyRule.isGlobal()) {
            if (!pathMatches(assignmentPath, sourceOrderConstraints)) {
                LOGGER.trace("Assignment path to the global policy rule does not match source order constraints,"
                                + " not triggering. Path={}, source order constraints={}",
                        assignmentPath, sourceOrderConstraints);
                return true;
            }
        } else {
            // It is not clear how to match orderConstraint with assignment path of the constraint.
            // Let us try the following test: we consider it matching if there's at least one segment
            // on the path that matches the constraint.
            boolean found = assignmentPath.getSegments().stream()
                    .anyMatch(segment -> segment.matches(sourceOrderConstraints));
            if (!found) {
                LOGGER.trace("No segment in assignment path to the assigned policy rule does not match source order "
                                + "constraints, not triggering. Whole path={}, constraints={}", assignmentPath,
                        sourceOrderConstraints);
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean pathMatches(@NotNull AssignmentPath assignmentPath, List<OrderConstraintsType> definedOrderConstraints) {
        if (assignmentPath.isEmpty()) {
            throw new IllegalStateException("Assignment path is empty.");
        }
        return assignmentPath.matches(definedOrderConstraints);
    }

    @NotNull
    private List<OrderConstraintsType> defaultIfEmpty(List<OrderConstraintsType> definedOrderConstraints) {
        return !definedOrderConstraints.isEmpty() ? definedOrderConstraints : defaultOrderConstraints();
    }

    private List<OrderConstraintsType> defaultOrderConstraints() {
        return Collections.singletonList(new OrderConstraintsType().order(1));
    }

    private EvaluatedExclusionRequirementTrigger createExclusionTrigger(
            EvaluatedAssignmentImpl<?> assignmentA,
            @NotNull EvaluatedAssignmentImpl<?> assignmentB,
            EvaluatedAssignmentTargetImpl targetB,
            JAXBElement<ExclusionPolicyConstraintType> constraintElement,
            AssignmentPolicyRuleEvaluationContext<?> ctx,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        EvaluatedPolicyRule policyRule = ctx.policyRule;
        AssignmentPath pathA = policyRule.getAssignmentPath();
        @NotNull AssignmentPath pathB = targetB.getAssignmentPath();
        LocalizableMessage infoA = createObjectInfo(pathA, assignmentA.getTarget(), true);
        LocalizableMessage infoB = createObjectInfo(pathB, targetB.getTarget(), false);
        ObjectType objectA = getConflictingObject(pathA, assignmentA.getTarget());
        ObjectType objectB = getConflictingObject(pathB, targetB.getTarget());

        stateCheck(pathA != null,
                "Assignment path for exclusion constraint cannot be determined: %s", ctx);
        stateCheck(objectA != null,
                "Object for exclusion constraint cannot be determined: %s", ctx);
        stateCheck(objectB != null,
                "Conflicting object for exclusion constraint cannot be determined: %s", ctx);

        LocalizableMessage message = createMessage(infoA, infoB, constraintElement, ctx, CONSTRAINT_KEY_EXCLUSION, result);
        LocalizableMessage shortMessage = createShortMessage(infoA, infoB, constraintElement, ctx, CONSTRAINT_KEY_EXCLUSION, result);
        return new EvaluatedExclusionTrigger(
                constraintElement.getValue(), message, shortMessage,
                assignmentA, assignmentB,
                objectA, objectB, pathA, pathB,
                false);
    }

    private EvaluatedExclusionRequirementTrigger createRequirementTrigger(
            EvaluatedAssignmentImpl<?> assignmentA,
            JAXBElement<ExclusionPolicyConstraintType> constraintElement,
            AssignmentPolicyRuleEvaluationContext<?> ctx,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        EvaluatedPolicyRule policyRule = ctx.policyRule;
        AssignmentPath pathA = policyRule.getAssignmentPath();
        LocalizableMessage infoA = createObjectInfo(pathA, assignmentA.getTarget(), false);
        ObjectType objectA = getConflictingObject(pathA, assignmentA.getTarget());
        ObjectReferenceType requiredObjectRef = constraintElement.getValue().getTargetRef();
        ObjectReferenceType requiredArchetypeRef = constraintElement.getValue().getTargetArchetypeRef();
        Object infoB;
        if (requiredObjectRef == null) {
            if (requiredArchetypeRef == null) {
                infoB = "unspecified";
            } else {
                try {
                    @NotNull ArchetypeType requiredArchetype = objectResolver.resolve(requiredArchetypeRef, ArchetypeType.class, null, "requirement policy rule", ctx.getTask(), result);
                    infoB = createObjectInfo(null, requiredArchetype.asPrismObject(), false);
                } catch (ObjectNotFoundException e) {
                    infoB = requiredArchetypeRef.getOid();
                }
            }
        } else {
            if (requiredObjectRef.getOid() == null) {
                if (requiredObjectRef.getFilter() == null) {
                    infoB = "unspecified";
                } else {
                    infoB = "specified by filter";
                }
            } else {
                try {
                    @NotNull ObjectType requiredObject = objectResolver.resolve(requiredObjectRef, ObjectType.class, null, "requirement policy rule", ctx.getTask(), result);
                    infoB = createObjectInfo(null, requiredObject.asPrismObject(), false);
                } catch (ObjectNotFoundException e) {
                    infoB = requiredObjectRef.getOid();
                }
            }
        }

        stateCheck(pathA != null,
                "Assignment path for requirement constraint cannot be determined: %s", ctx);
        stateCheck(objectA != null,
                "Object for requirement constraint cannot be determined: %s", ctx);

        LocalizableMessage message = createMessage(infoA, infoB, constraintElement, ctx, CONSTRAINT_KEY_REQUIREMENT, result);
        LocalizableMessage shortMessage = createShortMessage(infoA, infoB, constraintElement, ctx, CONSTRAINT_KEY_REQUIREMENT, result);
        return new EvaluatedRequirementTrigger(
                constraintElement.getValue(), message, shortMessage,
                assignmentA,
                objectA, requiredObjectRef, requiredArchetypeRef, pathA, false);
    }

    @NotNull
    private <AH extends AssignmentHolderType> LocalizableMessage createMessage(
            LocalizableMessage infoA, Object infoB,
            JAXBElement<ExclusionPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<AH> ctx, String constraintKey, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + constraintKey)
                .args(infoA, infoB)
                .build();
        return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
    }

    @NotNull
    private <AH extends AssignmentHolderType> LocalizableMessage createShortMessage(LocalizableMessage infoA, Object infoB,
            JAXBElement<ExclusionPolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<AH> ctx, String constraintKey, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + constraintKey)
                .args(infoA, infoB)
                .build();
        return evaluatorHelper.createLocalizableShortMessage(constraintElement, ctx, builtInMessage, result);
    }

    private ObjectType getConflictingObject(AssignmentPath path, PrismObject<?> defaultObject) {
        if (path == null) {
            return ObjectTypeUtil.toObjectable(defaultObject);
        }
        List<ObjectType> objects = path.getFirstOrderChain();
        return objects.isEmpty() ?
                ObjectTypeUtil.toObjectable(defaultObject) : objects.get(objects.size() - 1);
    }

    private LocalizableMessage createObjectInfo(AssignmentPath path, PrismObject<?> defaultObject, boolean startsWithUppercase) {
        if (path == null) {
            return ObjectTypeUtil.createDisplayInformation(defaultObject, startsWithUppercase);
        }
        List<ObjectType> objects = path.getFirstOrderChain();
        if (objects.isEmpty()) {        // shouldn't occur
            return ObjectTypeUtil.createDisplayInformation(defaultObject, startsWithUppercase);
        }
        PrismObject<?> last = objects.get(objects.size() - 1).asPrismObject();
        if (objects.size() == 1) {
            return ObjectTypeUtil.createDisplayInformation(last, startsWithUppercase);
        }
        String pathString = objects.stream()
                .map(o -> PolyString.getOrig(o.getName()))
                .collect(Collectors.joining(" -> "));
        return ObjectTypeUtil.createDisplayInformationWithPath(last, startsWithUppercase, pathString);
    }
}
