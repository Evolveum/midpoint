/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import static com.evolveum.midpoint.model.api.util.ReferenceResolver.Source.REPOSITORY;
import static com.evolveum.midpoint.model.impl.lens.assignments.Util.isChanged;
import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.model.impl.lens.projector.AssignmentOrigin;
import com.evolveum.midpoint.util.QNameUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.model.api.util.ReferenceResolver.FilterExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.AssignmentPathVariables;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Evaluates assignment target(s) - if there are any.
 */
class TargetsEvaluation<AH extends AssignmentHolderType> extends AbstractEvaluation<AH> {

    private static final Trace LOGGER = TraceManager.getTrace(TargetsEvaluation.class);

    /**
     * Number of times any given target is allowed to occur in the assignment path.
     * After exceeding it is not evaluated.
     */
    private static final int MAX_TARGET_OCCURRENCES = 2;

    private final OperationResult result;

    private final ObjectReferenceType targetRef;

    /**
     * Resolved target objects.
     */
    final List<PrismObject<? extends ObjectType>> targets = new ArrayList<>();

    TargetsEvaluation(AssignmentPathSegmentImpl segment, EvaluationContext<AH> ctx, OperationResult result) {
        super(segment, ctx);
        this.result = result;
        this.targetRef = segment.assignment.getTargetRef();
    }

    void evaluate()
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, PolicyViolationException, ObjectNotFoundException {

        assert !ctx.assignmentPath.isEmpty();
        assert ctx.assignmentPath.last() == segment;
        assert segment.getOverallConditionState().isNotAllFalse();
        assert segment.isAssignmentActive() || segment.direct || segment.isArchetypeHierarchy();

        checkIfAlreadyEvaluated();

        if (targetRef == null) {
            LOGGER.trace("No targetRef for {}, nothing to evaluate", segment);
            return;
        }

        if (ctx.ae.loginMode && !ctx.ae.relationRegistry.isProcessedOnLogin(segment.relation)) {
            LOGGER.trace("Skipping processing of assignment target {} because relation {} is configured for login skip",
                    targetRef.getOid(), segment.relation);
            // Skip - to optimize logging-in, we skip all assignments with non-membership/non-delegation relations
            // (e.g. approver, owner, etc). We want to make this configurable in the future (MID-3581).
            return;
        }

        AssignmentOrigin origin = ctx.evalAssignment.getOrigin();
        if (!ctx.ae.loginMode
                // MID-10779 assignments with non default relationship (owner) not being evaluated when needed
                // todo switch for ctx.evalAssignment.isBeingAdded(), o however it currently fails in some cases
                //  with NPE where origin.isNew() can't be decided if origin is not frozen
                && !isChanged(ctx.primaryAssignmentMode)
                && !ctx.ae.relationRegistry.isProcessedOnRecompute(segment.relation)
                && !shouldEvaluateAllAssignmentRelationsOnRecompute()) {
            LOGGER.debug("Skipping processing of assignment target for {} because relation {} is configured for "
                    + "recompute skip (primary assignment mode={})", segment, segment.relation, ctx.primaryAssignmentMode);
            // Skip - to optimize recompute, we skip all assignments with non-membership/non-delegation relations
            // (e.g. approver, owner, etc). Never skip this if assignment has changed. We want to process this, e.g. to enforce
            // min/max assignee rules. We want to make this configurable in the future (MID-3581).
            // TODO but what if the assignment itself has not changed but some of the conditions have?
            addSkippedTargetsToMembershipLists();
            return;
        }

        targets.addAll(getTargets());
        LOGGER.trace("Targets in {}, assignment ID {}: {}", segment.source, segment.assignment.getId(), targets);
        for (PrismObject<? extends ObjectType> target : targets) {
            if (hasCycle(target)) {
                continue;
            }
            if (isDelegationToNonDelegableTarget(target)) {
                continue;
            }
            AssignmentHolderType targetBean = (AssignmentHolderType) target.asObjectable();
            ctx.assignmentPath.replaceLastSegmentWithTargetedOne(targetBean);
            new TargetEvaluation<>(ctx.assignmentPath.last(), ctx, result).evaluate();
        }
    }

    private void addSkippedTargetsToMembershipLists()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException {
//        boolean resolvedTargets = false;
        // TODO CLEAN THIS UP
        // Important: but we still want this to be reflected in roleMembershipRef

        assert targetRef != null;
        if (!Util.shouldCollectMembership(segment)) {
            return;
        }

        boolean collectAllMembership = segment.isNonNegativeRelativeRelativityMode();
        if (collectAllMembership || QNameUtil.match(ArchetypeType.COMPLEX_TYPE, targetRef.getType())) {
            if (targetRef.getOid() != null) {
                ctx.membershipCollector.collect(targetRef, segment.relation, !collectAllMembership);
                // This branch does not set target for cases like Approver assignments, but saves one resolve.
                // This means that in EvaluatedAssignment you can later either have target,
                // or - if null - assignment/targetRef should have OID filled in.
            } else {
                // no OID, so we have to resolve the filter
                targets.addAll(getTargets());
                for (PrismObject<? extends ObjectType> targetObject : targets) {
                    ObjectType target = targetObject.asObjectable();
                    if (target instanceof FocusType) {
                        ctx.membershipCollector.collect((FocusType) target, segment.relation, !collectAllMembership);
                    }
                }
            }
        }
    }

