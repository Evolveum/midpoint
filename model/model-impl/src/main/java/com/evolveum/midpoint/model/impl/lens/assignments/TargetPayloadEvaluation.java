/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Collects target (i.e. role) payload: authorizations, GUI configuration.
 */
class TargetPayloadEvaluation<AH extends AssignmentHolderType> extends AbstractEvaluation<AH> {

    private static final Trace LOGGER = TraceManager.getTrace(TargetPayloadEvaluation.class);

    private final ConditionState targetOverallConditionState;

    @NotNull private final TargetEvaluation.TargetActivity targetActivity;

    TargetPayloadEvaluation(@NotNull AssignmentPathSegmentImpl segment, ConditionState targetOverallConditionState,
            @NotNull TargetEvaluation.TargetActivity targetActivity,
            @NotNull EvaluationContext<AH> ctx) {
        super(segment, ctx);
        this.targetOverallConditionState = targetOverallConditionState;
        this.targetActivity = targetActivity;
    }

    public void evaluate() {
        assert segment.isAssignmentActive() || segment.direct;
        assert targetActivity.targetActive;
        assert targetOverallConditionState.isNotAllFalse();
        checkIfAlreadyEvaluated();

        ObjectType target = segment.getTarget();
        if (target instanceof AbstractRoleType) {
            if (!segment.isMatchingOrder) {
                LOGGER.trace("Not collecting payload from target of {} as it is of not matching order: {}", segment, segment.getEvaluationOrder());
            } else if (targetOverallConditionState.isNewFalse()) {
                LOGGER.trace("Not collecting payload from target of {} as the target relativity mode is not non-negative: {}", segment, targetOverallConditionState);
            } else {
                for (AuthorizationType authorizationBean : ((AbstractRoleType) target).getAuthorization()) {
                    Authorization authorization = createAuthorization(authorizationBean, target.toString());
                    if (!ctx.evalAssignment.getAuthorizations().contains(authorization)) {
                        ctx.evalAssignment.addAuthorization(authorization);
                    }
                }
                AdminGuiConfigurationType adminGuiConfiguration = ((AbstractRoleType) target).getAdminGuiConfiguration();
                ctx.evalAssignment.addAdminGuiDependency(target.getOid());
                if (adminGuiConfiguration != null &&
                        !ctx.evalAssignment.getAdminGuiConfigurations().contains(adminGuiConfiguration)) {
                    ctx.evalAssignment.addAdminGuiConfiguration(adminGuiConfiguration);
                }
            }
        }
    }

    private Authorization createAuthorization(@NotNull AuthorizationType authorizationBean, String sourceDesc) {
        Authorization authorization = new Authorization(authorizationBean);
        authorization.setSourceDescription(sourceDesc);
        return authorization;
    }
}
