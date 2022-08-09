/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.composite;

import com.evolveum.midpoint.model.api.correlator.*;
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Objects;

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
        return new Correlation(correlationContext)
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

    @NotNull private CompositeCorrelatorType getConfiguration() {
        return correlatorContext.getConfigurationBean();
    }

    private class Correlation {

        @NotNull private final CorrelationContext correlationContext;
        @NotNull private final Task task;
        @NotNull private final String contextDescription; // TODO

        @NotNull private final List<CorrelationResult> authoritativeResults = new ArrayList<>();
        @NotNull private final List<CorrelationResult> nonAuthoritativeResults = new ArrayList<>();

        Correlation(@NotNull CorrelationContext correlationContext) {
            this.correlationContext = correlationContext;
            this.task = correlationContext.getTask();
            this.contextDescription = getDefaultContextDescription(correlationContext);
        }

        public CorrelationResult execute(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {

            List<CorrelatorConfiguration> childConfigurations =
                    CorrelatorConfiguration.getConfigurationsSorted(getConfiguration());

            checkIfAuthoritativeAfterNotAuthoritative(childConfigurations);

            for (CorrelatorConfiguration childConfiguration : childConfigurations) {
                CorrelationResult authoritativeResult = checkPreliminaryAuthoritativeResult(childConfiguration);
                if (authoritativeResult != null) {
                    return authoritativeResult;
                }

                CorrelationResult childResult = invokeChildCorrelator(childConfiguration, result);

                CorrelationResult immediateResult = categorizeChildResult(childConfiguration, childResult);
                if (immediateResult != null) {
                    return immediateResult;
                }
            }

            return determineOverallResult();
        }

        /**
         * If we have an authoritative result by the time we reach first non-authoritative correlator,
         * we can stop.
         */
        private @Nullable CorrelationResult checkPreliminaryAuthoritativeResult(CorrelatorConfiguration childConfiguration) {
            if (childConfiguration.getAuthority() == CorrelatorAuthorityLevelType.NON_AUTHORITATIVE) {
                CorrelationResult authoritativeResult = getAuthoritativeResult();
                if (authoritativeResult != null) {
                    LOGGER.trace("Reached non-authoritative child '{}' while having authoritative result, finishing.",
                            childConfiguration);
                    return authoritativeResult;
                }
            }
            return null;
        }

        @NotNull
        private CorrelationResult invokeChildCorrelator(CorrelatorConfiguration childConfiguration, OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            LOGGER.trace("Going to invoke child correlator '{}'", childConfiguration);
            CorrelationResult childResult = instantiateChild(childConfiguration, task, result)
                    .correlate(correlationContext, result);
            LOGGER.trace("Child correlator '{}' provided the following result:\n{}",
                    childConfiguration, childResult.debugDumpLazily(1));
            return childResult;
        }

        private @Nullable CorrelationResult categorizeChildResult(
                CorrelatorConfiguration childConfiguration, CorrelationResult childResult) {
            if (childResult.isError()) {
                LOGGER.trace("Child correlator '{}' finished with an error, exiting: {}",
                        childConfiguration, childResult.getErrorMessage());
                return childResult;
            }

            CorrelatorAuthorityLevelType childAuthority = childConfiguration.getAuthority();
            if (childAuthority == CorrelatorAuthorityLevelType.PRINCIPAL) {
                if (childResult.isExistingOwner()) {
                    LOGGER.trace("Principal child evaluator '{}' returned the owner, finishing with:\n{}",
                            childConfiguration, childResult.debugDumpLazily(1));
                    return childResult;
                } else {
                    authoritativeResults.add(childResult);
                }
            } else if (childAuthority == CorrelatorAuthorityLevelType.AUTHORITATIVE) {
                authoritativeResults.add(childResult);
            } else {
                nonAuthoritativeResults.add(childResult);
            }
            return null;
        }

        private @NotNull CorrelationResult determineOverallResult() throws SchemaException {
            CorrelationResult authoritativeResult = getAuthoritativeResult();
            if (authoritativeResult != null) {
                return authoritativeResult;
            }

            OwnersInfo ownersInfo = createUnifiedOwnerInfo();
            List<ResourceObjectOwnerOptionType> options = ownersInfo.optionsBean.getOption();
            if (options.size() > 1) {
                return CorrelationResult.uncertain(ownersInfo);
            } else {
                assert options.size() == 1;
                assert OwnerOptionIdentifier.fromStringValue(options.get(0).getIdentifier())
                        .isNewOwner();
                return CorrelationResult.noOwner();
            }
        }

        private @Nullable CorrelationResult getAuthoritativeResult() {
            Collection<ObjectType> authoritativeCandidates = getAuthoritativeCandidates();
            if (authoritativeCandidates.size() == 1) {
                return CorrelationResult.existingOwner(authoritativeCandidates.iterator().next(), null);
            } else {
                return null;
            }
        }

        /** Taking candidates from all authoritative children. */
        private Collection<ObjectType> getAuthoritativeCandidates() {
            Map<String, ObjectType> candidateOwners = new HashMap<>(); // by OID
            for (CorrelationResult authoritativeResult : authoritativeResults) {
                if (authoritativeResult.isUncertain()) {
                    LOGGER.trace("Uncertain authoritative result. This should not occur. We'll continue by including "
                            + "non-authoritative answers as well. The result:\n{}",
                            authoritativeResult.debugDumpLazily(1));
                    return Set.of(); // causing continuing with all answers
                } else if (authoritativeResult.isExistingOwner()) {
                    ObjectType existingOwner = authoritativeResult.getOwnerRequired();
                    candidateOwners.put(existingOwner.getOid(), existingOwner);
                } else if (authoritativeResult.isNoOwner()) {
                    // continuing
                } else {
                    throw new IllegalStateException("Unexpected result: " + authoritativeResult);
                }
            }
            LOGGER.trace("getAuthoritativeCandidates found: {}", candidateOwners);
            return candidateOwners.values();
        }

        /**
         * Taking options from all results (authoritative + non-authoritative)
         *
         * Limitations:
         *
         * 1. Assumes identifiers are comparable (or even that they are OID-based).
         * 2. Ignores the confidence
         */
        private @NotNull OwnersInfo createUnifiedOwnerInfo() {
            OwnersInfo ownersInfo = new OwnersInfo(
                    new ResourceObjectOwnerOptionsType(), new ObjectSet<>());
            addOptions(ownersInfo, authoritativeResults);
            addOptions(ownersInfo, nonAuthoritativeResults);
            addNoneIfNeeded(ownersInfo.optionsBean.getOption());
            return ownersInfo;
        }

        private void addNoneIfNeeded(List<ResourceObjectOwnerOptionType> options) {
            if (options.stream().noneMatch(
                    o -> o.getCandidateOwnerRef() == null)) {
                options.add(
                        new ResourceObjectOwnerOptionType()
                                .identifier(OwnerOptionIdentifier.forNoOwner().getStringValue()));
            }
        }

        private void addOptions(OwnersInfo aggregate, List<CorrelationResult> results) {
            for (CorrelationResult result : results) {
                if (result.isUncertain()) {
                    for (ResourceObjectOwnerOptionType option : result.getOwnerOptionsRequired().getOption()) {
                        addOption(aggregate.optionsBean, option);
                    }
                } else if (result.isExistingOwner()) {
                    addExistingOwnerOption(aggregate.optionsBean, result.getOwnerRequired());
                }
                aggregate.allOwnerCandidates.addAll(
                        result.getAllOwnerCandidates());
            }
        }

        private void addExistingOwnerOption(@NotNull ResourceObjectOwnerOptionsType aggregate, @NotNull ObjectType owner) {
            addOption(
                    aggregate,
                    new ResourceObjectOwnerOptionType()
                            .identifier(
                                    OwnerOptionIdentifier.forExistingOwner(owner.getOid()).getStringValue())
                            .candidateOwnerRef(
                                    ObjectTypeUtil.createObjectRef(owner)));

        }

        private void addOption(@NotNull ResourceObjectOwnerOptionsType aggregate, @NotNull ResourceObjectOwnerOptionType option) {
            for (ResourceObjectOwnerOptionType existingOption : aggregate.getOption()) {
                Objects.requireNonNull(existingOption.getIdentifier(), () -> "No identifier in " + existingOption);
                Objects.requireNonNull(option.getIdentifier(), () -> "No identifier in " + option);
                if (existingOption.getIdentifier().equals(option.getIdentifier())) {
                    return; // the option is already there
                }
            }
            aggregate.getOption().add(
                    option.cloneWithoutId());
        }

        private void checkIfAuthoritativeAfterNotAuthoritative(List<CorrelatorConfiguration> allChildren) {
            CorrelatorConfiguration firstNonAuth = null;
            for (CorrelatorConfiguration current : allChildren) {
                if (firstNonAuth == null && current.getAuthority() == CorrelatorAuthorityLevelType.NON_AUTHORITATIVE) {
                    firstNonAuth = current;
                }
                if (firstNonAuth != null && current.getAuthority() != CorrelatorAuthorityLevelType.NON_AUTHORITATIVE) {
                    LOGGER.warn("Authoritative/principal correlator configuration {} after non-authoritative one: {}",
                            current, firstNonAuth);
                }
            }
        }
    }
}
