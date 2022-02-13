/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MatchingUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.prism.PrismObject.asObjectable;

/**
 * Evaluates "pre-mappings" i.e. inbound mappings that are evaluated before the actual clockwork is run.
 * (This is currently done to simplify the correlation process.)
 *
 * FIXME only a fake implementation for now
 */
class PreMappingsEvaluation<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(PreMappingsEvaluation.class);

    @NotNull private final SynchronizationContext<F> syncCtx;
    @NotNull private final F preFocus;
    @NotNull private final ModelBeans beans;

    PreMappingsEvaluation(@NotNull SynchronizationContext<F> syncCtx, @NotNull ModelBeans beans) throws SchemaException {
        this.syncCtx = syncCtx;
        this.preFocus = syncCtx.getPreFocus();
        this.beans = beans;
    }

    /**
     * We simply copy matching attributes from the resource object to the focus.
     */
    public void evaluate(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {

        ObjectSynchronizationType config = syncCtx.getObjectSynchronizationBean();
        if (config == null || config.getPreMappingsSimulation() == null) {
            LOGGER.trace("No pre-mapping simulation specified, doing simple 'copy attributes' operation");
            try {
                MatchingUtil.copyAttributes(preFocus, syncCtx.getShadowedResourceObject().asObjectable());
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute pre-mapping demo replacement, continuing without it", t);
            }
        } else {
            LOGGER.trace("Evaluating pre-mapping simulation expression");
            evaluateSimulationExpression(config.getPreMappingsSimulation(), result);
        }

        LOGGER.info("Pre-focus:\n{}", preFocus.debugDumpLazily(1));
    }

    private void evaluateSimulationExpression(ExpressionType simulationExpressionBean, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        VariablesMap variables = getVariablesMap();
        Task task = syncCtx.getTask();
        ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
        String contextDesc = "pre-mapping simulation";

        Expression<PrismValue, ItemDefinition<?>> expression =
                beans.expressionFactory.makeExpression(
                        simulationExpressionBean, null, expressionProfile, contextDesc, task, result);

        ExpressionEvaluationContext params =
                new ExpressionEvaluationContext(null, variables, contextDesc, task);
        ModelExpressionThreadLocalHolder.evaluateAnyExpressionInContext(expression, params, task, result);
    }

    public VariablesMap getVariablesMap() throws SchemaException {
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(
                syncCtx.getPreFocus(),
                syncCtx.getShadowedResourceObject().asObjectable(),
                asObjectable(syncCtx.getResource()),
                asObjectable(syncCtx.getSystemConfiguration()));
        variables.put(ExpressionConstants.VAR_SYNCHRONIZATION_CONTEXT, syncCtx, CorrelationContext.class);
        return variables;
    }
}
