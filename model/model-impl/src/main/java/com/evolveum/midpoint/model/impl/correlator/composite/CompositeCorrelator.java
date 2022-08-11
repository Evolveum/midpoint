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
 * PRELIMINARY IMPLEMENTATION!
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
        return new Correlation(correlationContext)
                .execute(result);
    }

    @Override
    protected double checkCandidateOwnerInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull FocusType candidateOwner,
            @NotNull OperationResult result) {
        throw new UnsupportedOperationException("CompositeCorrelator is not supported in the 'opportunistic synchronization'"
                + " mode. Please disable this mode for this particular resource or object type.");
    }

    private class Correlation {

        @NotNull private final CorrelationContext correlationContext;
        @NotNull private final Task task;
        @NotNull private final String contextDescription; // TODO

        @NotNull private final Collection<CorrelatorConfiguration> childConfigurations;

        @NotNull private final Set<Integer> tiers;

        private final double ownerThreshold = correlatorContext.getOwnerThreshold();
        private final double scale = getScale();

        /** What candidate owners were matched by what correlators (only with non-null names). */
        @NotNull private final HashMultimap<String, String> candidateOwnersMatchedBy = HashMultimap.create();

        /** Current confidence values - scaled to [0,1]. Keyed by candidate OID. */
        @NotNull private final Map<String, Double> currentConfidences = new HashMap<>();

        @NotNull private final ObjectSet<ObjectType> allCandidates = new ObjectSet<>();

        @NotNull private final Map<String, String> externalIds = new HashMap<>();

        @NotNull private final Set<String> evaluatedCorrelators = new HashSet<>();

        Correlation(@NotNull CorrelationContext correlationContext) {
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
        }

        public CorrelationResult execute(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {

            CorrelatorConfiguration.computeDependencyLayers(childConfigurations);

            LOGGER.trace("Starting composite correlator computation with scale = {}, owner threshold = {}",
                    scale, ownerThreshold);

            int tierNumber = 1;
            for (Integer tier : tiers) {
                List<CorrelatorConfiguration> childConfigurations = getConfigurationsInTierSorted(tier);
                LOGGER.trace("Processing tier #{}/{} with ID '{}': {} correlator(s)",
                        tierNumber, tiers.size(), tier, childConfigurations.size());

                for (CorrelatorConfiguration childConfiguration : childConfigurations) {
                    invokeChildCorrelator(childConfiguration, result);
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
                    .filter(e -> e.getValue() >= ownerThreshold)
                    .map(e -> e.getKey())
                    .collect(Collectors.toSet());
        }

        private List<CorrelatorConfiguration> getConfigurationsInTierSorted(Integer tier) {
            return childConfigurations.stream()
                    .filter(c -> Objects.equals(c.getTier(), tier))
                    .sorted(
                            Comparator.comparing(
                                            CorrelatorConfiguration::getOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                                    .thenComparing(
                                            CorrelatorConfiguration::getDependencyLayer))
                    .collect(Collectors.toList());
        }

        private void invokeChildCorrelator(CorrelatorConfiguration childConfiguration, OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {

            LOGGER.trace("Going to invoke child correlator '{}'", childConfiguration);
            CorrelationResult childResult =
                    instantiateChild(childConfiguration, task, result)
                            .correlate(correlationContext, result);
            LOGGER.trace("Child correlator '{}' provided the following result:\n{}",
                    childConfiguration, childResult.debugDumpLazily(1));

            String childCorrelatorName = childConfiguration.getName();
            if (childCorrelatorName != null) {
                evaluatedCorrelators.add(childCorrelatorName);
            }

            CandidateOwnersMap childCandidateOwnersMap =
                    MiscUtil.requireNonNull(
                            childResult.getCandidateOwnersMap(),
                            () -> new IllegalStateException(
                                    String.format("No candidate owner map obtained from the child correlator: %s",
                                            childConfiguration.identify())));

            Set<String> ignoreIfMatchedBy = childConfiguration.getIgnoreIfMatchedBy();
            Set<String> parentsNotEvaluatedYet = Sets.difference(ignoreIfMatchedBy, evaluatedCorrelators);
            if (!parentsNotEvaluatedYet.isEmpty()) {
                throw new ConfigurationException(
                        String.format("Correlator %s depends on %s that has not executed yet. Please check the "
                                + "distribution into tiers and the ordering.", childConfiguration, parentsNotEvaluatedYet));
            }

            double weight = childConfiguration.getWeight();
            for (CandidateOwner candidateOwner : childCandidateOwnersMap.values()) {
                String candidateOwnerOid = candidateOwner.getOid();

                LOGGER.trace("Considering candidate owner {}", candidateOwner);

                allCandidates.add(candidateOwner.getObject());

                String externalId = candidateOwner.getExternalId();
                if (externalId != null) {
                    LOGGER.trace("Registering external ID: {}", externalId);
                    String existing = externalIds.put(candidateOwnerOid, externalId);
                    if (existing != null && !existing.equals(externalId)) {
                        throw new UnsupportedOperationException(
                                String.format(
                                        "Multiple external IDs are not supported: %s for %s", existing, candidateOwner));
                    }
                }

                if (childCorrelatorName != null) {
                    candidateOwnersMatchedBy.put(candidateOwnerOid, childCorrelatorName);
                }
                Set<String> ignoredBecause =
                        Sets.intersection(ignoreIfMatchedBy, candidateOwnersMatchedBy.get(candidateOwnerOid));
                if (!ignoredBecause.isEmpty()) {
                    LOGGER.trace("Ignoring this candidate because of {}", ignoredBecause);
                } else {
                    double currentConfidence = increaseConfidence(candidateOwner, weight);
                    LOGGER.trace("Added to the candidate owners map, current confidence: {}", currentConfidence);
                }
            }
        }

        private double increaseConfidence(CandidateOwner candidateOwner, double weight) {
            return currentConfidences.compute(
                    candidateOwner.getOid(),
                    (oid, confidenceBefore) ->
                            Math.min(
                                    or0(confidenceBefore) + candidateOwner.getConfidence() * weight / scale,
                                    1.0));
        }

        public double getScale() {
            CompositeCorrelatorScalingDefinitionType scaling = configurationBean.getScaling();
            Double scale = scaling != null ? scaling.getScale() : null;
            return Objects.requireNonNullElse(scale, DEFAULT_SCALE);
        }
    }
}
