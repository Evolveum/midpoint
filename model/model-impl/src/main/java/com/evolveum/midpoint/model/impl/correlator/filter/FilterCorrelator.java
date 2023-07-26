/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.filter;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectables;
import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.BaseCorrelator;
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
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectSet;
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
class FilterCorrelator extends BaseCorrelator<FilterCorrelatorType> {

    private static final Trace LOGGER = TraceManager.getTrace(FilterCorrelator.class);

    FilterCorrelator(@NotNull CorrelatorContext<FilterCorrelatorType> correlatorContext, @NotNull ModelBeans beans) {
        super(LOGGER, "filter", correlatorContext, beans);
    }

    @Override
    public @NotNull CorrelationResult correlateInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return new Correlation<>(correlationContext)
                .correlate(result);
    }

    @Override
    protected double checkCandidateOwnerInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull FocusType candidateOwner,
            @NotNull OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        return new Correlation<>(correlationContext)
                .checkCandidateOwner(candidateOwner, result);
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
            this.contextDescription = getDefaultContextDescription(correlationContext);
        }

        public CorrelationResult correlate(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            ObjectSet<F> candidates = findCandidatesUsingConditionalFilters(result);
            ObjectSet<F> confirmedCandidates = confirmCandidates(candidates, result);
            return createResult(confirmedCandidates, null, task, result);
        }

        double checkCandidateOwner(F candidateOwner, @NotNull OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            boolean matches =
                    checkCandidateUsingConditionalFilters(candidateOwner, result)
                            && !confirmCandidates(ObjectSet.of(candidateOwner), result).isEmpty();
            if (matches) {
                return determineConfidence(candidateOwner, null, task, result);
            } else {
                return 0;
            }
        }

        private @NotNull ObjectSet<F> findCandidatesUsingConditionalFilters(OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {

            List<ConditionalSearchFilterType> conditionalFilters = getConditionalFilters();

            ObjectSet<F> allCandidates = new ObjectSet<>();
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

        private boolean checkCandidateUsingConditionalFilters(F candidateOwner, OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {

            List<ConditionalSearchFilterType> conditionalFilters = getConditionalFilters();

            for (ConditionalSearchFilterType conditionalFilter : conditionalFilters) {
                if (isConditionSatisfied(conditionalFilter, result)
                    && checkCandidateUsingFilter(conditionalFilter, candidateOwner, result)) {
                    return true;
                }
            }
            return false;
        }

        @NotNull
        private List<ConditionalSearchFilterType> getConditionalFilters() throws ConfigurationException {
            List<ConditionalSearchFilterType> conditionalFilters = configurationBean.getOwnerFilter();

            LOGGER.trace("Going to find candidates (or check candidate) using {} conditional filter(s) in {}",
                    conditionalFilters.size(), contextDescription);

            if (conditionalFilters.isEmpty()) {
                throw new ConfigurationException("No filters specified in " + contextDescription);
            }
            return conditionalFilters;
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
                        getVariablesMap(correlationContext.getPreFocus()),
                        condition,
                        expressionProfile,
                        beans.expressionFactory,
                        "filter condition in " + contextDescription,
                        task,
                        result);
                LOGGER.trace("Condition {} in correlation filter evaluated to {}", condition, value);
                return value;
            }
        }

        @NotNull private List<F> findCandidatesUsingFilter(
                SearchFilterType conditionalFilter, OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {
            ObjectQuery query = getQuery(conditionalFilter, result);
            // TODO use read-only option in the future (but is it OK to start a clockwork with immutable object?)
            //noinspection unchecked
            return asObjectables(
                    beans.cacheRepositoryService
                            .searchObjects((Class<F>) correlationContext.getFocusType(), query, null, result));
        }

        private boolean checkCandidateUsingFilter(
                SearchFilterType conditionalFilter, F candidateOwner, OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {
            ObjectQuery query = getQuery(conditionalFilter, result);
            return query.getFilter().match(candidateOwner.asPrismContainerValue(), beans.matchingRuleRegistry);
        }

        /** Here we add the archetype-related clause (if needed). */
        private @NotNull ObjectQuery getQuery(SearchFilterType conditionalFilter, OperationResult result)
                throws ConfigurationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
                CommunicationException, SecurityViolationException {
            if (!conditionalFilter.containsFilterClause()) {
                throw new ConfigurationException("No filter clause in: " + contextDescription);
            }

            ObjectQuery query;
            try {
                ObjectQuery rawQuery = PrismContext.get().getQueryConverter()
                        .createObjectQuery(correlationContext.getFocusType(), conditionalFilter);
                ObjectQuery evaluatedQuery = evaluateQueryExpressions(rawQuery, result);
                query = addArchetypeClauseIfNeeded(evaluatedQuery);
            } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException |
                    ConfigurationException | SecurityViolationException e) {
                // Logging here to provide information about failing query
                LoggingUtils.logException(LOGGER, "Couldn't convert correlation query in {}\n{}.", e,
                        contextDescription, SchemaDebugUtil.prettyPrint(conditionalFilter));
                MiscUtil.throwAsSame(e,
                        "Couldn't convert correlation query in: " + contextDescription + ": " + e.getMessage());
                throw e; // just to make compiler happy (exception is thrown in the above statement)
            }

            LOGGER.trace("Using the following query to find/confirm owner candidate(s):\n{}", query.debugDumpLazily(1));
            return query;
        }

        private ObjectQuery evaluateQueryExpressions(ObjectQuery origQuery, OperationResult result)
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {

            if (origQuery.getFilter() == null) {
                LOGGER.trace("No filter provided, skipping updating filter (strange but may happen)");
                return origQuery;
            } else {
                LOGGER.trace("Evaluating query expression(s)");
                return ExpressionUtil.evaluateQueryExpressions(
                        origQuery,
                        getVariablesMap(correlationContext.getPreFocus()),
                        expressionProfile,
                        beans.expressionFactory,
                        contextDescription,
                        task,
                        result);
            }
        }

        private ObjectQuery addArchetypeClauseIfNeeded(ObjectQuery query) {
            String archetypeOid = correlationContext.getArchetypeOid();
            if (archetypeOid == null) {
                return query;
            } else {
                return ObjectQueryUtil.addConjunctions(
                        query,
                        PrismContext.get().queryFor(FocusType.class)
                                .item(FocusType.F_ARCHETYPE_REF).ref(archetypeOid)
                                .buildFilter());
            }
        }

        private ObjectSet<F> confirmCandidates(ObjectSet<F> candidates, OperationResult result)
                throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
                ConfigurationException, SecurityViolationException {
            if (configurationBean.getConfirmation() == null) {
                LOGGER.trace("No confirmation expression");
                return candidates;
            }
            if (candidates.size() == 1 && Boolean.FALSE.equals(configurationBean.isUseConfirmationForSingleCandidate())) {
                LOGGER.trace("Single candidate and useConfirmationForSingleCandidate is FALSE -> skipping confirmation");
                return candidates;
            }
            ObjectSet<F> confirmedCandidates = new ObjectSet<>();
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
                            configurationBean.getConfirmation(), outputDefinition, expressionProfile, shortDesc, task, result);

            // TODO contention for "focus" variable (candidate, pre-focus)
            VariablesMap variables = getVariablesMap(candidate);
            ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(null, variables, shortDesc, task);
            eeContext.setExpressionFactory(beans.expressionFactory);
            PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple =
                    ExpressionUtil.evaluateExpressionInContext(expression, eeContext, task, result);
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
