/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import static com.evolveum.midpoint.model.impl.lens.assignments.Util.isNonNegative;

import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.ArchetypeManager;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Evaluates resolved assignment target: its payload (authorizations, GUI config) and assignments/inducements.
 */
class TargetEvaluation<AH extends AssignmentHolderType> extends AbstractEvaluation<AH> {

    private static final Trace LOGGER = TraceManager.getTrace(TargetEvaluation.class);

    private final OperationResult result;

    /**
     * The resolved target.
     */
    @NotNull private final AssignmentHolderType target;

    /**
     * Overall condition state of this segment. It is composed later with target (role) condition
     * state into targetOverallConditionState.
     */
    private final ConditionState assignmentOverallConditionState;

    /**
     * Condition state after application of target (role) condition.
     */
    private ConditionState targetOverallConditionState;

    /**
     * Aggregated validity of target and the whole path.
     */
    private TargetActivation targetActivation;

    TargetEvaluation(AssignmentPathSegmentImpl segment, EvaluationContext<AH> ctx, OperationResult result) {
        super(segment, ctx);
        this.result = result;

        this.assignmentOverallConditionState = segment.getOverallConditionState();
        this.target = (AssignmentHolderType) segment.getTarget();
    }

    void evaluate() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException,
            SecurityViolationException, ConfigurationException, CommunicationException {

        assert ctx.assignmentPath.last() == segment;
        assert assignmentOverallConditionState.isNotAllFalse();
        assert segment.isAssignmentActive() || segment.direct;
        checkIfAlreadyEvaluated();

        if (ctx.ae.evaluatedAssignmentTargetCache.canSkip(segment, ctx.primaryAssignmentMode)) {
            LOGGER.trace("Skipping evaluation of segment {} because it is idempotent and we have seen the target before", segment);
            InternalMonitor.recordRoleEvaluationSkip(target, true);
            return;
        }

        LOGGER.trace("Evaluating segment TARGET:\n{}", segment.debugDumpLazily(1));
        LOGGER.debug("Evaluating RBAC [{}]", ctx.assignmentPath.shortDumpLazily());

        checkRelationWithTarget(target);
        determineValidity();

        InternalMonitor.recordRoleEvaluation(target, true);

        AssignmentTargetEvaluationInformation targetEvaluationInformation;
        if (targetActivation.pathAndTargetActive) {
            // Cache it immediately, even before evaluation. So if there is a cycle in the role path
            // then we can detect it and skip re-evaluation of aggressively idempotent roles.
            targetEvaluationInformation = ctx.ae.evaluatedAssignmentTargetCache.recordProcessing(segment, ctx.primaryAssignmentMode);
        } else {
            targetEvaluationInformation = null;
        }

        int targetPolicyRulesOnEntry = ctx.evalAssignment.getAllTargetsPolicyRulesCount();
        try {
            if (targetActivation.targetActive) {
                // TODO why only for valid targets? This is how it was implemented in original AssignmentEvaluator.
                targetOverallConditionState = ConditionState.merge(
                        assignmentOverallConditionState, determineTargetConditionState());
            } else {
                targetOverallConditionState = assignmentOverallConditionState;
            }

            if (targetOverallConditionState.isNotAllFalse()) {
                evaluateInternal();
            }
        } finally {
            int targetPolicyRulesOnExit = ctx.evalAssignment.getAllTargetsPolicyRulesCount();
            LOGGER.trace("Evaluating segment target DONE for {}; target policy rules: {} -> {}", segment,
                    targetPolicyRulesOnEntry, targetPolicyRulesOnExit);
            if (targetEvaluationInformation != null) {
                targetEvaluationInformation.setBringsTargetPolicyRules(targetPolicyRulesOnExit > targetPolicyRulesOnEntry);
            }
        }
    }

