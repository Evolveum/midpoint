/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.filter;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectables;
import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.api.correlator.Correlator;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * A correlator based on a filter that matches focal object(s) to given resource object.
 * (This is the most usual approach to correlation; and the only one - besides so-called synchronization sorter -
 * before midPoint 4.5.)
 */
class FilterCorrelator implements Correlator {

    private static final Trace LOGGER = TraceManager.getTrace(FilterCorrelator.class);

    /**
     * Configuration of the correlator.
     */
    @NotNull private final FilterCorrelatorType configuration;

    /** Useful beans. */
    @NotNull private final ModelBeans beans;

    FilterCorrelator(@NotNull FilterCorrelatorType configuration, @NotNull ModelBeans beans) {
        this.configuration = configuration;
        this.beans = beans;
        LOGGER.trace("Instantiated the correlator with the configuration:\n{}", configuration.debugDumpLazily(1));
    }

    @Override
    public CorrelationResult correlate(
            @NotNull CorrelationContext correlationContext,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        LOGGER.trace("Correlating:\n{}", correlationContext.debugDumpLazily(1));

        return new Correlation<>(correlationContext)
                .execute(result);
    }

    @Override
    public void resolve(
            @NotNull CaseType aCase,
            @NotNull String outcomeUri,
            @NotNull Task task,
            @NotNull OperationResult result) {
        // This correlator should never create any correlation cases.
        throw new IllegalStateException("The resolve() method should not be called for this correlator");
    }

    private class Correlation<F extends FocusType> {

        @NotNull private final ShadowType resourceObject;
        @NotNull private final CorrelationContext correlationContext;
        @NotNull private final Task task;
        @NotNull private final String contextDescription;
        /** TODO: determine from the resource */
        @Nullable private final ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();

        Correlation(@NotNull CorrelationContext correlationContext) {
            this.resourceObject = correlationContext.getResourceObject();
            this.correlationContext = correlationContext;
            this.task = correlationContext.getTask();
            this.contextDescription =
                    ("filter correlator" +
                            (configuration.getName() != null ? " '" + configuration.getName() + "'" : ""))
                            + " for " + correlationContext.getObjectTypeDefinition().getHumanReadableName()
                            + " in " + correlationContext.getResource();
        }

        public CorrelationResult execute(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            List<F> candidates = findCandidatesUsingConditionalFilters(result);
            List<F> confirmedCandidates = confirmCandidates(candidates, result);
            // TODO selection expression

            return beans.builtInResultCreator.createCorrelationResult(confirmedCandidates, correlationContext);
        }

