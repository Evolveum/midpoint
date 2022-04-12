/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeSynchronizationPolicy;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;

public class SynchronizationServiceUtils {

    //private static final Trace LOGGER = TraceManager.getTrace(SynchronizationServiceUtils.class);

    /**
     * Checks if the policy is applicable either to discriminator (if provided), or to the shadow in sync context.
     *
     * Moreover, policy condition is evaluated in both cases. Hence the name "fully applicable".
     *
     * TODO move to {@link SynchronizationContextLoader} after
     *  {@link MidpointFunctions#findCandidateOwners(Class, ShadowType, String, ShadowKindType, String)} is reworked.
     */
    public static <F extends FocusType> boolean isPolicyFullyApplicable(
            @NotNull ResourceObjectTypeSynchronizationPolicy policy,
            @Nullable ObjectSynchronizationDiscriminatorType discriminator,
            @NotNull SynchronizationContext<F> syncCtx,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        boolean isPolicyApplicable;
        if (discriminator != null) {
            isPolicyApplicable = policy.isApplicableToDiscriminator(discriminator);
        } else {
            isPolicyApplicable = policy.isApplicableToShadow(syncCtx.getShadowedResourceObject());
        }

        return isPolicyApplicable &&
                evaluateSynchronizationPolicyCondition(policy, syncCtx, result);
    }

    private static <F extends FocusType> boolean evaluateSynchronizationPolicyCondition(
            @NotNull ResourceObjectTypeSynchronizationPolicy policy,
            @NotNull SynchronizationContext<F> syncCtx,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        ObjectSynchronizationType synchronizationBean = policy.getSynchronizationBean();
        if (synchronizationBean.getCondition() == null) {
            return true;
        }
        ExpressionType conditionExpressionBean = synchronizationBean.getCondition();
        String desc = "condition in object synchronization " + synchronizationBean.getName();
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(
                null, syncCtx.getShadowedResourceObject(), syncCtx.getResource(), syncCtx.getSystemConfiguration());
        try {
            ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(syncCtx.getTask(), result));
            ExpressionFactory expressionFactory = syncCtx.getBeans().expressionFactory;
            return ExpressionUtil.evaluateConditionDefaultTrue(variables,
                    conditionExpressionBean, syncCtx.getExpressionProfile(), expressionFactory, desc, syncCtx.getTask(), result);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }

    static boolean isLogDebug(ResourceObjectShadowChangeDescription change) {
        // Reconciliation changes are routine. Do not let them pollute the log files.
        return !SchemaConstants.CHANNEL_RECON_URI.equals(change.getSourceChannel());
    }

    static <F extends FocusType> boolean isLogDebug(SynchronizationContext<F> syncCtx) {
        return !SchemaConstants.CHANNEL_RECON_URI.equals(syncCtx.getChannel());
    }
}
