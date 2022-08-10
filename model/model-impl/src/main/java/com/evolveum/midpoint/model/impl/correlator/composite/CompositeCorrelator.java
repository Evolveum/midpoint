/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.composite;

import static com.evolveum.midpoint.model.api.correlator.CorrelatorConfiguration.getChildConfigurations;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.api.correlator.*;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelationResult.OwnersInfo;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.BaseCorrelator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectSet;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.cases.OwnerOptionIdentifier;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Composite correlator that evaluates its components (child correlators) and builds up the result
 * according to their results.
 *
 * PRELIMINARY IMPLEMENTATION!
 *
 * TODO ignore identifiers in owner options
 */
class CompositeCorrelator extends BaseCorrelator<CompositeCorrelatorType> {

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
        return new Correlation<>(correlationContext)
                .execute(result);
    }

    @Override
    protected boolean checkCandidateOwnerInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull FocusType candidateOwner,
            @NotNull OperationResult result) {
        throw new UnsupportedOperationException("CompositeCorrelator is not supported in the 'opportunistic synchronization'"
                + " mode. Please disable this mode for this particular resource or object type.");
    }

    private class Correlation<F extends FocusType> {

        @NotNull private final CorrelationContext correlationContext;
        @NotNull private final Task task;
        @NotNull private final String contextDescription; // TODO

        @NotNull private final Collection<CorrelatorConfiguration> childConfigurations;
        @NotNull private final Set<Integer> tiers;

        /** What candidate owners were matched by what correlators (only with non-null names). */
        @NotNull private final HashMultimap<String, String> candidateOwnersMatchedBy = HashMultimap.create();

        @NotNull private final Set<String> evaluatedCorrelators = new HashSet<>();

        @NotNull private final CandidateOwnerMap<F> candidateOwnerMap = new CandidateOwnerMap<>();

        private CorrelationResult errorResult;

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

            CorrelationResult overallResult = null;
            int tierNumber = 1;
            for (Integer tier : tiers) {
                List<CorrelatorConfiguration> childConfigurations = getConfigurationsInTierSorted(tier);
                LOGGER.trace("Processing tier #{}/{} with ID '{}': {} child correlator(s)",
                        tierNumber, tiers.size(), tier, childConfigurations.size());

                for (CorrelatorConfiguration childConfiguration : childConfigurations) {
                    invokeChildCorrelator(childConfiguration, result);
                    if (errorResult != null) {
                        LOGGER.trace("Got an error result in tier {}, finishing:\n{}",
                                tier, errorResult.debugDumpLazily(1));
                        return errorResult;
                    }
                }

                overallResult = determineOverallResult();
                if (overallResult.isExistingOwner()) {
                    LOGGER.trace("Reached 'existing owner' answer in tier {}, finishing:\n{}",
                            tier, overallResult.debugDumpLazily(1));
                    return overallResult;
                }
            }
            stateCheck(overallResult != null, "No tiers?");
            LOGGER.trace("Finishing after last tier was evaluated:\n{}", overallResult.debugDumpLazily(1));
            return overallResult;
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
            CorrelationResult childResult = instantiateChild(childConfiguration, task, result)
                    .correlate(correlationContext, result);
            LOGGER.trace("Child correlator '{}' provided the following result:\n{}",
                    childConfiguration, childResult.debugDumpLazily(1));

            if (childResult.isError()) {
                LOGGER.trace("Child correlator '{}' finished with an error, exiting: {}",
                        childConfiguration, childResult.getErrorMessage());
                errorResult = childResult;
                return;
            }

            String childCorrelatorName = childConfiguration.getName();
            if (childCorrelatorName != null) {
                evaluatedCorrelators.add(childCorrelatorName);
            }

            CandidateOwnerMap<?> childCandidateOwnerMap =
                    MiscUtil.requireNonNull(
                            childResult.getCandidateOwnerMap(),
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

            for (CandidateOwner<?> candidateOwner : childCandidateOwnerMap.values()) {
                LOGGER.trace("Considering candidate owner {}", candidateOwner);
                String candidateOwnerOid = candidateOwner.getOid();
                if (childCorrelatorName != null) {
                    candidateOwnersMatchedBy.put(candidateOwnerOid, childCorrelatorName);
                }
                Set<String> ignoredBecause =
                        Sets.intersection(ignoreIfMatchedBy, candidateOwnersMatchedBy.get(candidateOwnerOid));
                if (!ignoredBecause.isEmpty()) {
                    LOGGER.trace("Ignoring this candidate because of {}", ignoredBecause);
                } else {
                    //noinspection unchecked
                    Double currentWeight = candidateOwnerMap.add((CandidateOwner<F>) candidateOwner);
                    LOGGER.trace("Added to the candidate owners map, current weight: {}", currentWeight);
                }
            }
        }

        private @NotNull CorrelationResult determineOverallResult() {

            double ownerThreshold = correlatorContext.getConfiguration().getOwnerThreshold();
            var owners =
                    candidateOwnerMap.selectWithConfidenceAtLeast(
                            ownerThreshold);
            double candidateThreshold = correlatorContext.getConfiguration().getCandidateThreshold();
            var eligibleCandidates =
                    candidateOwnerMap.selectWithConfidenceAtLeast(
                            candidateThreshold);

            LOGGER.trace("Determining overall result with owner threshold of {}, candidate threshold of {}, "
                            + "owners: {}, eligible candidates: {}, all candidates:\n{}",
                    ownerThreshold, candidateThreshold, owners.size(), eligibleCandidates.size(),
                    DebugUtil.toStringCollectionLazy(candidateOwnerMap.values(), 1));

            ResourceObjectOwnerOptionsType optionsBean = new ResourceObjectOwnerOptionsType();
            for (CandidateOwner<F> eligibleCandidate : eligibleCandidates) {
                optionsBean.getOption().add(
                        new ResourceObjectOwnerOptionType()
                                .identifier(
                                        OwnerOptionIdentifier.forExistingOwner(eligibleCandidate.getOid()).getStringValue())
                                .candidateOwnerRef(
                                        ObjectTypeUtil.createObjectRef(((CandidateOwner<?>) eligibleCandidate).getObject()))
                                .confidence(
                                        eligibleCandidate.getConfidence()));
            }
            if (owners.size() != 1) {
                optionsBean.getOption().add(
                        new ResourceObjectOwnerOptionType()
                                .identifier(OwnerOptionIdentifier.forNoOwner().getStringValue()));
            }

            ObjectSet<ObjectType> allOwnerCandidates = new ObjectSet<>();
            for (CandidateOwner<F> eligibleCandidate : eligibleCandidates) {
                allOwnerCandidates.add(
                        eligibleCandidate.getObject());
            }

            OwnersInfo ownersInfo = new OwnersInfo(
                    CandidateOwnerMap.from(eligibleCandidates),
                    optionsBean,
                    allOwnerCandidates);

            if (owners.size() == 1) {
                return CorrelationResult.existingOwner(owners.iterator().next().getObject(), ownersInfo);
            } else if (eligibleCandidates.isEmpty()) {
                return CorrelationResult.noOwner();
            } else {
                return CorrelationResult.uncertain(ownersInfo);
            }
        }
    }
}
