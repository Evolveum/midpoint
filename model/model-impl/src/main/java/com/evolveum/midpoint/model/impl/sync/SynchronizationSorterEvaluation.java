/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.prism.PrismPropertyValue.getRealValue;

/**
 * Evaluation of the synchronization sorter within given sync context.
 */
class SynchronizationSorterEvaluation<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationSorterEvaluation.class);

    @NotNull private final SynchronizationContext<F> syncCtx;
    @NotNull private final ModelBeans beans;

    SynchronizationSorterEvaluation(@NotNull SynchronizationContext<F> syncCtx, @NotNull ModelBeans beans) {
        this.syncCtx = syncCtx;
        this.beans = beans;
    }

    @Nullable ObjectSynchronizationDiscriminatorType evaluateAndApply(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        ExpressionType expression = ResourceTypeUtil.getSynchronizationSorterExpression(syncCtx.getResource().asObjectable());
        if (expression == null) {
            return null;
        }

        ObjectSynchronizationDiscriminatorType sorterResult = evaluate(expression, result);
        if (sorterResult != null) {
            applySorterResult(sorterResult);
        }
        return sorterResult;
    }

    @Nullable
    private ObjectSynchronizationDiscriminatorType evaluate(@NotNull ExpressionType expression, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        Task task = syncCtx.getTask();
        String desc = "synchronization divider type ";
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(
                null,
                syncCtx.getShadowedResourceObject(),
                null,
                syncCtx.getResource(),
                syncCtx.getSystemConfiguration(),
                null,
                syncCtx.getPrismContext());
        variables.put(ExpressionConstants.VAR_CHANNEL, syncCtx.getChannel(), String.class);
        try {
            ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
            //noinspection unchecked
            PrismPropertyDefinition<ObjectSynchronizationDiscriminatorType> discriminatorDef =
                    PrismContext.get().getSchemaRegistry()
                            .findPropertyDefinitionByElementName(SchemaConstantsGenerated.C_OBJECT_SYNCHRONIZATION_DISCRIMINATOR);
            PrismPropertyValue<ObjectSynchronizationDiscriminatorType> discriminator =
                    ExpressionUtil.evaluateExpression(
                            variables,
                            discriminatorDef,
                            expression,
                            syncCtx.getExpressionProfile(),
                            beans.expressionFactory,
                            desc,
                            task,
                            result);
            return getRealValue(discriminator);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }

    /**
     * Applies the sorter result (i.e. the computed discriminator):
     *
     * - sets the situation
     * - sets the correlated owner
     * - sets the "force intent change" flag, forcing the (fresh) intent to be written to the shadow - TODO ok?
     */
    private void applySorterResult(@NotNull ObjectSynchronizationDiscriminatorType sorterResult) {
        syncCtx.setForceIntentChange(true);
        LOGGER.trace("Setting synchronization situation to synchronization context: {}",
                sorterResult.getSynchronizationSituation());
        syncCtx.setSituation(sorterResult.getSynchronizationSituation());

        // TODO This is dubious: How could be the linked owner set in syncCtx at this moment?
        if (syncCtx.isShadowAlreadyLinked()) {
            LOGGER.trace("Setting linked owner in synchronization context: {}", sorterResult.getOwner());
            //noinspection unchecked
            syncCtx.setLinkedOwner((F) sorterResult.getOwner());
        }

        LOGGER.trace("Setting correlated owner in synchronization context: {}", sorterResult.getOwner());
        //noinspection unchecked
        syncCtx.setCorrelatedOwner((F) sorterResult.getOwner());
    }
}
