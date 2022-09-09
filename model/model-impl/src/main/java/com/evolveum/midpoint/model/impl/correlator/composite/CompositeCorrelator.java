/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.composite;

import static com.evolveum.midpoint.model.api.correlator.CorrelatorConfiguration.getChildConfigurations;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.api.correlator.CompositeCorrelationExplanation;
import com.evolveum.midpoint.model.api.correlator.CompositeCorrelationExplanation.ChildCorrelationExplanationRecord;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelationExplanation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.*;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.BaseCorrelator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectSet;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CompositeCorrelatorScalingDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CompositeCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Composite correlator that evaluates its components (child correlators) and builds up the result according to their results.
 *
 * TODO ignore identifiers in owner options
 */
class CompositeCorrelator extends BaseCorrelator<CompositeCorrelatorType> {

    private static final double DEFAULT_SCALE = 1.0;

    private static final Trace LOGGER = TraceManager.getTrace(CompositeCorrelator.class);

    CompositeCorrelator(@NotNull CorrelatorContext<CompositeCorrelatorType> correlatorContext, @NotNull ModelBeans beans) {
        super(LOGGER, "composite", correlatorContext, beans);
    }

    @Override
    public @NotNull CorrelationResult correlateInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return new CorrelationOperation(correlationContext)
                .execute(result);
    }

    @Override
    protected @NotNull CorrelationExplanation explainInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull FocusType candidateOwner,
            @NotNull OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        return new ExplanationOperation(correlationContext, candidateOwner)
                .execute(result);
    }

    @Override
    protected double checkCandidateOwnerInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull FocusType candidateOwner,
            @NotNull OperationResult result) throws ConfigurationException, SchemaException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ObjectNotFoundException {
        return new CandidateCheckOperation(correlationContext, candidateOwner)
                .execute(result);
    }

    private abstract class CorrelationLikeOperation {

        @NotNull final CorrelationContext correlationContext;

        @NotNull final Task task;

        @NotNull private final String contextDescription; // TODO

        @NotNull private final Collection<CorrelatorConfiguration> childConfigurations;

        @NotNull final Set<Integer> tiers;

        final double definiteThreshold = correlatorContext.getDefiniteThreshold();

        final double scale = getScale();

        /** What candidate owners were matched by what correlators (only with non-null names). */
        @NotNull final HashMultimap<String, String> candidateOwnersMatchedBy = HashMultimap.create();

        @NotNull final Set<String> evaluatedCorrelators = new HashSet<>();

        /** Current confidence values - scaled to [0,1]. Keyed by candidate OID. */
        @NotNull final Map<String, Double> currentConfidences = new HashMap<>();

        CorrelationLikeOperation(@NotNull CorrelationContext correlationContext) throws ConfigurationException {
            this.correlationContext = correlationContext;
            this.task = correlationContext.getTask();
            this.contextDescription = getDefaultContextDescription(correlationContext);
            this.childConfigurations = getChildConfigurations(correlatorContext.getConfigurationBean());
            Supplier<TreeSet<Integer>> treeSetSupplier = () -> new TreeSet<>(
                    Comparator.nullsLast(Comparator.naturalOrder()));
            this.tiers = childConfigurations.stream()
                    .map(CorrelatorConfiguration::getTier)
                    .collect(
                            Collectors.toCollection(treeSetSupplier));
            LOGGER.trace("Tiers: {}", tiers);
            CorrelatorConfiguration.computeDependencyLayers(childConfigurations);
        }

        List<CorrelatorConfiguration> getConfigurationsInTierSorted(Integer tier) {
            return childConfigurations.stream()
                    .filter(c -> Objects.equals(c.getTier(), tier))
                    .sorted(
                            Comparator.comparing(
                                            CorrelatorConfiguration::getOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                                    .thenComparing(
                                            CorrelatorConfiguration::getDependencyLayer))
                    .collect(Collectors.toList());
        }

        void registerEvaluationAndCheckThatParentsWereEvaluated(CorrelatorConfiguration childConfiguration)
                throws ConfigurationException {
            String childCorrelatorName = childConfiguration.getName();
            if (childCorrelatorName != null) {
                evaluatedCorrelators.add(childCorrelatorName);
            }
            Set<String> ignoreIfMatchedBy = childConfiguration.getIgnoreIfMatchedBy();
            Set<String> parentsNotEvaluatedYet = Sets.difference(ignoreIfMatchedBy, evaluatedCorrelators);
            if (!parentsNotEvaluatedYet.isEmpty()) {
                throw new ConfigurationException(
                        String.format("Correlator '%s' depends on '%s' that has not executed yet. Please check the "
                                + "distribution into tiers and the ordering.", childConfiguration, parentsNotEvaluatedYet));
            }
        }

        @NotNull Set<String> registerMatchAndCheckIfIgnored(CorrelatorConfiguration childConfiguration, String candidateOwnerOid) {
            String childCorrelatorName = childConfiguration.getName();
            if (childCorrelatorName != null) {
                candidateOwnersMatchedBy.put(candidateOwnerOid, childCorrelatorName);
            }
            return Sets.intersection(
                    childConfiguration.getIgnoreIfMatchedBy(),
                    candidateOwnersMatchedBy.get(candidateOwnerOid));
        }

        double increaseConfidenceIfNeeded(
                String candidateOwnerOid, double confidenceFromChild, double weight, Set<String> ignoredBecause) {
            if (!ignoredBecause.isEmpty()) {
                LOGGER.trace("Not increasing the confidence because matched by: {}", ignoredBecause);
                return 0;
            } else {
                double increment = confidenceFromChild * weight / scale;
                double newValue = currentConfidences.compute(
                        candidateOwnerOid,
                        (oid, confidenceBefore) ->
                                Math.min(
                                        or0(confidenceBefore) + increment,
                                        1.0));
                LOGGER.trace("Computed confidence increment: {}; new (aggregated) confidence value - potentially cropped: {}",
                        increment, newValue);
                return increment;
            }
        }

        public double getScale() {
            CompositeCorrelatorScalingDefinitionType scaling = configurationBean.getScaling();
            Double scale = scaling != null ? scaling.getScale() : null;
            return Objects.requireNonNullElse(scale, DEFAULT_SCALE);
        }
    }

    private class CorrelationOperation extends CorrelationLikeOperation {

        @NotNull private final ObjectSet<ObjectType> allCandidates = new ObjectSet<>();

        @NotNull private final Map<String, String> externalIds = new HashMap<>();

        CorrelationOperation(@NotNull CorrelationContext correlationContext) throws ConfigurationException {
            super(correlationContext);
        }

        public CorrelationResult execute(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {

            LOGGER.trace("Starting correlation operation with scale = {}, 'definite' threshold = {}", scale, definiteThreshold);

            int tierNumber = 1;
            for (Integer tier : tiers) {
                List<CorrelatorConfiguration> childConfigurations = getConfigurationsInTierSorted(tier);
                LOGGER.trace("Processing tier #{}/{} with ID '{}': {} correlator(s)",
                        tierNumber, tiers.size(), tier, childConfigurations.size());

                for (CorrelatorConfiguration childConfiguration : childConfigurations) {
                    correlateInChild(childConfiguration, result);
                }

                Collection<String> certainOwners = getCertainOwners();
                if (certainOwners.size() == 1) {
                    CorrelationResult correlationResult = createCorrelationResult();
                    LOGGER.trace("Reached 'existing owner' answer in tier {}, finishing:\n{}",
                            tier, correlationResult.debugDumpLazily(1));
                    return correlationResult;
                }
            }
            CorrelationResult correlationResult = createCorrelationResult();
            LOGGER.trace("Finishing after last tier was evaluated:\n{}", correlationResult.debugDumpLazily(1));
            return correlationResult;
        }

        private void correlateInChild(CorrelatorConfiguration childConfiguration, OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {

            LOGGER.trace("Going to correlate in child correlator '{}'", childConfiguration);
            CorrelationResult childResult =
                    instantiateChild(childConfiguration, task, result)
                            .correlate(correlationContext, result);
            LOGGER.trace("Child correlator '{}' provided the following result:\n{}",
                    childConfiguration, childResult.debugDumpLazily(1));

            registerEvaluationAndCheckThatParentsWereEvaluated(childConfiguration);

            CandidateOwnersMap childCandidateOwnersMap =
                    MiscUtil.requireNonNull(
                            childResult.getCandidateOwnersMap(),
                            () -> new IllegalStateException(
                                    String.format("No candidate owner map obtained from the child correlator: %s",
                                            childConfiguration.identify())));

            for (CandidateOwner candidateOwner : childCandidateOwnersMap.values()) {
                String candidateOwnerOid = candidateOwner.getOid();

                LOGGER.trace("Considering candidate owner {}", candidateOwner);
                allCandidates.add(candidateOwner.getObject());
                registerExternalId(candidateOwner);

                Set<String> ignoredBecause = registerMatchAndCheckIfIgnored(childConfiguration, candidateOwnerOid);
                increaseConfidenceIfNeeded(
                        candidateOwner.getOid(), candidateOwner.getConfidence(), childConfiguration.getWeight(), ignoredBecause);
            }
        }

        private void registerExternalId(CandidateOwner candidateOwner) {
            String externalId = candidateOwner.getExternalId();
            if (externalId != null) {
                LOGGER.trace("Registering external ID: {}", externalId);
                String existing = externalIds.put(candidateOwner.getOid(), externalId);
                if (existing != null && !existing.equals(externalId)) {
                    throw new UnsupportedOperationException(
                            String.format(
                                    "Multiple external IDs are not supported: %s for %s", existing, candidateOwner));
                }
            }
        }

        private CorrelationResult createCorrelationResult() {
            CandidateOwnersMap candidateOwnersMap = new CandidateOwnersMap();
            currentConfidences.forEach(
                    (oid, confidence) ->
                            candidateOwnersMap.put(
                                    allCandidates.get(oid),
                                    externalIds.get(oid),
                                    confidence));
            return CorrelationResult.of(candidateOwnersMap);
        }

        private Collection<String> getCertainOwners() {
            return currentConfidences.entrySet().stream()
                    .filter(e -> e.getValue() >= definiteThreshold)
                    .map(e -> e.getKey())
                    .collect(Collectors.toSet());
        }
    }

    private class ExplanationOperation extends CorrelationLikeOperation {

        @NotNull private final FocusType candidateOwner;
        @NotNull private final String candidateOwnerOid;
        @NotNull private final List<ChildCorrelationExplanationRecord> childRecords = new ArrayList<>();

        ExplanationOperation(@NotNull CorrelationContext correlationContext, @NotNull FocusType candidateOwner)
                throws ConfigurationException {
            super(correlationContext);
            this.candidateOwner = candidateOwner;
            this.candidateOwnerOid = Objects.requireNonNull(candidateOwner.getOid());
        }

        CorrelationExplanation execute(@NotNull OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {

            LOGGER.trace("Starting explanation operation with scale = {}, 'definite' threshold = {}", scale, definiteThreshold);

            int tierNumber = 1;
            for (Integer tier : tiers) {
                List<CorrelatorConfiguration> childConfigurations = getConfigurationsInTierSorted(tier);
                LOGGER.trace("Processing tier #{}/{} with ID '{}': {} correlator(s)",
                        tierNumber, tiers.size(), tier, childConfigurations.size());

                for (CorrelatorConfiguration childConfiguration : childConfigurations) {
                    explainInChild(childConfiguration, result);
                }
            }
            CorrelationExplanation explanation = new CompositeCorrelationExplanation(
                    correlatorContext.getConfiguration(),
                    or0(currentConfidences.get(candidateOwnerOid)),
                    childRecords);
            LOGGER.trace("Finishing the explanation operation evaluated:\n{}", explanation.debugDumpLazily(1));
            return explanation;
        }

        private void explainInChild(CorrelatorConfiguration childConfiguration, OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {

            LOGGER.trace("Going to explain in child correlator '{}'", childConfiguration);
            CorrelationExplanation childExplanation =
                    instantiateChild(childConfiguration, task, result)
                            .explain(correlationContext, candidateOwner, result);
            LOGGER.trace("Child correlator '{}' provided the following explanation:\n{}",
                    childConfiguration, childExplanation.debugDumpLazily(1));

            registerEvaluationAndCheckThatParentsWereEvaluated(childConfiguration);
            double weight = childConfiguration.getWeight();
            double childConfidence = childExplanation.getConfidence();
            double confidenceIncrement;
            Set<String> ignoredBecause;
            if (childConfidence > 0) {
                ignoredBecause = registerMatchAndCheckIfIgnored(childConfiguration, candidateOwnerOid);
                confidenceIncrement = increaseConfidenceIfNeeded(
                        candidateOwnerOid, childConfidence, weight, ignoredBecause);
            } else {
                ignoredBecause = Set.of();
                confidenceIncrement = 0;
            }
            childRecords.add(
                    new ChildCorrelationExplanationRecord(childExplanation, weight, confidenceIncrement, ignoredBecause));
        }
    }

    private class CandidateCheckOperation extends CorrelationLikeOperation {

        @NotNull private final FocusType candidateOwner;
        @NotNull private final String candidateOwnerOid;

        CandidateCheckOperation(@NotNull CorrelationContext correlationContext, @NotNull FocusType candidateOwner)
                throws ConfigurationException {
            super(correlationContext);
            this.candidateOwner = candidateOwner;
            this.candidateOwnerOid = Objects.requireNonNull(candidateOwner.getOid());
        }

        double execute(@NotNull OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {

            LOGGER.trace("Starting candidate check with scale = {}, 'definite' threshold = {}", scale, definiteThreshold);

            int tierNumber = 1;
            for (Integer tier : tiers) {
                List<CorrelatorConfiguration> childConfigurations = getConfigurationsInTierSorted(tier);
                LOGGER.trace("Processing tier #{}/{} with ID '{}': {} correlator(s)",
                        tierNumber, tiers.size(), tier, childConfigurations.size());

                for (CorrelatorConfiguration childConfiguration : childConfigurations) {
                    checkInChild(childConfiguration, result);
                }
            }
            double resultingConfidence = or0(currentConfidences.get(candidateOwnerOid));
            LOGGER.trace("Finishing the candidate check operation with the resulting confidence of {}", resultingConfidence);
            return resultingConfidence;
        }

        private void checkInChild(CorrelatorConfiguration childConfiguration, OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {

            LOGGER.trace("Going to check the candidate in child correlator '{}'", childConfiguration);
            double childConfidence =
                    instantiateChild(childConfiguration, task, result)
                            .checkCandidateOwner(correlationContext, candidateOwner, result);
            LOGGER.trace("Child correlator '{}' provided the following confidence: {}", childConfiguration, childConfidence);

            registerEvaluationAndCheckThatParentsWereEvaluated(childConfiguration);
            if (childConfidence > 0) {
                double weight = childConfiguration.getWeight();
                Set<String> ignoredBecause = registerMatchAndCheckIfIgnored(childConfiguration, candidateOwnerOid);
                increaseConfidenceIfNeeded(candidateOwnerOid, childConfidence, weight, ignoredBecause);
            }
        }
    }
}
