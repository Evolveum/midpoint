/*

 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.api.context.EvaluatedExclusionTrigger;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentTargetImpl;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;

import static com.evolveum.midpoint.util.DebugUtil.lazy;

/**
 * Evaluates exclusion policy constraints.
 */
@Component
public class ExclusionConstraintEvaluator implements PolicyConstraintEvaluator<ExclusionPolicyConstraintType> {

    private static final Trace LOGGER = TraceManager.getTrace(ExclusionConstraintEvaluator.class);

    private static final String OP_EVALUATE = ExclusionConstraintEvaluator.class.getName() + ".evaluate";

    private static final String CONSTRAINT_KEY = "exclusion";

    @Autowired private ConstraintEvaluatorHelper evaluatorHelper;
    @Autowired private PrismContext prismContext;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired private RelationRegistry relationRegistry;

    @Override
    public <AH extends AssignmentHolderType> EvaluatedExclusionTrigger evaluate(@NotNull JAXBElement<ExclusionPolicyConstraintType> constraint,
            @NotNull PolicyRuleEvaluationContext<AH> rctx, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            LOGGER.trace("Evaluating exclusion constraint {} on {}",
                    lazy(() -> PolicyRuleTypeUtil.toShortString(constraint)), rctx);
            if (!(rctx instanceof AssignmentPolicyRuleEvaluationContext)) {
                return null;
            }

            AssignmentPolicyRuleEvaluationContext<AH> ctx = (AssignmentPolicyRuleEvaluationContext<AH>) rctx;
            if (!ctx.isAdded && !ctx.isKept) {
                LOGGER.trace("Assignment not being added nor kept, skipping evaluation.");
                return null;
            }

            if (sourceOrderConstraintsDoNotMatch(constraint, ctx)) {
                // logged in the called method body
                return null;
            }

            /*
             * Now let us check the exclusions.
             *
             * Assignment A is the current evaluated assignment. It has directly or indirectly attached the exclusion policy rule.
             * We now go through all other assignments B and check the exclusions.
             */

            List<OrderConstraintsType> targetOrderConstraints = defaultIfEmpty(constraint.getValue().getTargetOrderConstraint());
            List<EvaluatedAssignmentTargetImpl> nonNegativeTargetsA = ctx.evaluatedAssignment.getNonNegativeTargets();

            for (EvaluatedAssignmentImpl<AH> assignmentB : ctx.evaluatedAssignmentTriple.getNonNegativeValues()) {
                if (assignmentB == ctx.evaluatedAssignment) { // currently there is no other way of comparing the evaluated assignments
                    continue;
                }
                targetB:
                for (EvaluatedAssignmentTargetImpl targetB : assignmentB.getNonNegativeTargets()) {
                    if (!pathMatches(targetB.getAssignmentPath(), targetOrderConstraints)) {
                        LOGGER.trace("Skipping considering exclusion target {} because it does not match target path constraints."
                                + " Path={}, constraints={}", targetB, targetB.getAssignmentPath(), targetOrderConstraints);
                        continue;
                    }
                    if (!oidMatches(constraint.getValue().getTargetRef(), targetB, prismContext, matchingRuleRegistry,
                            "exclusion constraint")) {
                        LOGGER.trace("Target {} OID does not match exclusion filter", targetB);
                        continue;
                    }
                    // To avoid false positives let us check if this target is not already covered by assignment being evaluated
                    for (EvaluatedAssignmentTargetImpl targetA : nonNegativeTargetsA) {
                        if (targetIsAlreadyCovered(targetB, targetA)) {
                            continue targetB;
                        }
                    }
                    EvaluatedExclusionTrigger rv =
                            createTrigger(ctx.evaluatedAssignment, assignmentB, targetB, constraint, ctx.policyRule, ctx, result);
                    result.addReturn("trigger", rv.toDiagShortcut());
                    return rv;
                }
            }
            return null;
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
        if (ctx.policyRule.isGlobal()) {
            if (!pathMatches(ctx.policyRule.getAssignmentPath(), sourceOrderConstraints)) {
                LOGGER.trace("Assignment path to the global policy rule does not match source order constraints,"
                                + " not triggering. Path={}, source order constraints={}",
                        ctx.policyRule.getAssignmentPath(), sourceOrderConstraints);
                return true;
            }
        } else {
            // It is not clear how to match orderConstraint with assignment path of the constraint.
            // Let us try the following test: we consider it matching if there's at least one segment
            // on the path that matches the constraint.
            boolean found = ctx.policyRule.getAssignmentPath().getSegments().stream()
                    .anyMatch(segment -> segment.matches(sourceOrderConstraints));
            if (!found) {
                LOGGER.trace("No segment in assignment path to the assigned policy rule does not match source order "
                        + "constraints, not triggering. Whole path={}, constraints={}", ctx.policyRule.getAssignmentPath(),
                        sourceOrderConstraints);
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean pathMatches(AssignmentPath assignmentPath, List<OrderConstraintsType> definedOrderConstraints) {
        if (assignmentPath == null) {
            throw new IllegalStateException("Assignment path is null.");
        }
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
        return Collections.singletonList(new OrderConstraintsType(prismContext).order(1));
    }

    static boolean oidMatches(ObjectReferenceType targetRef, EvaluatedAssignmentTargetImpl assignmentTarget,
            PrismContext prismContext, MatchingRuleRegistry matchingRuleRegistry, String context) throws SchemaException {
        if (targetRef == null) {
            return true; // this means we rely on comparing relations
        }
        if (assignmentTarget.getOid() == null) {
            return false;        // shouldn't occur
        }
        if (targetRef.getOid() != null) {
            return assignmentTarget.getOid().equals(targetRef.getOid());
        }
        if (targetRef.getResolutionTime() == EvaluationTimeType.RUN) {
            SearchFilterType filterType = targetRef.getFilter();
            if (filterType == null) {
                throw new SchemaException("No filter in " + context);
            }
            QName typeQName = targetRef.getType();
            @SuppressWarnings("rawtypes")
            PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByType(typeQName);
            ObjectFilter filter = prismContext.getQueryConverter().parseFilter(filterType, objDef);
            PrismObject<? extends AssignmentHolderType> target = assignmentTarget.getTarget();
            return filter.match(target.getValue(), matchingRuleRegistry);
        } else {
            throw new SchemaException("No OID in " + context);
        }
    }

    private <AH extends AssignmentHolderType> EvaluatedExclusionTrigger createTrigger(EvaluatedAssignmentImpl<AH> assignmentA,
            @NotNull EvaluatedAssignmentImpl<AH> assignmentB, EvaluatedAssignmentTargetImpl targetB,
            JAXBElement<ExclusionPolicyConstraintType> constraintElement, EvaluatedPolicyRule policyRule,
            AssignmentPolicyRuleEvaluationContext<AH> ctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

        AssignmentPath pathA = policyRule.getAssignmentPath();
        AssignmentPath pathB = targetB.getAssignmentPath();
        LocalizableMessage infoA = createObjectInfo(pathA, assignmentA.getTarget(), true);
        LocalizableMessage infoB = createObjectInfo(pathB, targetB.getTarget(), false);
        ObjectType objectA = getConflictingObject(pathA, assignmentA.getTarget());
        ObjectType objectB = getConflictingObject(pathB, targetB.getTarget());

        LocalizableMessage message = createMessage(infoA, infoB, constraintElement, ctx, result);
        LocalizableMessage shortMessage = createShortMessage(infoA, infoB, constraintElement, ctx, result);
        return new EvaluatedExclusionTrigger(constraintElement.getValue(), message, shortMessage, assignmentB, objectA, objectB, pathA, pathB);
    }

    @NotNull
    private <AH extends AssignmentHolderType> LocalizableMessage createMessage(LocalizableMessage infoA, LocalizableMessage infoB,
            JAXBElement<ExclusionPolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<AH> ctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY)
                .args(infoA, infoB)
                .build();
        return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
    }

    @NotNull
    private <AH extends AssignmentHolderType> LocalizableMessage createShortMessage(LocalizableMessage infoA, LocalizableMessage infoB,
            JAXBElement<ExclusionPolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<AH> ctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY)
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
                ObjectTypeUtil.toObjectable(defaultObject) : objects.get(objects.size()-1);
    }

    private LocalizableMessage createObjectInfo(AssignmentPath path, PrismObject<?> defaultObject, boolean startsWithUppercase) {
        if (path == null) {
            return ObjectTypeUtil.createDisplayInformation(defaultObject, startsWithUppercase);
        }
        List<ObjectType> objects = path.getFirstOrderChain();
        if (objects.isEmpty()) {        // shouldn't occur
            return ObjectTypeUtil.createDisplayInformation(defaultObject, startsWithUppercase);
        }
        PrismObject<?> last = objects.get(objects.size()-1).asPrismObject();
        if (objects.size() == 1) {
            return ObjectTypeUtil.createDisplayInformation(last, startsWithUppercase);
        }
        String pathString = objects.stream()
                .map(o -> PolyString.getOrig(o.getName()))
                .collect(Collectors.joining(" -> "));
        return ObjectTypeUtil.createDisplayInformationWithPath(last, startsWithUppercase, pathString);
    }


}
