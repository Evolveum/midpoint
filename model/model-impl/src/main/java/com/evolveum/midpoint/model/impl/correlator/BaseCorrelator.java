/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.api.correlator.Correlator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;

/**
 * Abstract superclass for non-trivial built-in correlators.
 *
 * @param <CCB> correlator configuration bean
 */
public abstract class BaseCorrelator<CCB extends AbstractCorrelatorType> implements Correlator {

    private static final String OP_CORRELATE_SUFFIX = ".correlate";

    /** Correlator-specific logger. */
    @NotNull private final Trace logger;

    /** Correlator type name (like "filter", "expression", etc) - for diagnostics purposes. */
    @NotNull protected final String typeName;

    /** Correlator [instantiation] context. */
    @NotNull protected final CorrelatorContext<CCB> correlatorContext;

    /** Configuration of the correlator. */
    @NotNull protected final CCB configurationBean;

    /** Useful beans. */
    @NotNull protected final ModelBeans beans;

    // Temporary. This should be configurable.
    protected static final int MAX_CANDIDATES = 100;

    protected BaseCorrelator(
            @NotNull Trace logger,
            @NotNull String typeName,
            @NotNull CorrelatorContext<CCB> correlatorContext,
            @NotNull ModelBeans beans) {
        this.logger = logger;
        this.typeName = typeName;
        this.correlatorContext = correlatorContext;
        this.configurationBean = correlatorContext.getConfigurationBean();
        this.beans = beans;
        logger.trace("Instantiating the correlator with the context:\n{}", correlatorContext.debugDumpLazily(1));
    }

    @Override
    public @NotNull CorrelationResult correlate(
            @NotNull CorrelationContext correlationContext, @NotNull OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        OperationResult result = parentResult.subresult(getClass().getName() + OP_CORRELATE_SUFFIX)
                .build();
        try {
            logger.trace("Correlating:\n{}", correlationContext.debugDumpLazily(1));

            CorrelationResult correlationResult = correlateInternal(correlationContext, result);

            logger.trace("Result:\n{}", correlationResult.debugDumpLazily(1));

            result.addArbitraryObjectAsReturn("correlationResult", correlationResult);

            return correlationResult;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    protected abstract @NotNull CorrelationResult correlateInternal(
            @NotNull CorrelationContext correlationContext, @NotNull OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException;

    protected @NotNull String getDefaultContextDescription(@NotNull CorrelationContext correlationContext) {
        return (typeName + " correlator" +
                (configurationBean.getName() != null ? " '" + configurationBean.getName() + "'" : ""))
                + " for " + correlationContext.getObjectTypeDefinition().getHumanReadableName()
                + " in " + correlationContext.getResource();
    }
}
