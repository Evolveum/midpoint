/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.composite;

import com.evolveum.midpoint.model.api.correlator.*;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.BaseCorrelator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
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

import static com.evolveum.midpoint.util.MiscUtil.configCheck;

/**
 * TODO
 *
 * PRELIMINARY IMPLEMENTATION!
 */
class CompositeCorrelator extends BaseCorrelator {

    private static final Trace LOGGER = TraceManager.getTrace(CompositeCorrelator.class);

    /**
     * Configuration of the this correlator.
     */
    @NotNull private final CorrelatorContext<CompositeCorrelatorType> correlatorContext;

    /** Useful beans. */
    @NotNull private final ModelBeans beans;

    CompositeCorrelator(@NotNull CorrelatorContext<CompositeCorrelatorType> correlatorContext, @NotNull ModelBeans beans) {
        this.correlatorContext = correlatorContext;
        this.beans = beans;
        LOGGER.trace("Instantiated the correlator with the configuration:\n{}", getConfiguration().debugDumpLazily(1));
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

    @NotNull private CompositeCorrelatorType getConfiguration() {
        return correlatorContext.getConfigurationBean();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

    private class Correlation {

        @NotNull private final CorrelationContext correlationContext;
        @NotNull private final Task task;
        @NotNull private final String contextDescription;

        @NotNull private final List<CorrelationResult> authoritativeResults = new ArrayList<>();
        @NotNull private final List<CorrelationResult> nonAuthoritativeResults = new ArrayList<>();

        Correlation(@NotNull CorrelationContext correlationContext) {
            this.correlationContext = correlationContext;
            this.task = correlationContext.getTask();
            this.contextDescription =
                    ("composite correlator" +
                            (getConfiguration().getName() != null ? " '" + getConfiguration().getName() + "'" : ""))
                            + " for " + correlationContext.getObjectTypeDefinition().getHumanReadableName()
                            + " in " + correlationContext.getResource();
        }

        public CorrelationResult execute(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {

            configCheck(getConfiguration().getComponents() != null,
                    "The 'components' item is missing in %s", contextDescription);

            List<CorrelatorConfiguration> childConfigurations =
                    CorrelatorConfiguration.getConfigurationsSorted(
                            getConfiguration().getComponents());

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
            CorrelationResult childResult = beans.correlatorFactoryRegistry
                    .instantiateCorrelator(
                            correlatorContext.spawn(childConfiguration), task, result)
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

        private @NotNull CorrelationResult determineOverallResult() {
            CorrelationResult authoritativeResult = getAuthoritativeResult();
            if (authoritativeResult != null) {
                return authoritativeResult;
            }

            ResourceObjectOwnerOptionsType ownerOptions = createUnifiedOwnerOptions();
            if (ownerOptions.getOption().size() > 1) {
                return CorrelationResult.uncertain(ownerOptions);
            } else {
                // Only "create new"
                return CorrelationResult.noOwner();
            }
        }

        private @Nullable CorrelationResult getAuthoritativeResult() {
            Collection<ObjectType> authoritativeCandidates = getAuthoritativeCandidates();
            if (authoritativeCandidates.size() == 1) {
                return CorrelationResult.existingOwner(authoritativeCandidates.iterator().next());
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
        private @NotNull ResourceObjectOwnerOptionsType createUnifiedOwnerOptions() {
            ResourceObjectOwnerOptionsType optionsBean = new ResourceObjectOwnerOptionsType(PrismContext.get());
            addOptions(optionsBean, authoritativeResults);
            addOptions(optionsBean, nonAuthoritativeResults);
            addNoneIfNeeded(optionsBean.getOption());
            return optionsBean;
        }

        private void addNoneIfNeeded(List<ResourceObjectOwnerOptionType> options) {
            if (options.stream().noneMatch(
                    o -> o.getCandidateOwnerRef() == null)) {
                options.add(
                        new ResourceObjectOwnerOptionType(PrismContext.get())
                                .identifier(OwnerOptionIdentifier.forNoOwner().getStringValue()));
            }
        }

        private void addOptions(ResourceObjectOwnerOptionsType aggregate, List<CorrelationResult> results) {
            for (CorrelationResult result : results) {
                if (result.isUncertain()) {
                    for (ResourceObjectOwnerOptionType option : result.getOwnerOptionsRequired().getOption()) {
                        addOption(aggregate, option);
                    }
                } else if (result.isExistingOwner()) {
                    addExistingOwnerOption(aggregate, result.getOwnerRequired());
                }
            }
        }

        private void addExistingOwnerOption(@NotNull ResourceObjectOwnerOptionsType aggregate, @NotNull ObjectType owner) {
            addOption(
                    aggregate,
                    new ResourceObjectOwnerOptionType(PrismContext.get())
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
