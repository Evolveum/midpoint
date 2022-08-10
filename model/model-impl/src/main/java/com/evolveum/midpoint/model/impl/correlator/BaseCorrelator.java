/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import com.evolveum.midpoint.model.api.correlator.*;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlation.CorrelatorContextCreator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;

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
    private static final String OP_CHECK_CANDIDATE_OWNER_SUFFIX = ".checkCandidateOwner";

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
        logger.trace("Instantiating the correlator with the context:\n{}", correlatorContext.dumpXmlLazily());
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

    @Override
    public boolean checkCandidateOwner(
            @NotNull CorrelationContext correlationContext,
            @NotNull FocusType candidateOwner,
            @NotNull OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        OperationResult result = parentResult.subresult(getClass().getName() + OP_CHECK_CANDIDATE_OWNER_SUFFIX)
                .build();
        try {
            logger.trace("Checking owner:\n{}\nin context:\n{}",
                    candidateOwner.debugDumpLazily(1),
                    correlationContext.debugDumpLazily(1));

            boolean matches = checkCandidateOwnerInternal(correlationContext, candidateOwner, result);

            logger.trace("Result: {}", matches);

            result.addArbitraryObjectAsReturn("matches", matches);

            return matches;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    protected abstract boolean checkCandidateOwnerInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull FocusType candidateOwner,
            @NotNull OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException;

    protected @NotNull String getDefaultContextDescription(@NotNull CorrelationContext correlationContext) {
        return (typeName + " correlator" +
                (configurationBean.getName() != null ? " '" + configurationBean.getName() + "'" : ""))
                + " for " + correlationContext.getResourceObjectDefinition().getHumanReadableName()
                + " in " + correlationContext.getResource();
    }

    protected @NotNull Correlator instantiateChild(
            @NotNull CorrelatorConfiguration childConfiguration,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException, ConfigurationException {
        CorrelatorContext<?> childContext = CorrelatorContextCreator.createChildContext(
                childConfiguration,
                correlatorContext.getCorrelationDefinitionBean(),
                correlatorContext.getIdentityManagementConfiguration(),
                correlatorContext.getIndexingConfiguration(),
                correlatorContext.getSystemConfiguration());
        return beans.correlatorFactoryRegistry
                .instantiateCorrelator(childContext, task, result);
    }
}
