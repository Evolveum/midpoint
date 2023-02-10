/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelationExplanation;
import com.evolveum.midpoint.model.api.correlator.*;

import com.evolveum.midpoint.model.api.correlator.CorrelationExplanation.GenericCorrelationExplanation;
import com.evolveum.midpoint.model.api.correlator.CorrelationExplanation.UnsupportedCorrelationExplanation;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlation.CorrelatorContextCreator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Abstract superclass for built-in correlators.
 *
 * @param <CCB> correlator configuration bean
 */
public abstract class BaseCorrelator<CCB extends AbstractCorrelatorType> implements Correlator {

    private static final String OP_CORRELATE_SUFFIX = ".correlate";
    private static final String OP_EXPLAIN_SUFFIX = ".explain";
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
    public @NotNull CorrelationExplanation explain(
            @NotNull CorrelationContext correlationContext,
            @NotNull FocusType candidate,
            @NotNull OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        OperationResult result = parentResult.subresult(getClass().getName() + OP_EXPLAIN_SUFFIX)
                .build();
        try {
            logger.trace("Explaining candidate:\n{}\nin context:\n{}",
                    candidate.debugDumpLazily(1),
                    correlationContext.debugDumpLazily(1));

            CorrelationExplanation explanation = explainInternal(correlationContext, candidate, result);

            logger.trace("Determined candidate owner explanation:\n{}", explanation.debugDumpLazily(1));

            result.addArbitraryObjectAsReturn("explanation", explanation);

            return explanation;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /** This the default implementation, to be overridden in subclasses. */
    protected @NotNull CorrelationExplanation explainInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull FocusType candidateOwner,
            @NotNull OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        double confidence;
        try {
            confidence = checkCandidateOwnerInternal(correlationContext, candidateOwner, result);
        } catch (Exception e) {
            logger.debug("Determination of the confidence for candidate owner {} failed, no explanation can be provided",
                    candidateOwner, e);
            return new UnsupportedCorrelationExplanation(correlatorContext.getConfiguration());
        }
        return new GenericCorrelationExplanation(correlatorContext.getConfiguration(), confidence);
    }

    @Override
    public double checkCandidateOwner(
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

            double confidence = checkCandidateOwnerInternal(correlationContext, candidateOwner, result);

            logger.trace("Determined candidate owner confidence: {}", confidence);

            result.addArbitraryObjectAsReturn("confidence", confidence);

            return confidence;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    protected abstract double checkCandidateOwnerInternal(
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
                correlatorContext.getTemplateCorrelationConfiguration(),
                correlatorContext.getSystemConfiguration());
        return beans.correlatorFactoryRegistry
                .instantiateCorrelator(childContext, task, result);
    }

    protected CorrelationResult createResult(
            @NotNull Collection<? extends ObjectType> candidates,
            @Nullable ConfidenceValueProvider confidenceValueProvider,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return CorrelationResult.of(
                createCandidateOwnersMap(candidates, confidenceValueProvider, task, result));
    }

    private CandidateOwnersMap createCandidateOwnersMap(
            @NotNull Collection<? extends ObjectType> candidates,
            @Nullable ConfidenceValueProvider confidenceValueProvider,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        CandidateOwnersMap candidateOwnersMap = new CandidateOwnersMap();
        for (ObjectType candidate : candidates) {
            candidateOwnersMap.put(
                    candidate,
                    null, // no external IDs for the clients of this method
                    determineConfidence(candidate, confidenceValueProvider, task, result));
        }
        return candidateOwnersMap;
    }

    protected double determineConfidence(
            @NotNull ObjectType candidate,
            @Nullable ConfidenceValueProvider confidenceValueProvider,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        var customConfidence = determineConfidenceUsingExpression(candidate, task, result);
        if (customConfidence != null) {
            return customConfidence;
        }
        if (confidenceValueProvider != null) {
            Double customConfidence2 = confidenceValueProvider.getConfidence(candidate, task, result);
            if (customConfidence2 != null) {
                return customConfidence2;
            }
        }
        return 1;
    }

    private Double determineConfidenceUsingExpression(ObjectType candidate, Task task, OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        CorrelationConfidenceDefinitionType confidenceDef = correlatorContext.getConfigurationBean().getConfidence();
        if (confidenceDef == null) {
            return null;
        }
        ExpressionType expressionBean = confidenceDef.getExpression();
        if (expressionBean == null) {
            return null;
        }
        VariablesMap variablesMap = new VariablesMap();
        variablesMap.put(ExpressionConstants.VAR_CANDIDATE, new TypedValue<>(candidate, String.class));
        PrismPropertyDefinition<Double> outputDefinition =
                PrismContext.get().definitionFactory().createPropertyDefinition(
                        ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_DOUBLE);
        PrismValue output = ExpressionUtil.evaluateExpression(
                variablesMap,
                outputDefinition,
                expressionBean,
                MiscSchemaUtil.getExpressionProfile(),
                beans.expressionFactory,
                "confidence expression for " + candidate,
                task,
                result);
        if (output == null) {
            return null;
        } else {
            return JavaTypeConverter.convert(
                    Double.class,
                    output.getRealValue());
        }
    }

    @FunctionalInterface
    protected interface ConfidenceValueProvider {
        Double getConfidence(ObjectType candidate, Task task, OperationResult result)
                throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException;
    }
}
