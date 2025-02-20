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

import java.util.*;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlation.CorrelationPropertyDefinition;
import com.evolveum.midpoint.model.api.correlator.Confidence.PerItemConfidence;
import com.evolveum.midpoint.model.api.correlator.CorrelationExplanation;
import com.evolveum.midpoint.model.api.correlator.ItemsCorrelationExplanation;
import com.evolveum.midpoint.model.api.correlator.*;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.PathKeyedMap;

import com.evolveum.midpoint.schema.SchemaService;
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
public class ItemsCorrelator extends BaseCorrelator<ItemsCorrelatorType> {

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
    protected @NotNull Confidence checkCandidateOwnerInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull FocusType candidateOwner,
            @NotNull OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        return new CheckCandidateOperation<>(correlationContext, candidateOwner)
                .execute(result);
    }

    @Override
    public @NotNull Collection<CorrelationPropertyDefinition> getCorrelationPropertiesDefinitions(
            @Nullable PrismObjectDefinition<? extends FocusType> focusDefinition,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ConfigurationException {
        PathKeyedMap<CorrelationPropertyDefinition> properties = new PathKeyedMap<>();
        for (var itemBean : configurationBean.getItem()) {
            var def = CorrelationPropertyDefinition.fromConfiguration(
                    itemBean, focusDefinition != null ? focusDefinition.getComplexTypeDefinition() : null);
            properties.put(def.getItemPath(), def);
        }
        return properties.values();
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
                        correlationContext.getFocusContainerableType(),
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
        public @NotNull Confidence getConfidence(Containerable candidate, Task task, OperationResult result)
                throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException {
            PathKeyedMap<Double> itemConfidencesMap = new PathKeyedMap<>();
            double overallConfidence = 1.0;
            for (CorrelationItem item : correlationItems.getItems()) {
                double itemConfidence = item.computeConfidence(candidate, task, result);
                itemConfidencesMap.put(item.getItemPath(), itemConfidence);
                overallConfidence *= itemConfidence;
            }
            LOGGER.trace("Overall confidence for {}: {}", candidate, overallConfidence);
            return PerItemConfidence.of(overallConfidence, itemConfidencesMap);
        }
    }

    private class CorrelationOperation<C extends Containerable> extends CorrelationLikeOperation {

        CorrelationOperation(@NotNull CorrelationContext correlationContext) throws ConfigurationException {
            super(correlationContext);
        }

        CorrelationResult execute(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            Collection<C> candidates = findCandidates(result);
            return createResult(candidates, this, task, result);
        }

        private @NotNull Collection<C> findCandidates(OperationResult result)
                throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException {

            ObjectQuery query = createQuery(result);
            Collection<C> allCandidates = query != null ? executeQuery(query, result) : List.of();

            LOGGER.debug("Found {} owner candidates for {} using {} correlation item(s) in {}: {}",
                    allCandidates.size(), correlationContext.getPrimaryCorrelatedObject(),
                    correlationItems.size(), contextDescription,
                    lazy(() -> PrettyPrinter.prettyPrint(allCandidates, 3)));

            return allCandidates;
        }

        private Collection<C> executeQuery(ObjectQuery query, OperationResult result) throws SchemaException {
            Collection<? extends Containerable> candidatePool = correlationContext.getCandidatePool();
            if (candidatePool != null) {
                return executeQueryInMemory(candidatePool, query);
            } else {
                return executeQueryInRepo(query, result);
            }
        }

        private Collection<C> executeQueryInMemory(Collection<? extends Containerable> candidatePool, ObjectQuery query)
                throws SchemaException {
            LOGGER.trace("Executing a query in memory against {} object(s):\n{}",
                    candidatePool.size(), query.debugDumpLazily(1));
            MatchingRuleRegistry matchingRuleRegistry = SchemaService.get().matchingRuleRegistry();
            List<C> list = new ArrayList<>();
            for (Containerable candidate : candidatePool) {
                if (query.getFilter().match(candidate.asPrismContainerValue(), matchingRuleRegistry)) {
                    //noinspection unchecked
                    list.add((C) candidate);
                }
            }
            return list;
        }

        private @NotNull Set<C> executeQueryInRepo(ObjectQuery query, OperationResult gResult) throws SchemaException {
            Set<C> candidates = new HashSet<>();
            LOGGER.trace("Using the following query to find owner candidates:\n{}", query.debugDumpLazily(1));
            // TODO use read-only option in the future (but is it OK to start a clockwork with immutable object?)
            //noinspection unchecked
            beans.cacheRepositoryService.searchObjectsIterative(
                    (Class<ObjectType>) correlationContext.getFocusType(),
                    query,
                    (prismObject, lResult) -> {
                        // not providing own operation result, as the processing is minimal here
                        //noinspection unchecked
                        candidates.add((C) prismObject.asObjectable());
                        if (candidates.size() > MAX_CANDIDATES) {
                            // TEMPORARY
                            throw new SystemException("Maximum number of candidate focus objects was exceeded: " + MAX_CANDIDATES);
                        }
                        return true;
                    },
                    createRetrieveCollection(),
                    true,
                    gResult);
            return candidates;
        }

    }

    private class CheckCandidateOperation<F extends FocusType> extends CorrelationLikeOperation {

        private final F candidateOwner;

        CheckCandidateOperation(@NotNull CorrelationContext correlationContext, @NotNull F candidateOwner)
                throws ConfigurationException {
            super(correlationContext);
            this.candidateOwner = candidateOwner;
        }

        Confidence execute(@NotNull OperationResult result)
                throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException {

            boolean matches = checkCandidateOwner(candidateOwner, result);

            LOGGER.debug("Does candidate owner {} for {} using {} correlation item(s) in {} match: {}",
                    candidateOwner, correlationContext.getPrimaryCorrelatedObject(),
                    correlationItems.size(), contextDescription, matches);

            if (matches) {
                return determineConfidence(candidateOwner, this, task, result);
            } else {
                return Confidence.zero();
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

            Confidence confidence = checkCandidateOwnerInternal(correlationContext, candidateOwner, result);
            return new ItemsCorrelationExplanation(
                    correlatorContext.getConfiguration(),
                    confidence);
        }
    }
}
