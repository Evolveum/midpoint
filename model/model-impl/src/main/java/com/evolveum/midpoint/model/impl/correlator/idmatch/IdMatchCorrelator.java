/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch;

import static com.evolveum.midpoint.util.MiscUtil.*;

import java.util.Collection;

import com.evolveum.midpoint.model.impl.correlator.BaseCorrelator;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.ShadowUtil;

import com.evolveum.midpoint.schema.util.cases.OwnerOptionIdentifier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlator.*;
import com.evolveum.midpoint.model.api.correlator.idmatch.*;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * A correlator based on an external service providing ID Match API.
 * (https://spaces.at.internet2.edu/display/cifer/SOR-Registry+Strawman+ID+Match+API)
 */
class IdMatchCorrelator extends BaseCorrelator<IdMatchCorrelatorType> {

    private static final Trace LOGGER = TraceManager.getTrace(IdMatchCorrelator.class);

    /**
     * Configuration of a follow-on correlator (used to find real account owner based on matched identity).
     */
    @NotNull private final CorrelatorConfiguration followOnCorrelatorConfiguration;

    /**
     * Service that resolves "reference ID" for resource objects being correlated.
     */
    @NotNull private final IdMatchService service;

    /**
     * @param serviceOverride An instance of {@link IdMatchService} that should be used instead of the default one.
     *                        Used for unit testing.
     */
    IdMatchCorrelator(
            @NotNull CorrelatorContext<IdMatchCorrelatorType> correlatorContext,
            @Nullable IdMatchService serviceOverride,
            ModelBeans beans) throws ConfigurationException {
        super(LOGGER, "idmatch", correlatorContext, beans);
        this.service = instantiateService(serviceOverride);
        this.followOnCorrelatorConfiguration = getFollowOnConfiguration();
        LOGGER.trace("ID Match service (i.e. client) instantiated: {}", service);
    }

    @NotNull
    private IdMatchService instantiateService(@Nullable IdMatchService serviceOverride)
            throws ConfigurationException {
        if (serviceOverride != null) {
            return serviceOverride;
        } else {
            return IdMatchServiceImpl.instantiate(configurationBean);
        }
    }

    private CorrelatorConfiguration getFollowOnConfiguration() throws ConfigurationException {
        configCheck(configurationBean.getFollowOn() != null,
                "No 'follow on' correlator configured in %s", configurationBean);
        Collection<CorrelatorConfiguration> followOnConfigs =
                CorrelatorConfiguration.getConfigurations(configurationBean.getFollowOn());
        configCheck(followOnConfigs.size() == 1, "Not a single 'follow on' correlator configured: %s",
                followOnConfigs);
        return followOnConfigs.iterator().next();
    }

    @Override
    public @NotNull CorrelationResult correlateInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull OperationResult result) throws ConfigurationException, SchemaException,
            ExpressionEvaluationException, CommunicationException, SecurityViolationException, ObjectNotFoundException {

        return new CorrelationOperation(correlationContext)
                .execute(result);
    }

    private class Operation {

        @NotNull final ShadowType resourceObject;
        @NotNull final CorrelationContext correlationContext;
        @NotNull final Task task;

        Operation(@NotNull CorrelationContext correlationContext) {
            this.resourceObject = correlationContext.getResourceObject();
            this.correlationContext = correlationContext;
            this.task = correlationContext.getTask();
        }

        IdMatchObject prepareIdMatchObjectFromContext() throws SchemaException {
            return prepareIdMatchObject(
                    correlationContext.getPreFocus(),
                    correlationContext.getResourceObject());
        }
    }

    private class CorrelationOperation extends Operation {

        CorrelationOperation(@NotNull CorrelationContext correlationContext) {
            super(correlationContext);
        }

        public CorrelationResult execute(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {

            MatchingRequest mRequest =
                    new MatchingRequest(
                            prepareIdMatchObjectFromContext());
            MatchingResult mResult = service.executeMatch(mRequest, result);
            LOGGER.trace("Matching result:\n{}", mResult.debugDumpLazily(1));

            // FIXME this assumes we are the top-level correlator!
            IdMatchCorrelatorStateType correlatorState = createCorrelatorState(mResult);
            correlationContext.setCorrelatorState(correlatorState);
            // we also need the state in the shadow in the case object
            ShadowUtil.setCorrelatorState(resourceObject, correlatorState.clone());

            if (mResult.getReferenceId() != null) {
                return correlateUsingKnownReferenceId(result);
            } else {
                return CorrelationResult.uncertain(
                        createOwnerOptions(mResult, result));
            }
        }

        private @NotNull IdMatchCorrelatorStateType createCorrelatorState(MatchingResult mResult) {
            IdMatchCorrelatorStateType state = new IdMatchCorrelatorStateType(PrismContext.get());
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
                    .correlate(correlationContext, result);
        }

        /**
         * Converts internal {@link MatchingResult} into "externalized" {@link ResourceObjectOwnerOptionsType} bean
         * to be stored in the shadow.
         *
         * _Temporarily_ adding also "none of the above" potential match here. (If it is not present among options returned
         * from the ID Match service.)
         */
        private @NotNull ResourceObjectOwnerOptionsType createOwnerOptions(
                @NotNull MatchingResult mResult,
                @NotNull OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            ResourceObjectOwnerOptionsType options = new ResourceObjectOwnerOptionsType(PrismContext.get());
            boolean newIdentityOptionPresent = false;
            for (PotentialMatch potentialMatch : mResult.getPotentialMatches()) {
                if (potentialMatch.isNewIdentity()) {
                    newIdentityOptionPresent = true;
                }
                ResourceObjectOwnerOptionType potentialMatchBean = createPotentialOwnerBeanFromReturnedMatch(potentialMatch, result);
                if (potentialMatchBean != null) {
                    options.getOption().add(potentialMatchBean);
                }
            }
            if (!newIdentityOptionPresent) {
                options.getOption().add(
                        createPotentialMatchBeanForNewIdentity());
            }
            return options;
        }

        private @Nullable ResourceObjectOwnerOptionType createPotentialOwnerBeanFromReturnedMatch(
                @NotNull PotentialMatch potentialMatch,
                @NotNull OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            @Nullable String id = potentialMatch.getReferenceId();
            ResourceObjectOwnerOptionType potentialOwnerBean = new ResourceObjectOwnerOptionType(PrismContext.get())
                    .identifier(OwnerOptionIdentifier.forExistingOrNoOwner(id).getStringValue())
                    .confidence(potentialMatch.getConfidenceScaledToOne());
            if (id != null) {
                ObjectReferenceType candidateOwnerRef = getCandidateOwnerRef(id, result);
                if (candidateOwnerRef == null) {
                    LOGGER.warn("Non-null reference ID {} contained in {} yields no owner reference. Ignoring this match.",
                            id, potentialMatch);
                    return null;
                } else {
                    potentialOwnerBean.setCandidateOwnerRef(candidateOwnerRef);
                }
            }
            return potentialOwnerBean;
        }

        private @Nullable ObjectReferenceType getCandidateOwnerRef(String referenceId, OperationResult result)
                throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException {

            // We create a context with a fake state having the referenceId we want to resolve
            CorrelationContext clonedContext = correlationContext.clone();
            clonedContext.setCorrelatorState(
                    new IdMatchCorrelatorStateType(PrismContext.get())
                            .referenceId(referenceId));

            CorrelationResult correlationResult = beans.correlatorFactoryRegistry
                    .instantiateCorrelator(
                            correlatorContext.spawn(followOnCorrelatorConfiguration), task, result)
                    .correlate(clonedContext, result);

            stateCheck(!correlationResult.isUncertain(),
                    "Unexpected uncertain correlation result for candidate reference ID %s: %s",
                    referenceId, correlationResult);

            return ObjectTypeUtil.createObjectRef(correlationResult.getOwner());
        }

        private ResourceObjectOwnerOptionType createPotentialMatchBeanForNewIdentity() {
            return new ResourceObjectOwnerOptionType(PrismContext.get())
                    .identifier(OwnerOptionIdentifier.forNoOwner().getStringValue());
        }
    }

    @Override
    public void update(@NotNull CorrelationContext correlationContext, @NotNull OperationResult result)
            throws SchemaException, CommunicationException, SecurityViolationException {

        LOGGER.trace("Updating:\n{}", correlationContext.debugDumpLazily(1));
        new UpdateOperation(correlationContext)
                .execute(result);
    }

    private class UpdateOperation extends Operation {

        UpdateOperation(@NotNull CorrelationContext correlationContext) {
            super(correlationContext);
        }

        public void execute(OperationResult result)
                throws SchemaException, CommunicationException, SecurityViolationException {

            IdMatchCorrelatorStateType correlatorState =
                    ShadowUtil.getCorrelatorStateRequired(
                            correlationContext.getResourceObject(), IdMatchCorrelatorStateType.class);

            service.update(
                    prepareIdMatchObjectFromContext(),
                    correlatorState.getReferenceId(),
                    result);
        }
    }

    @Override
    public void resolve(
            @NotNull CaseType aCase,
            @NotNull String outcomeUri,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, CommunicationException, SecurityViolationException, ObjectNotFoundException {
        ShadowType shadow = CorrelatorUtil.getShadowFromCorrelationCase(aCase);
        FocusType preFocus = CorrelatorUtil.getPreFocusFromCorrelationCase(aCase);
        IdMatchObject idMatchObject = prepareIdMatchObject(preFocus, shadow);
        IdMatchCorrelatorStateType state = ShadowUtil.getCorrelatorStateRequired(shadow, IdMatchCorrelatorStateType.class);
        String matchRequestId = state.getMatchRequestId();
        String correlatedReferenceId = OwnerOptionIdentifier.fromStringValue(outcomeUri).getExistingOwnerId();

        @NotNull String assignedReferenceId = service.resolve(idMatchObject, matchRequestId, correlatedReferenceId, result);

        setReferenceIdInShadow(shadow, assignedReferenceId, result);
    }

    private void setReferenceIdInShadow(ShadowType shadow, String referenceId, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        ItemPath referenceIdPath = ItemPath.create(
                ShadowType.F_CORRELATION,
                ShadowCorrelationStateType.F_CORRELATOR_STATE,
                IdMatchCorrelatorStateType.F_REFERENCE_ID);
        PrismPropertyDefinition<String> referenceIdDefinition = PrismContext.get().getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(IdMatchCorrelatorStateType.class)
                .findPropertyDefinition(IdMatchCorrelatorStateType.F_REFERENCE_ID);

        try {
            beans.cacheRepositoryService.modifyObject(
                    ShadowType.class,
                    shadow.getOid(),
                    PrismContext.get().deltaFor(ShadowType.class)
                            .item(referenceIdPath, referenceIdDefinition).replace(referenceId)
                            .asItemDeltas(),
                    result);
        } catch (ObjectAlreadyExistsException e) {
            throw SystemException.unexpected(e, "while setting ID Match Reference ID");
        }
    }

    private IdMatchObject prepareIdMatchObject(@NotNull FocusType preFocus, @NotNull ShadowType shadow) throws SchemaException {
        return new IdMatchObjectCreator(correlatorContext, preFocus, shadow)
                .create();
    }

}
