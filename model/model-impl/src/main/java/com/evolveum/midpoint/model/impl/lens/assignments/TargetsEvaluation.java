/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import static com.evolveum.midpoint.model.impl.lens.assignments.Util.isChanged;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.AssignmentPathVariables;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
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
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

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

    void evaluate() throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException, PolicyViolationException, ObjectNotFoundException {
        assert ctx.assignmentPath.last() == segment;
        assert segment.getAssignmentRelativityMode() != null;
        assert segment.isAssignmentValid() || segment.direct;

        checkIfAlreadyEvaluated();

        if (segment.assignment.getTargetRef() == null) {
            LOGGER.trace("No targetRef for {}, nothing to evaluate", segment);
            return;
        }

        if (ctx.ae.loginMode && !ctx.ae.relationRegistry.isProcessedOnLogin(segment.relation)) {
            LOGGER.trace("Skipping processing of assignment target {} because relation {} is configured for login skip", segment.assignment.getTargetRef().getOid(), segment.relation);
            // Skip - to optimize logging-in, we skip all assignments with non-membership/non-delegation relations (e.g. approver, owner, etc)
            // We want to make this configurable in the future MID-3581
            return;
        }

        if (!ctx.ae.loginMode && !isChanged(ctx.primaryAssignmentMode) &&
                !ctx.ae.relationRegistry.isProcessedOnRecompute(segment.relation) && !shouldEvaluateAllAssignmentRelationsOnRecompute()) {
            LOGGER.debug("Skipping processing of assignment target for {} because relation {} is configured for recompute skip (primary assignment mode={})", segment, segment.relation, ctx.primaryAssignmentMode);
            // Skip - to optimize recompute, we skip all assignments with non-membership/non-delegation relations (e.g. approver, owner, etc)
            // Never skip this if assignment has changed. We want to process this, e.g. to enforce min/max assignee rules.
            // We want to make this configurable in the future MID-3581
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
        boolean resolvedTargets = false;
        // TODO CLEAN THIS UP
        // Important: but we still want this to be reflected in roleMembershipRef
        if (segment.isNonNegativeRelativityMode() && Util.shouldCollectMembership(segment)) {
            if (segment.assignment.getTargetRef().getOid() != null) {
                ctx.membershipCollector.collect(segment.assignment.getTargetRef(), segment.relation);
            } else {
                // no OID, so we have to resolve the filter
                resolvedTargets = true;
                targets.addAll(getTargets());
                for (PrismObject<? extends ObjectType> targetObject : targets) {
                    ObjectType target = targetObject.asObjectable();
                    if (target instanceof FocusType) {
                        ctx.membershipCollector.collect((FocusType) target, segment.relation);
                    }
                }
            }
        }

        // We have to know targets for direct assignments
        if (segment.direct && !resolvedTargets) {
            targets.addAll(getTargets());
        }
    }

    private boolean hasCycle(@NotNull PrismObject<? extends ObjectType> target) throws PolicyViolationException {
        // TODO reconsider this
        if (target.getOid().equals(segment.source.getOid())) {
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
    private List<? extends PrismObject<? extends ObjectType>> getTargets() throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
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
        }
    }

    @NotNull
    private List<? extends PrismObject<? extends ObjectType>> resolveTargets(AssignmentPathSegmentImpl segment, EvaluationContext<AH> ctx,
            OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        ObjectReferenceType targetRef = segment.assignment.getTargetRef();
        String oid = targetRef.getOid();

        // Target is referenced, need to fetch it
        Class<? extends ObjectType> targetClass;
        if (targetRef.getType() != null) {
            targetClass = ctx.ae.prismContext.getSchemaRegistry().determineCompileTimeClass(targetRef.getType());
            if (targetClass == null) {
                throw new SchemaException("Cannot determine type from " + targetRef.getType() + " in target reference in " + segment.assignment + " in " + segment.sourceDescription);
            }
        } else {
            throw new SchemaException("Missing type in target reference in " + segment.assignment + " in " + segment.sourceDescription);
        }

        if (oid == null) {
            LOGGER.trace("Resolving dynamic target ref");
            if (targetRef.getFilter() == null) {
                throw new SchemaException("The OID and filter are both null in assignment targetRef in "+segment.source);
            }
            return resolveTargetsFromFilter(targetClass, targetRef.getFilter(), segment, ctx, result);
        } else {
            LOGGER.trace("Resolving target {}:{} from repository", targetClass.getSimpleName(), oid);
            PrismObject<? extends ObjectType> target;
            try {
                target = ctx.ae.repository.getObject(targetClass, oid, null, result);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " in " + segment.sourceDescription, e);
            }
            // Not handling object not found exception here. Caller will handle that.
            if (target == null) {
                throw new IllegalArgumentException("Got null target from repository, oid:"+oid+", class:"+targetClass+" (should not happen, probably a bug) in "+segment.sourceDescription);
            }
            return Collections.singletonList(target);
        }
    }

    @NotNull
    private List<? extends PrismObject<? extends ObjectType>> resolveTargetsFromFilter(Class<? extends ObjectType> targetClass,
            SearchFilterType filter, AssignmentPathSegmentImpl segment,
            EvaluationContext<AH> ctx, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException{
        ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(ctx.ae.lensContext, null, ctx.task, result));
        try {
            PrismObject<SystemConfigurationType> systemConfiguration = ctx.ae.systemObjectCache.getSystemConfiguration(result);
            ExpressionVariables variables = ModelImplUtils.getDefaultExpressionVariables(segment.source, null, null, systemConfiguration.asObjectable(), ctx.ae.prismContext);
            variables.put(ExpressionConstants.VAR_SOURCE, segment.getOrderOneObject(), ObjectType.class);
            AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(ctx.assignmentPath);
            if (assignmentPathVariables != null) {
                ModelImplUtils.addAssignmentPathVariables(assignmentPathVariables, variables, getPrismContext());
            }
            variables.addVariableDefinitions(ctx.ae.getAssignmentEvaluationVariables());
            ObjectFilter origFilter = ctx.ae.prismContext.getQueryConverter().parseFilter(filter, targetClass);
            // TODO: expression profile should be determined from the holding object archetype
            ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
            ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(origFilter, variables, expressionProfile, ctx.ae.mappingFactory.getExpressionFactory(), ctx.ae.prismContext, " evaluating resource filter expression ", ctx.task, result);
            if (evaluatedFilter == null) {
                throw new SchemaException("The OID is null and filter could not be evaluated in assignment targetRef in "+segment.source);
            }

            return ctx.ae.repository.searchObjects(targetClass, ctx.ae.prismContext.queryFactory().createQuery(evaluatedFilter), null, result);
            // we don't check for no targets here; as we don't care for referential integrity
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }
}