    private void evaluateInternal()
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException,
            ConfigurationException, CommunicationException, PolicyViolationException {
        assert targetOverallConditionState.isNotAllFalse();

        collectEvaluatedAssignmentTarget();

        // we need to evaluate assignments also for non-valid targets, because of target policy rules
        // ... but only for direct ones!
        if (targetActivation.targetActive || segment.direct) {
            evaluateAssignments();
        }

        // We need to collect membership also for disabled targets (provided the assignment itself is enabled): MID-4127.
        // It is quite logical: if a user is member of a disabled role, it still needs to have roleMembershipRef
        // pointing to that role.
        if (targetOverallConditionState.isNewTrue() && Util.shouldCollectMembership(segment)) {
            ctx.membershipCollector.collect(target, segment.relation);
        }

        if (targetActivation.targetActive) {

            // TODO In a way analogous to the membership info above: shouldn't we collect tenantRef information
            //  also for disabled tenants?
            if (targetOverallConditionState.isNewTrue()) {
                collectTenantRef(target, ctx);
            }

            // We continue evaluation even if the relation is non-membership and non-delegation.
            // Computation of isMatchingOrder will ensure that we won't collect any unwanted content.
            evaluateInducements();

            // Respective conditions are evaluated inside
            evaluateTargetPayload();

        } else {
            LOGGER.trace("Not collecting payload from target of {} as it is not valid", segment);
        }
    }

    private void evaluateAssignments() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {
        for (AssignmentType assignment : target.getAssignment()) {
            new TargetAssignmentEvaluation<>(segment, targetOverallConditionState, targetActivation, ctx, result, assignment)
                    .evaluate();
        }
    }

    private void evaluateInducements() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {
        if (target instanceof AbstractRoleType) {
            for (AssignmentType inducement : ((AbstractRoleType) target).getInducement()) {
                new TargetInducementEvaluation<>(segment, targetOverallConditionState, targetActivation, ctx, result, inducement)
                        .evaluate();
            }
        }
    }

    private void evaluateTargetPayload() {
        new TargetPayloadEvaluation<>(segment, targetOverallConditionState, targetActivation, ctx).evaluate();
    }

    private ConditionState determineTargetConditionState()
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        MappingType condition = target instanceof AbstractRoleType ? ((AbstractRoleType) target).getCondition() : null;
        // TODO why we use "segment.source" as source object for condition evaluation even if
        //  we evaluate condition in target role? We should probably use the role itself as the source here.
        AssignmentHolderType source = segment.source;
        return ctx.conditionEvaluator.computeConditionState(
                condition,
                source,
                "condition in " + segment.getTargetDescription(), target,
                result);
    }

    private void collectEvaluatedAssignmentTarget() {
        EvaluatedAssignmentTargetImpl evalAssignmentTarget = new EvaluatedAssignmentTargetImpl(
                target.asPrismObject(),
                segment.isMatchingOrder, // evaluateConstructions: exact meaning of this is to be revised
                ctx.assignmentPath.clone(),
                segment.assignment,
                targetActivation.pathAndTargetActive);
        ctx.evalAssignment.addRole(evalAssignmentTarget, targetOverallConditionState.getAbsoluteRelativityMode()); // TODO absolute or relative?
    }

    private void determineValidity() throws ConfigurationException {
        // FIXME Target state model does not reflect its archetype!
        LifecycleStateModelType targetStateModel = ArchetypeManager.determineLifecycleModel(target.asPrismObject(), ctx.ae.systemConfiguration);
        boolean targetValid = LensUtil.isFocusValid(target, ctx.ae.now, ctx.ae.activationComputer, targetStateModel);
        boolean pathAndTargetValid = segment.isFullPathActive() && targetValid;
        targetActivation = new TargetActivation(targetValid, pathAndTargetValid);
    }

    private void checkRelationWithTarget(AssignmentHolderType target)
            throws SchemaException {
        if (target instanceof AbstractRoleType || target instanceof TaskType) { //TODO:
            // OK, just go on
        } else if (target instanceof UserType) {
            if (!ctx.ae.relationRegistry.isDelegation(segment.relation)) {
                throw new SchemaException("Unsupported relation " + segment.relation + " for assignment of target type " + target + " in " + segment.sourceDescription);
            }
        } else {
            throw new SchemaException("Unknown assignment target type " + target + " in " + segment.sourceDescription);
        }
    }

    private void collectTenantRef(AssignmentHolderType targetToSet, EvaluationContext<AH> ctx) {
        if (targetToSet instanceof OrgType) {
            if (BooleanUtils.isTrue(((OrgType) targetToSet).isTenant()) && ctx.evalAssignment.getTenantOid() == null) {
                if (ctx.assignmentPath.hasOnlyOrgs()) {
                    ctx.evalAssignment.setTenantOid(targetToSet.getOid());
                }
            }
        }
    }

    static class TargetActivation {
        final boolean targetActive;
        final boolean pathAndTargetActive;

        private TargetActivation(boolean targetActive, boolean pathAndTargetActive) {
            this.targetActive = targetActive;
            this.pathAndTargetActive = pathAndTargetActive;
        }
    }
}
