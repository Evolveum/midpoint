/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import static com.evolveum.midpoint.schema.GetOperationOptions.createRetrieveCollection;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelationExplanation;
import com.evolveum.midpoint.model.api.correlator.ItemsCorrelationExplanation;
import com.evolveum.midpoint.model.api.correlator.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.BaseCorrelator;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsCorrelatorType;

/**
 * A "user-friendly" correlator based on a list of items that need to be matched between the source
 * (usually the pre-focus, but a shadow is acceptable here as well), and the target (set of focal objects).
 */
class ItemsCorrelator extends BaseCorrelator<ItemsCorrelatorType> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemsCorrelator.class);

    ItemsCorrelator(@NotNull CorrelatorContext<ItemsCorrelatorType> correlatorContext, @NotNull ModelBeans beans) {
        super(LOGGER, "items", correlatorContext, beans);
    }

    @Override
    public @NotNull CorrelationResult correlateInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return new CorrelationOperation<>(correlationContext)
                .execute(result);
    }

    @Override
    protected @NotNull CorrelationExplanation explainInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull FocusType candidateOwner,
            @NotNull OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        return new ExplanationOperation<>(correlationContext, candidateOwner)
                .execute(result);
    }

    @Override
    protected double checkCandidateOwnerInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull FocusType candidateOwner,
            @NotNull OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        return new CheckCandidateOperation<>(correlationContext, candidateOwner)
                .execute(result);
    }

    private abstract class CorrelationLikeOperation implements ConfidenceValueProvider {

        @NotNull final CorrelationContext correlationContext;
        @NotNull final Task task;
        @NotNull final String contextDescription;
        @NotNull final CorrelationItems correlationItems;

        CorrelationLikeOperation(@NotNull CorrelationContext correlationContext) throws ConfigurationException {
            this.correlationContext = correlationContext;
            this.task = correlationContext.getTask();
            this.contextDescription = getDefaultContextDescription(correlationContext);
            this.correlationItems = createCorrelationItems();
        }

        @NotNull CorrelationItems createCorrelationItems() throws ConfigurationException {
            CorrelationItems items = CorrelationItems.create(correlatorContext, correlationContext);
            configCheck(!items.isEmpty(), "No items specified in %s", contextDescription);
            LOGGER.trace("Going to proceed using {} correlation items(s) in {}",
                    items.size(), contextDescription);
            return items;
        }

        /** Returns `null` if we cannot use the definitions here. */
        @Nullable ObjectQuery createQuery(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            if (areItemsApplicable(correlationItems)) {
                return correlationItems.createIdentityQuery(
                        correlationContext.getFocusType(),
                        correlationContext.getArchetypeOid(),
                        correlationContext.getCandidateOids(),
                        correlationContext.getTask(),
                        result);
            } else {
                return null;
            }
        }

        private boolean areItemsApplicable(CorrelationItems correlationItems) throws SchemaException {
            assert !correlationItems.isEmpty();

            for (CorrelationItem item : correlationItems.getItems()) {
                if (!item.isApplicable()) {
                    LOGGER.trace("Correlation item {} forbids us to use this correlator", item);
                    return false;
                }
            }
            return true;
        }

        @Override
        public Double getConfidence(ObjectType candidate, Task task, OperationResult result)
                throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException {
            double confidence = 1.0;
            for (CorrelationItem item : correlationItems.getItems()) {
                confidence *= item.computeConfidence(candidate, task, result);
            }
            LOGGER.trace("Overall confidence for {}: {}", candidate, confidence);
            return confidence;
        }
    }

    private class CorrelationOperation<F extends FocusType> extends CorrelationLikeOperation {

        CorrelationOperation(@NotNull CorrelationContext correlationContext) throws ConfigurationException {
            super(correlationContext);
        }

        CorrelationResult execute(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            List<F> candidates = findCandidates(result);
            return createResult(candidates, this, task, result);
        }

        private @NotNull List<F> findCandidates(OperationResult result)
                throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException {

            ObjectQuery query = createQuery(result);
            List<F> allCandidates = query != null ? executeQuery(query, result) : List.of();

            LOGGER.debug("Found {} owner candidates for {} using {} correlation item(s) in {}: {}",
                    allCandidates.size(), correlationContext.getPrimaryCorrelatedObject(),
                    correlationItems.size(), contextDescription,
                    lazy(() -> PrettyPrinter.prettyPrint(allCandidates, 3)));

            return allCandidates;
        }

        private List<F> executeQuery(ObjectQuery query, OperationResult gResult) throws SchemaException {
            List<F> candidates = new ArrayList<>();
            LOGGER.trace("Using the following query to find owner candidates:\n{}", query.debugDumpLazily(1));
            // TODO use read-only option in the future (but is it OK to start a clockwork with immutable object?)
            //noinspection unchecked
            beans.cacheRepositoryService.searchObjectsIterative(
                    (Class<F>) correlationContext.getFocusType(),
                    query,
                    (object, lResult) -> addToCandidates(object.asObjectable(), candidates),
                    createRetrieveCollection(),
                    true,
                    gResult);
            return candidates;
        }

        private boolean addToCandidates(F object, List<F> candidates) {
            if (candidates.stream()
                    .noneMatch(candidate -> candidate.getOid().equals(object.getOid()))) {
                candidates.add(object);
                if (candidates.size() > MAX_CANDIDATES) {
                    // TEMPORARY
                    throw new SystemException("Maximum number of candidate focus objects was exceeded: " + MAX_CANDIDATES);
                }
            }
            return true;
        }
    }

    private class CheckCandidateOperation<F extends FocusType> extends CorrelationLikeOperation {

        private final F candidateOwner;

        CheckCandidateOperation(@NotNull CorrelationContext correlationContext, @NotNull F candidateOwner)
                throws ConfigurationException {
            super(correlationContext);
            this.candidateOwner = candidateOwner;
        }

        double execute(@NotNull OperationResult result)
                throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException {

            boolean matches = checkCandidateOwner(candidateOwner, result);

            LOGGER.debug("Does candidate owner {} for {} using {} correlation item(s) in {} match: {}",
                    candidateOwner, correlationContext.getPrimaryCorrelatedObject(),
                    correlationItems.size(), contextDescription, matches);

            if (matches) {
                return determineConfidence(candidateOwner, this, task, result);
            } else {
                return 0;
            }
        }

        private boolean checkCandidateOwner(F candidateOwner, OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            ObjectQuery query = createQuery(result);
            return query != null && candidateOwnerMatches(query, candidateOwner);
        }

        private boolean candidateOwnerMatches(ObjectQuery query, F candidateOwner) throws SchemaException {
            LOGGER.trace("Checking the following query:\n{}\nregarding the candidate:\n{}",
                    query.debugDumpLazily(1),
                    candidateOwner.debugDumpLazily(1));
            ObjectFilter filter = query.getFilter();
            return filter == null
                    || filter.match(candidateOwner.asPrismContainerValue(), beans.matchingRuleRegistry);
        }
    }

    private class ExplanationOperation<F extends FocusType> extends CorrelationLikeOperation {

        private final F candidateOwner;

        ExplanationOperation(@NotNull CorrelationContext correlationContext, @NotNull F candidateOwner)
                throws ConfigurationException {
            super(correlationContext);
            this.candidateOwner = candidateOwner;
        }

        /**
         * This is a preliminary implementation. I am not sure if we can provide something more detailed here.
         */
        ItemsCorrelationExplanation execute(@NotNull OperationResult result)
                throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException {

            double confidence = checkCandidateOwnerInternal(correlationContext, candidateOwner, result);
            return new ItemsCorrelationExplanation(
                    correlatorContext.getConfiguration(),
                    confidence);
        }
    }
}