        private @NotNull List<F> findCandidatesUsingConditionalFilters(OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {

            List<ConditionalSearchFilterType> conditionalFilters = configuration.getOwnerFilter();

            LOGGER.trace("Going to find candidates using {} conditional filter(s) in {}",
                    conditionalFilters.size(), contextDescription);

            if (conditionalFilters.isEmpty()) {
                throw new ConfigurationException("No filters specified in " + contextDescription);
            }

            List<F> allCandidates = new ArrayList<>();
            for (ConditionalSearchFilterType conditionalFilter : conditionalFilters) {
                if (isConditionSatisfied(conditionalFilter, result)) {
                    CorrelatorUtil.addCandidates(
                            allCandidates,
                            findCandidatesUsingFilter(conditionalFilter, result),
                            LOGGER);
                }
            }

            LOGGER.debug("Found {} owner candidates for {} using {} conditional filter(s) in {}: {}",
                    allCandidates.size(), resourceObject, conditionalFilters.size(), contextDescription,
                    lazy(() -> PrettyPrinter.prettyPrint(allCandidates, 3)));

            return allCandidates;
        }

        private boolean isConditionSatisfied(ConditionalSearchFilterType conditionalFilter, OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {
            ExpressionType condition = conditionalFilter.getCondition();
            if (condition == null) {
                LOGGER.trace("No filter condition specified, will use the filter automatically");
                return true;
            } else {
                boolean value = ExpressionUtil.evaluateConditionDefaultFalse(
                        getVariablesMap(null), condition, expressionProfile, beans.expressionFactory,
                        "filter condition in " + contextDescription, task, result);
                LOGGER.trace("Condition {} in correlation filter evaluated to {}", condition, value);
                return value;
            }
        }

        @NotNull private List<F> findCandidatesUsingFilter(
                SearchFilterType conditionalFilter, OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {
            if (!conditionalFilter.containsFilterClause()) {
                throw new ConfigurationException("No filter clause in: " + contextDescription);
            }

            ObjectQuery query;
            try {
                ObjectQuery rawQuery = PrismContext.get().getQueryConverter()
                        .createObjectQuery(correlationContext.getFocusType(), conditionalFilter);
                query = evaluateQueryExpressions(rawQuery, result);
            } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException |
                    ConfigurationException | SecurityViolationException e) {
                // Logging here to provide information about failing query
                LoggingUtils.logException(LOGGER, "Couldn't convert correlation query in {}\n{}.", e,
                        contextDescription, SchemaDebugUtil.prettyPrint(conditionalFilter));
                MiscUtil.throwAsSame(e,
                        "Couldn't convert correlation query in: " + contextDescription + ": " + e.getMessage());
                throw e; // just to make compiler happy (exception is thrown in the above statement)
            }

            LOGGER.trace("Using the following query to find owner candidates:\n{}", query.debugDumpLazily(1));
            // TODO use read-only option in the future (but is it OK to start a clockwork with immutable object?)
            //noinspection unchecked
            return asObjectables(
                    beans.cacheRepositoryService
                            .searchObjects((Class<F>) correlationContext.getFocusType(), query, null, result));
        }

        private ObjectQuery evaluateQueryExpressions(ObjectQuery origQuery, OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {

            if (origQuery.getFilter() == null) {
                LOGGER.trace("No filter provided, skipping updating filter (strange but may happen)");
                return origQuery;
            } else {
                LOGGER.trace("Evaluating query expression(s)");
                return ExpressionUtil.evaluateQueryExpressions(origQuery, getVariablesMap(null), expressionProfile,
                        beans.expressionFactory, PrismContext.get(), contextDescription, task, result);
            }
        }

        private List<F> confirmCandidates(List<F> candidates, OperationResult result)
                throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
                ConfigurationException, SecurityViolationException {
            if (configuration.getConfirmation() == null) {
                LOGGER.trace("No confirmation expression");
                return candidates;
            }
            if (candidates.size() == 1 && Boolean.FALSE.equals(configuration.isUseConfirmationForSingleCandidate())) {
                LOGGER.trace("Single candidate and useConfirmationForSingleCandidate is FALSE -> skipping confirmation");
                return candidates;
            }
            List<F> confirmedCandidates = new ArrayList<>();
            for (F candidate : candidates) {
                try {
                    LOGGER.trace("Going to confirm candidate owner {}", candidate);
                    boolean isConfirmed = evaluateConfirmationExpression(candidate, result);
                    if (isConfirmed) {
                        LOGGER.trace("Confirmed");
                        confirmedCandidates.add(candidate);
                    } else {
                        LOGGER.trace("Not confirmed");
                    }
                } catch (Exception e) {
                    MiscUtil.throwAsSame(e, "Couldn't confirm candidate owner: " + e.getMessage());
                    throw e; // just to make compiler happy
                }
            }

            LOGGER.debug("Confirmed {} candidate owner(s) out of {} originally found one(s): {}",
                    confirmedCandidates.size(), candidates.size(),
                    lazy(() -> PrettyPrinter.prettyPrint(confirmedCandidates, 3)));

            return confirmedCandidates;
        }

        private boolean evaluateConfirmationExpression(F candidate, OperationResult result)
                throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
                ConfigurationException, SecurityViolationException {
            String shortDesc = "confirmation expression for " + contextDescription;

            PrismPropertyDefinition<Boolean> outputDefinition =
                    PrismContext.get().definitionFactory().createPropertyDefinition(
                            ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_BOOLEAN);
            Expression<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> expression =
                    beans.expressionFactory.makeExpression(
                            configuration.getConfirmation(), outputDefinition, expressionProfile, shortDesc, task, result);

            VariablesMap variables = getVariablesMap(candidate);
            ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, variables, shortDesc, task);
            PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = ModelExpressionThreadLocalHolder
                    .evaluateExpressionInContext(expression, params, task, result);
            Set<Boolean> values = ExpressionUtil.getUniqueNonNullRealValues(outputTriple);
            if (values.isEmpty()) {
                throw new ExpressionEvaluationException("Expression returned no value in " + shortDesc);
            } else if (values.size() > 1) {
                throw new ExpressionEvaluationException("Expression returned multiple values (" + values + ") in " + shortDesc);
            } else {
                return values.iterator().next();
            }
        }

        /**
         * Creates variables for evaluation of an expression (in condition, in query, or confirmation).
         */
        @NotNull
        private VariablesMap getVariablesMap(ObjectType focus) {
            return CorrelatorUtil.getVariablesMap(focus, resourceObject, correlationContext);
        }
    }
}
