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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.model.api.util.ReferenceResolver;

import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.model.impl.lens.AssignmentPathVariables;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.*;
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

    /**
     * Resolved target objects.
     */
    final List<PrismObject<? extends ObjectType>> targets = new ArrayList<>();

    TargetsEvaluation(AssignmentPathSegmentImpl segment, EvaluationContext<AH> ctx, OperationResult result) {
        super(segment, ctx);
        this.result = result;
    }

    void evaluate()
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, PolicyViolationException, ObjectNotFoundException {

        assert ctx.assignmentPath.last() == segment;
        assert segment.getOverallConditionState().isNotAllFalse();
        assert segment.isAssignmentActive() || segment.direct;

        checkIfAlreadyEvaluated();

        if (segment.assignment.getTargetRef() == null) {
            LOGGER.trace("No targetRef for {}, nothing to evaluate", segment);
            return;
        }

        if (ctx.ae.loginMode && !ctx.ae.relationRegistry.isProcessedOnLogin(segment.relation)) {
            LOGGER.trace("Skipping processing of assignment target {} because relation {} is configured for login skip",
                    segment.assignment.getTargetRef().getOid(), segment.relation);
            // Skip - to optimize logging-in, we skip all assignments with non-membership/non-delegation relations
            // (e.g. approver, owner, etc). We want to make this configurable in the future (MID-3581).
            return;
        }

        if (!ctx.ae.loginMode && !isChanged(ctx.primaryAssignmentMode) &&
                !ctx.ae.relationRegistry.isProcessedOnRecompute(segment.relation) &&
                !shouldEvaluateAllAssignmentRelationsOnRecompute()) {
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
        if (segment.isNonNegativeRelativeRelativityMode() && Util.shouldCollectMembership(segment)) {
            if (segment.assignment.getTargetRef().getOid() != null) {
                ctx.membershipCollector.collect(segment.assignment.getTargetRef(), segment.relation);
            } else {
                // no OID, so we have to resolve the filter
//                resolvedTargets = true;
                targets.addAll(getTargets());
                for (PrismObject<? extends ObjectType> targetObject : targets) {
                    ObjectType target = targetObject.asObjectable();
                    if (target instanceof FocusType) {
                        ctx.membershipCollector.collect((FocusType) target, segment.relation);
                    }
                }
            }
        }

        // We have to know targets for direct assignments.
        // But these should be perhaps loaded lazily (MID-6292)
//        if (segment.direct && !resolvedTargets) {
//            targets.addAll(getTargets());
//        }
    }

    private boolean hasCycle(@NotNull PrismObject<? extends ObjectType> target) throws PolicyViolationException {
        // TODO reconsider this
        if (target.getOid() != null && segment.source.getOid() != null && Objects.equals(target.getOid(), segment.source.getOid())) {
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
    private List<? extends PrismObject<? extends ObjectType>> getTargets() throws SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        try {
            return resolveTargets(segment, ctx, result);
        } catch (ObjectNotFoundException ex) {
            // Do not throw an exception. We don't have referential integrity. Therefore if a role is deleted then throwing
            // an exception would prohibit any operations with the users that have the role, including removal of the reference.
            // The failure is recorded in the result and we will log it. It should be enough.
            LOGGER.error(ex.getMessage()+" in assignment target reference in "+segment.sourceDescription,ex);
            // For OrgType references we trigger the reconciliation (see MID-2242)
            ctx.evalAssignment.setForceRecon(true);
            return Collections.emptyList();
        } catch (SchemaException | ExpressionEvaluationException | CommunicationException | ConfigurationException |
                SecurityViolationException | RuntimeException e) {
            MiscUtil.throwAsSame(e, "Couldn't resolve targets in " + segment.assignment + " in " +
                    segment.sourceDescription + ": " + e.getMessage());
            throw e; // just to make compiler happy (exception is thrown in the above statement)
        }
    }

    @NotNull
    private List<PrismObject<? extends ObjectType>> resolveTargets(AssignmentPathSegmentImpl segment, EvaluationContext<AH> ctx,
            OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        ObjectReferenceType targetRef = segment.assignment.getTargetRef();
        ReferenceResolver.FilterEvaluator filterEvaluator = createFilterEvaluator(segment, ctx);
        return ctx.ae.referenceResolver.resolve(targetRef, createReadOnlyCollection(), REPOSITORY,
                filterEvaluator, ctx.task, result);
    }

    @NotNull
    private ReferenceResolver.FilterEvaluator createFilterEvaluator(AssignmentPathSegmentImpl segment,
            EvaluationContext<AH> ctx) {
        return (rawFilter, result1) -> {
                ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                        new ModelExpressionEnvironment<>(ctx.ae.lensContext, null, ctx.task, result1));
                try {
                    // TODO: expression profile should be determined from the holding object archetype
                    ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
                    VariablesMap variables = createVariables(segment, ctx, result1);
                    return ExpressionUtil.evaluateFilterExpressions(rawFilter, variables, expressionProfile,
                            ctx.ae.mappingFactory.getExpressionFactory(), ctx.ae.prismContext,
                            " evaluating resource filter expression ", ctx.task, result1);
                } finally {
                    ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
                }
            };
    }

    @NotNull
    private VariablesMap createVariables(AssignmentPathSegmentImpl segment, EvaluationContext<AH> ctx,
            OperationResult result) throws SchemaException {
        PrismObject<SystemConfigurationType> systemConfiguration = ctx.ae.systemObjectCache.getSystemConfiguration(result);
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(
                segment.source, null, null, systemConfiguration.asObjectable());
        variables.put(ExpressionConstants.VAR_SOURCE, segment.source, ObjectType.class);
        AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(ctx.assignmentPath);
        if (assignmentPathVariables != null) {
            ModelImplUtils.addAssignmentPathVariables(assignmentPathVariables, variables, ctx.ae.prismContext);
        }
        variables.addVariableDefinitions(ctx.ae.getAssignmentEvaluationVariables());
        return variables;
    }
}