    private boolean hasCycle(@NotNull PrismObject<? extends ObjectType> target) throws PolicyViolationException {
        // TODO reconsider this
        if (target.getOid() != null
                && segment.source.getOid() != null
                && Objects.equals(target.getOid(), segment.source.getOid())) {
            throw new PolicyViolationException("The "+segment.source+" refers to itself in assignment/inducement");
        }
        int count = ctx.assignmentPath.countTargetOccurrences(target.asObjectable());
        if (count >= MAX_TARGET_OCCURRENCES) {
            LOGGER.debug("Max # of target occurrences ({}) detected for target {} in {} - stopping evaluation here",
                    MAX_TARGET_OCCURRENCES, ObjectTypeUtil.toShortString(target), ctx.assignmentPath);
            return true;
        } else {
            return false;
        }
    }

    private boolean isDelegationToNonDelegableTarget(@NotNull PrismObject<? extends ObjectType> target) {
        AssignmentPathSegment previousSegment = ctx.assignmentPath.beforeLast(1);
        if (previousSegment == null || !previousSegment.isDelegation() || !target.canRepresent(AbstractRoleType.class)) {
            return false;
        }
        if (!Boolean.TRUE.equals(((AbstractRoleType)target.asObjectable()).isDelegable())) {
            LOGGER.trace("Skipping evaluation of {} because it delegates to a non-delegable target {}",
                    FocusTypeUtil.dumpAssignmentLazily(segment.assignment), target);
            return true;
        } else {
            return false;
        }
    }

    private boolean shouldEvaluateAllAssignmentRelationsOnRecompute() {
        return ModelExecuteOptions.isEvaluateAllAssignmentRelationsOnRecompute(ctx.ae.lensContext.getOptions());
    }

    @NotNull
    private List<? extends PrismObject<? extends ObjectType>> getTargets()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        try {
            return resolveTargets();
        } catch (ObjectNotFoundException ex) {
            // Do not throw an exception. We don't have referential integrity. Therefore if a role is deleted then throwing
            // an exception would prohibit any operations with the users that have the role, including removal of the reference.
            // The failure is recorded in the result (although marked as "success" in some parent result by
            // AssignmentTripleEvaluator) and we will log it. It should be enough.
            if (ctx.evalAssignment.isBeingDeleted()) {
                // Maybe we can even skip the error logging. MID-8366.
                LOGGER.debug("Referenced object not found in assignment target reference in {}; "
                        + "but the assignment is being deleted anyway: {}", segment.sourceDescription, ex.getMessage(), ex);
            } else {
                // The regular case
                LoggingUtils.logException(
                        LOGGER, "Referenced object not found in assignment target reference in {}", ex, segment.sourceDescription);
            }
            // We also trigger the reconciliation (see MID-2242) - TODO is this still needed?
            ctx.evalAssignment.setForceRecon(true);
            return List.of();
        } catch (SchemaException | ExpressionEvaluationException | CommunicationException | ConfigurationException |
                SecurityViolationException | RuntimeException e) {
            MiscUtil.throwAsSame(
                    e,
                    String.format("Couldn't resolve targets in %s in %s: %s",
                            segment.assignment, segment.sourceDescription, e.getMessage()));
            throw e; // just to make compiler happy (exception is thrown in the above statement)
        }
    }

    @NotNull
    private List<PrismObject<? extends ObjectType>> resolveTargets()
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        var filterExpressionEvaluator =
                createFilterExpressionEvaluator(segment.assignmentOrigin.child(AssignmentType.F_TARGET_REF));
        return ctx.ae.referenceResolver.resolve(
                targetRef, createReadOnlyCollection(), REPOSITORY,
                filterExpressionEvaluator, ctx.task, result);
    }

    private @NotNull FilterExpressionEvaluator createFilterExpressionEvaluator(
            @NotNull ConfigurationItemOrigin filterOrigin) {
        return (rawFilter, lResult) -> {
                ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                        new ModelExpressionEnvironment<>(ctx.ae.lensContext, null, ctx.task, lResult));
                try {
                    var expressionProfile =
                            ModelBeans.get().expressionProfileManager.determineExpressionProfileUnsafe(filterOrigin, lResult);
                    VariablesMap variables = createVariables(segment, ctx, lResult);
                    return ExpressionUtil.evaluateFilterExpressions(
                            rawFilter, variables, expressionProfile,
                            ctx.ae.mappingFactory.getExpressionFactory(),
                            "evaluating resource filter expression in " + filterOrigin.fullDescription(),
                            ctx.task, lResult);
                } finally {
                    ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
                }
            };
    }

    private @NotNull VariablesMap createVariables(
            AssignmentPathSegmentImpl segment,
            EvaluationContext<AH> ctx,
            OperationResult result) throws SchemaException {
        PrismObject<SystemConfigurationType> systemConfiguration =
                ModelBeans.get().systemObjectCache.getSystemConfiguration(result);
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(
                segment.source, null, null, asObjectable(systemConfiguration));
        variables.put(ExpressionConstants.VAR_SOURCE, segment.source, ObjectType.class);
        AssignmentPathVariables assignmentPathVariables = ctx.assignmentPath.computePathVariablesRequired();
        ModelImplUtils.addAssignmentPathVariables(assignmentPathVariables, variables);
        variables.addVariableDefinitions(ctx.ae.getAssignmentEvaluationVariables());
        return variables;
    }
}
