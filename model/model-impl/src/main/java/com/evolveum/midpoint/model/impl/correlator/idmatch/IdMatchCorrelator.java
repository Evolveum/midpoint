/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch;

import static com.evolveum.midpoint.model.api.correlator.Confidence.full;
import static com.evolveum.midpoint.model.api.correlator.Confidence.zero;
import static com.evolveum.midpoint.schema.GetOperationOptions.createRetrieveCollection;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.model.api.correlation.CorrelationPropertyDefinition;
import com.evolveum.midpoint.model.api.correlator.*;

import com.evolveum.midpoint.prism.PrismObjectDefinition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.idmatch.*;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.BaseCorrelator;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.cases.OwnerOptionIdentifier;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * A correlator based on an external service providing ID Match API.
 * (https://spaces.at.internet2.edu/display/cifer/SOR-Registry+Strawman+ID+Match+API)
 *
 * Limitations:
 *
 * . This correlator is not to be used as a child of the composite correlator.
 * . Currently supports only shadow-based correlations.
 */
public class IdMatchCorrelator extends BaseCorrelator<IdMatchCorrelatorType> {

    private static final double DEFAULT_CONFIDENCE_LIMIT = 0.9;

    private static final Trace LOGGER = TraceManager.getTrace(IdMatchCorrelator.class);

    /** Service that resolves "reference ID" for resource objects being correlated. */
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
        LOGGER.trace("ID Match service (i.e. client) instantiated: {}", service);
    }

    private @NotNull IdMatchService instantiateService(@Nullable IdMatchService serviceOverride)
            throws ConfigurationException {
        if (serviceOverride != null) {
            return serviceOverride;
        } else {
            return IdMatchServiceImpl.instantiate(configurationBean);
        }
    }

    @Override
    public @NotNull CorrelationResult correlateInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull OperationResult result) throws ConfigurationException, SchemaException, CommunicationException,
            SecurityViolationException {
        return new CorrelationLikeOperation(correlationContext.asShadowCtx())
                .correlate(result);
    }

    @Override
    protected @NotNull Confidence checkCandidateOwnerInternal(
            @NotNull CorrelationContext correlationContext,
            @NotNull FocusType candidateOwner,
            @NotNull OperationResult result)
            throws ConfigurationException, SchemaException, CommunicationException, SecurityViolationException {
        return new CorrelationLikeOperation(correlationContext.asShadowCtx())
                .checkCandidateOwner(candidateOwner, result);
    }

    /** Correlation or update operation. */
    private class Operation {

        @NotNull final ShadowType resourceObject;
        @NotNull final CorrelationContext.Shadow correlationContext;
        @NotNull final Task task;

        Operation(@NotNull CorrelationContext.Shadow correlationContext) {
            this.resourceObject = correlationContext.getResourceObject();
            this.correlationContext = correlationContext;
            this.task = correlationContext.getTask();
        }

        IdMatchObject prepareIdMatchObjectFromContext() throws SchemaException, ConfigurationException {
            return prepareIdMatchObject(
                    correlationContext.getPreFocus(),
                    correlationContext.getResourceObject());
        }

        void ensureNotInShadowSimulationMode() {
            if (task.areShadowChangesSimulated()) {
                throw new UnsupportedOperationException(
                        "Shadows-simulation mode cannot be used with ID Match correlator: " + task.getExecutionMode());
            }
        }
    }

    private class CorrelationLikeOperation extends Operation {

        CorrelationLikeOperation(@NotNull CorrelationContext.Shadow correlationContext) {
            super(correlationContext);
        }

        CorrelationResult correlate(OperationResult result)
                throws SchemaException, CommunicationException, SecurityViolationException, ConfigurationException {

            ensureNotInShadowSimulationMode();
            MatchingResult mResult = executeMatchAndStoreTheResult(result);
            String referenceId = mResult.getReferenceId();
            if (referenceId != null) {
                return correlateUsingKnownReferenceId(referenceId, result);
            } else {
                return createUncertainResult(mResult, result);
            }
        }

        // Note that the result is stored just to memory objects - nothing is set in the repository
        private @NotNull MatchingResult executeMatchAndStoreTheResult(OperationResult result)
                throws SchemaException, ConfigurationException, CommunicationException, SecurityViolationException {
            MatchingRequest mRequest =
                    new MatchingRequest(
                            prepareIdMatchObjectFromContext());
            MatchingResult mResult = service.executeMatch(mRequest, result);
            LOGGER.trace("Matching result:\n{}", mResult.debugDumpLazily(1));

            // FIXME this assumes we are the top-level correlator!
            IdMatchCorrelatorStateType correlatorState = createCorrelatorState(mResult);
            correlationContext.setCorrelatorState(correlatorState); // not connected with the resource object
            ShadowUtil.setCorrelatorState(resourceObject, correlatorState.clone());

            return mResult;
        }

        @NotNull Confidence checkCandidateOwner(@NotNull FocusType candidateOwner, OperationResult result)
                throws SchemaException, CommunicationException, SecurityViolationException, ConfigurationException {
            MatchingResult mResult = executeMatchAndStoreTheResult(result);
            String definiteReferenceId = mResult.getReferenceId();
            if (definiteReferenceId != null) {
                return checkCandidateOwnerByReferenceId(candidateOwner, definiteReferenceId) ? full() : zero();
            } else {
                for (PotentialMatch potentialMatch : mResult.getPotentialMatches()) {
                    String referenceId = potentialMatch.getReferenceId();
                    if (referenceId != null) {
                        if (checkCandidateOwnerByReferenceId(candidateOwner, referenceId)) {
                            return Confidence.of(getConfidenceValue(potentialMatch));
                        }
                    }
                }
                return zero();
            }
        }

        private @NotNull IdMatchCorrelatorStateType createCorrelatorState(MatchingResult mResult) {
            IdMatchCorrelatorStateType state = new IdMatchCorrelatorStateType();
            state.setReferenceId(mResult.getReferenceId());
            state.setMatchRequestId(mResult.getMatchRequestId());
            return state;
        }

        private CorrelationResult correlateUsingKnownReferenceId(String referenceId, OperationResult result)
                throws ConfigurationException, SchemaException  {
            var focus = findFocusWithGivenReferenceId(referenceId, result);
            if (focus != null) {
                // Note that ID Match does not provide confidence values for certain matches
                // And we don't support custom confidence values here. Hence always 1.0.
                return CorrelationResult.of(
                        CandidateOwners.from(
                                List.of(new CandidateOwner.ObjectBased(focus, referenceId, 1.0))));
            } else {
                return CorrelationResult.empty();
            }
        }

        private ObjectType findFocusWithGivenReferenceId(String referenceId, OperationResult result)
                throws ConfigurationException, SchemaException {
            ReferenceIdResolutionConfig referenceIdResolutionConfig = new ReferenceIdResolutionConfig(correlationContext);
            return findFocusDirectly(referenceId, referenceIdResolutionConfig.referenceIdPropertyPath, result);
        }

        private ObjectType findFocusDirectly(String referenceId, ItemPath referenceIdPropertyPath, OperationResult result)
                throws SchemaException {
            Class<? extends ObjectType> focusType = correlationContext.getFocusType();
            var matching = beans.cacheRepositoryService.searchObjects(
                    focusType,
                    PrismContext.get().queryFor(focusType)
                            .item(referenceIdPropertyPath)
                            .eq(referenceId)
                            .build(),
                    createRetrieveCollection(),
                    result);
            if (matching.size() > 1) {
                throw new IllegalStateException(
                        String.format("%d focus objects found with the reference ID property '%s' having"
                                        + " the value of '%s'. The property is supposed to have unique values. Objects: %s",
                                matching.size(), referenceIdPropertyPath, referenceId, matching));
            } else if (matching.size() == 1) {
                return matching.get(0).asObjectable();
            } else {
                return null;
            }
        }

        private boolean checkCandidateOwnerByReferenceId(FocusType candidateOwner, String referenceId)
                throws ConfigurationException {
            ReferenceIdResolutionConfig referenceIdResolutionConfig = new ReferenceIdResolutionConfig(correlationContext);
            return checkCandidateOwnerDirectly(candidateOwner, referenceId, referenceIdResolutionConfig.referenceIdPropertyPath);
        }

        private boolean checkCandidateOwnerDirectly(FocusType candidateOwner, String referenceId, ItemPath referenceIdPath) {
            Object candidateReferenceId = candidateOwner.asPrismObject().getPropertyRealValue(referenceIdPath, Object.class);
            return candidateReferenceId != null && candidateReferenceId.toString().equals(referenceId);
        }

        /** Converts internal {@link MatchingResult} into "externalized form" of {@link CorrelationResult}. */
        private @NotNull CorrelationResult createUncertainResult(
                @NotNull MatchingResult mResult,
                @NotNull OperationResult result)
                throws SchemaException, ConfigurationException {
            CandidateOwners candidateOwners = new CandidateOwners();
            for (PotentialMatch potentialMatch : mResult.getPotentialMatches()) {
                String referenceId = potentialMatch.getReferenceId();
                if (referenceId != null) {
                    var candidate = findFocusWithGivenReferenceId(referenceId, result);
                    if (candidate != null) {
                        candidateOwners.putObject(
                                candidate,
                                referenceId,
                                getConfidenceValue(potentialMatch));
                    } else {
                        LOGGER.debug("Potential match with no corresponding user: {}", potentialMatch);
                    }
                }
            }
            return CorrelationResult.of(candidateOwners);
        }

        private double getConfidenceValue(PotentialMatch potentialMatch) {
            double confidenceLimit = Objects.requireNonNullElse(
                    configurationBean.getCandidateConfidenceLimit(), DEFAULT_CONFIDENCE_LIMIT);
            Double confidence = potentialMatch.getConfidenceScaledToOne();
            return confidence != null && confidence <= confidenceLimit ? confidence : confidenceLimit;
        }
    }

    @Override
    public void update(@NotNull CorrelationContext correlationContext, @NotNull OperationResult result)
            throws SchemaException, CommunicationException, SecurityViolationException, ConfigurationException {

        LOGGER.trace("Updating:\n{}", correlationContext.debugDumpLazily(1));
        new UpdateOperation(correlationContext.asShadowCtx())
                .execute(result);
    }

    private class UpdateOperation extends Operation {

        UpdateOperation(@NotNull CorrelationContext.Shadow correlationContext) {
            super(correlationContext);
        }

        void execute(OperationResult result)
                throws SchemaException, CommunicationException, SecurityViolationException, ConfigurationException {

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
            throws SchemaException, CommunicationException, SecurityViolationException, ObjectNotFoundException,
            ExpressionEvaluationException, ConfigurationException {
        ShadowType shadow = CorrelatorUtil.getShadowFromCorrelationCase(aCase);
        beans.provisioningService.applyDefinition(shadow.asPrismObject(), task, result);
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

    /**
     * Shadow must have resource definitions applied.
     */
    private IdMatchObject prepareIdMatchObject(@NotNull FocusType preFocus, @NotNull ShadowType shadow)
            throws SchemaException, ConfigurationException {
        return new IdMatchObjectCreator(correlatorContext, preFocus, shadow)
                .create();
    }

    @Override
    public @NotNull Collection<CorrelationPropertyDefinition> getCorrelationPropertiesDefinitions(
            PrismObjectDefinition<? extends FocusType> focusDefinition, @NotNull Task task, @NotNull OperationResult result) {
        return List.of(); // Implement if really needed. (For the time being, properties from pre-focus are sufficient.)
    }

    private class ReferenceIdResolutionConfig { // TODO better name
        @NotNull private final ItemPath referenceIdPropertyPath;

        private ReferenceIdResolutionConfig(@NotNull CorrelationContext correlationContext) throws ConfigurationException {
            ItemPathType pathBean = configurationBean.getReferenceIdProperty();
            if (pathBean == null) {
                throw new ConfigurationException("Reference ID property path must be specified in "
                        + getDefaultContextDescription(correlationContext));
            }

            referenceIdPropertyPath = pathBean.getItemPath();
        }
    }
}
