/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch;

import static com.evolveum.midpoint.util.MiscUtil.*;
import static com.evolveum.midpoint.util.QNameUtil.qNameToUri;
import static com.evolveum.midpoint.util.QNameUtil.uriToQName;

import java.util.Collection;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlator.*;
import com.evolveum.midpoint.model.api.correlator.idmatch.*;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * A correlator based on an external service providing ID Match API.
 * (https://spaces.at.internet2.edu/display/cifer/SOR-Registry+Strawman+ID+Match+API)
 */
class IdMatchCorrelator implements Correlator {

    private static final Trace LOGGER = TraceManager.getTrace(IdMatchCorrelator.class);

    /**
     * Configuration of the this correlator.
     */
    @NotNull private final CorrelatorContext<IdMatchCorrelatorType> correlatorContext;

    /**
     * Configuration of a follow-on correlator (used to find real account owner based on matched identity).
     */
    @NotNull private final CorrelatorConfiguration followOnCorrelatorConfiguration;

    /**
     * Service that resolves "reference ID" for resource objects being correlated.
     */
    @NotNull private final IdMatchService service;

    private final ModelBeans beans;

    /**
     * @param serviceOverride An instance of {@link IdMatchService} that should be used instead of the default one.
     *                        Used for unit testing.
     */
    IdMatchCorrelator(
            @NotNull CorrelatorContext<IdMatchCorrelatorType> correlatorContext,
            @Nullable IdMatchService serviceOverride,
            ModelBeans beans) throws ConfigurationException {
        IdMatchCorrelatorType configuration = correlatorContext.getConfigurationBean();

        this.correlatorContext = correlatorContext;
        this.service = instantiateService(configuration, serviceOverride);
        this.beans = beans;

        this.followOnCorrelatorConfiguration = getFollowOnConfiguration(configuration);

        LOGGER.trace("Instantiated the correlator with the configuration:\n{}", configuration.debugDumpLazily(1));
    }

    @NotNull
    private IdMatchService instantiateService(
            @NotNull IdMatchCorrelatorType configuration, @Nullable IdMatchService serviceOverride)
            throws ConfigurationException {
        if (serviceOverride != null) {
            return serviceOverride;
        } else {
            return IdMatchServiceImpl.instantiate(configuration);
        }
    }

    private CorrelatorConfiguration getFollowOnConfiguration(@NotNull IdMatchCorrelatorType configuration)
            throws ConfigurationException {
        configCheck(configuration.getFollowOn() != null,
                "No 'follow on' correlator configured in %s", configuration);
        Collection<CorrelatorConfiguration> followOnConfigs =
                CorrelatorConfiguration.getConfigurations(configuration.getFollowOn());
        configCheck(followOnConfigs.size() == 1, "Not a single 'follow on' correlator configured: %s",
                followOnConfigs);
        return followOnConfigs.iterator().next();
    }

    @Override
    public CorrelationResult correlate(
            @NotNull CorrelationContext correlationContext,
            @NotNull Task task,
            @NotNull OperationResult result) throws ConfigurationException, SchemaException,
            ExpressionEvaluationException, CommunicationException, SecurityViolationException, ObjectNotFoundException {

        LOGGER.trace("Correlating:\n{}", correlationContext.debugDumpLazily(1));

        return new Correlation(correlationContext, task)
                .execute(result);
    }

    private class Correlation {
        @NotNull private final ShadowType resourceObject;
        @NotNull private final CorrelationContext correlationContext;
        @NotNull private final Task task;

        Correlation(
                @NotNull CorrelationContext correlationContext,
                @NotNull Task task) {
            this.resourceObject = correlationContext.getResourceObject();
            this.correlationContext = correlationContext;
            this.task = task;
        }

        public CorrelationResult execute(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {

            MatchingRequest mRequest =
                    new MatchingRequest(
                            prepareIdMatchObject(
                                    correlationContext.getPreFocus(),
                                    correlationContext.getResourceObject()));
            MatchingResult mResult = service.executeMatch(mRequest, result);
            LOGGER.trace("Matching result:\n{}", mResult.debugDumpLazily(1));

            IdMatchCorrelationStateType correlationState = createCorrelationState(mResult);
            correlationContext.setCorrelationState(correlationState);
            // we also need the state in the shadow in the case object
            resourceObject.setCorrelationState(correlationState.clone());

            if (mResult.getReferenceId() != null) {
                beans.correlationCaseManager.closeCaseIfExists(resourceObject, result);
                return correlateUsingKnownReferenceId(result);
            } else {
                beans.correlationCaseManager.createOrUpdateCase(
                        resourceObject,
                        correlationContext.getPreFocus(),
                        createPotentialOwnersBean(mResult, result),
                        task,
                        result);
                return CorrelationResult.uncertain();
            }
        }

        private @NotNull IdMatchCorrelationStateType createCorrelationState(MatchingResult mResult) {
            IdMatchCorrelationStateType state = new IdMatchCorrelationStateType(PrismContext.get());
            state.setReferenceId(mResult.getReferenceId());
            state.setMatchRequestId(mResult.getMatchRequestId());
            return state;
        }

        private CorrelationResult correlateUsingKnownReferenceId(OperationResult result)
                throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException {

            return beans.correlatorFactoryRegistry
                    .instantiateCorrelator(
                            correlatorContext.spawn(followOnCorrelatorConfiguration), task, result)
                    .correlate(correlationContext, task, result);
        }

        /**
         * Converts internal {@link MatchingResult} into "externalized" {@link PotentialOwnersType} bean
         * to be stored in the correlation case.
         *
         * _Temporarily_ adding also "none of the above" potential match here. (If it is not present among options returned
         * from the ID Match service.)
         */
        private @NotNull PotentialOwnersType createPotentialOwnersBean(
                @NotNull MatchingResult mResult,
                @NotNull OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            PotentialOwnersType context = new PotentialOwnersType(PrismContext.get());
            boolean newIdentityOptionPresent = false;
            for (PotentialMatch potentialMatch : mResult.getPotentialMatches()) {
                if (potentialMatch.isNewIdentity()) {
                    newIdentityOptionPresent = true;
                }
                PotentialOwnerType potentialMatchBean = createPotentialMatchBeanFromReturnedMatch(potentialMatch, result);
                if (potentialMatchBean != null) {
                    context.getPotentialOwner().add(potentialMatchBean);
                }
            }
            if (!newIdentityOptionPresent) {
                context.getPotentialOwner().add(
                        createPotentialMatchBeanForNewIdentity());
            }
            return context;
        }

        private @Nullable PotentialOwnerType createPotentialMatchBeanFromReturnedMatch(
                @NotNull PotentialMatch potentialMatch,
                @NotNull OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            @Nullable String id = potentialMatch.getReferenceId();
            if (id != null) {
                String optionUri = qNameToUri(
                        new QName(SchemaConstants.CORRELATION_NS, SchemaConstants.CORRELATION_OPTION_PREFIX + id));
                ObjectReferenceType candidateOwnerRef = getCandidateOwnerRef(id, result);
                if (candidateOwnerRef == null) {
                    LOGGER.warn("Non-null reference ID {} contained in {} yields no owner reference. Ignoring this match.",
                            id, potentialMatch);
                    return null;
                } else {
                    return new PotentialOwnerType(PrismContext.get())
                            .uri(optionUri)
                            .confidence(potentialMatch.getConfidence())
                            .candidateOwnerRef(candidateOwnerRef);
                }
            } else {
                return new PotentialOwnerType(PrismContext.get())
                        .uri(SchemaConstants.CORRELATION_NONE_URI)
                        .confidence(potentialMatch.getConfidence());
            }
        }

        private @Nullable ObjectReferenceType getCandidateOwnerRef(String referenceId, OperationResult result)
                throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException {

            // We create a context with a fake state having the referenceId we want to resolve
            CorrelationContext clonedContext = correlationContext.clone();
            clonedContext.setCorrelationState(
                    new IdMatchCorrelationStateType(PrismContext.get())
                            .referenceId(referenceId));

            CorrelationResult correlationResult = beans.correlatorFactoryRegistry
                    .instantiateCorrelator(
                            correlatorContext.spawn(followOnCorrelatorConfiguration), task, result)
                    .correlate(clonedContext, task, result);

            stateCheck(!correlationResult.isUncertain(),
                    "Unexpected uncertain correlation result for candidate reference ID %s: %s",
                    referenceId, correlationResult);

            return ObjectTypeUtil.createObjectRef(correlationResult.getOwner());
        }

        private PotentialOwnerType createPotentialMatchBeanForNewIdentity() {
            return new PotentialOwnerType(PrismContext.get())
                    .uri(SchemaConstants.CORRELATION_NONE_URI);
        }
    }

    @Override
    public void resolve(
            @NotNull CaseType aCase,
            @NotNull String outcomeUri,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException, CommunicationException {
        ShadowType shadow = CorrelatorUtil.getShadowFromCorrelationCase(aCase);
        FocusType preFocus = CorrelatorUtil.getPreFocusFromCorrelationCase(aCase);
        IdMatchObject idMatchObject = prepareIdMatchObject(preFocus, shadow);
        IdMatchCorrelationStateType state = MiscUtil.requireNonNull(
                MiscUtil.castSafely(shadow.getCorrelationState(), IdMatchCorrelationStateType.class),
                () -> new IllegalStateException("No correlation state in shadow " + shadow + " in " + aCase));
        String matchRequestId = state.getMatchRequestId();
        String correlatedReferenceId = getCorrelatedReferenceId(outcomeUri);

        service.resolve(idMatchObject, matchRequestId, correlatedReferenceId, result);
    }

    private IdMatchObject prepareIdMatchObject(@NotNull FocusType preFocus, @NotNull ShadowType shadow) throws SchemaException {
        return new IdMatchObjectCreator(correlatorContext, preFocus, shadow)
                .create();
    }

    private String getCorrelatedReferenceId(@NotNull String outcomeUri) throws SchemaException {
        String outcome = uriToQName(outcomeUri, true).getLocalPart();
        if (SchemaConstants.CORRELATION_NONE.equals(outcome)) {
            return null;
        } else if (outcome.startsWith(SchemaConstants.CORRELATION_OPTION_PREFIX)) {
            return outcome.substring(SchemaConstants.CORRELATION_OPTION_PREFIX.length());
        } else {
            throw new SchemaException("Unsupported outcome URI: " + outcomeUri);
        }
    }
}
