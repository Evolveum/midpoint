/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch;

import com.evolveum.midpoint.model.api.correlator.*;
import com.evolveum.midpoint.model.api.correlator.idmatch.IdMatchService;
import com.evolveum.midpoint.model.api.correlator.idmatch.MatchingResult;
import com.evolveum.midpoint.model.api.correlator.idmatch.PotentialMatch;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;
import static com.evolveum.midpoint.util.QNameUtil.qNameToUri;

/**
 * A correlator based on an external service providing ID Match API.
 * (https://spaces.at.internet2.edu/display/cifer/SOR-Registry+Strawman+ID+Match+API)
 */
class IdMatchCorrelator implements Correlator {

    private static final Trace LOGGER = TraceManager.getTrace(IdMatchCorrelator.class);

    /**
     * Configuration of the this correlator.
     */
    @NotNull private final IdMatchCorrelatorType configuration;

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
            @NotNull IdMatchCorrelatorType configuration,
            @Nullable IdMatchService serviceOverride,
            ModelBeans beans) throws ConfigurationException {
        this.configuration = configuration;
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
        } else if (TemporaryIdMatchServiceImpl.URL.equals(configuration.getUrl())) {
            return TemporaryIdMatchServiceImpl.INSTANCE;
        } else {
            return IdMatchServiceImpl.instantiate(configuration);
        }
    }

    private CorrelatorConfiguration getFollowOnConfiguration(@NotNull IdMatchCorrelatorType configuration)
            throws ConfigurationException {
        configCheck(configuration.getFollowOn() != null,
                "No 'follow on' correlator configured in %s", configuration);
        Collection<CorrelatorConfiguration> followOnConfigs = CorrelatorUtil.getConfigurations(configuration.getFollowOn());
        configCheck(followOnConfigs.size() == 1, "Not a single 'follow on' correlator configured: %s",
                followOnConfigs);
        return followOnConfigs.iterator().next();
    }

    @Override
    public CorrelationResult correlate(@NotNull ShadowType resourceObject, @NotNull CorrelationContext correlationContext,
            @NotNull Task task, @NotNull OperationResult result) throws ConfigurationException, SchemaException,
            ExpressionEvaluationException, CommunicationException, SecurityViolationException, ObjectNotFoundException {

        LOGGER.trace("Correlating the resource object:\n{}", resourceObject.debugDumpLazily(1));

        MatchingResult mResult = service.executeMatch(resourceObject.getAttributes(), result);
        LOGGER.trace("Matching result:\n{}", mResult.debugDumpLazily(1));

        correlationContext.setCorrelationState(
                createCorrelationState(mResult));

        if (mResult.getReferenceId() != null) {
            beans.correlationCaseManager.closeCaseIfExists(resourceObject, result);
            return correlateUsingKnownReferenceId(resourceObject, correlationContext, task, result);
        } else {
            beans.correlationCaseManager.createOrUpdateCase(
                    resourceObject,
                    createSpecificCaseContext(mResult),
                    result);
            return CorrelationResult.uncertain();
        }
    }

    private IdMatchCorrelationStateType createCorrelationState(MatchingResult mResult) {
        IdMatchCorrelationStateType state = new IdMatchCorrelationStateType(PrismContext.get());
        state.setReferenceId(mResult.getReferenceId());
        state.setMatchRequestId(mResult.getMatchRequestId());
        return state;
    }

    private CorrelationResult correlateUsingKnownReferenceId(
            ShadowType resourceObject, CorrelationContext correlationContext, Task task, OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {

        return beans.correlatorFactoryRegistry
                .instantiateCorrelator(followOnCorrelatorConfiguration, task, result)
                .correlate(resourceObject, correlationContext, task, result);
    }

    /**
     * Converts internal {@link MatchingResult} into "externalized" {@link IdMatchCorrelationContextType} bean
     * to be stored in the correlation case.
     */
    private @NotNull IdMatchCorrelationContextType createSpecificCaseContext(MatchingResult mResult) {
        IdMatchCorrelationContextType context = new IdMatchCorrelationContextType(PrismContext.get());
        for (PotentialMatch potentialMatch : mResult.getPotentialMatches()) {
            context.getPotentialMatch().add(
                    createPotentialMatchBean(potentialMatch));
        }
        return context;
    }

    private IdMatchCorrelationPotentialMatchType createPotentialMatchBean(PotentialMatch potentialMatch) {
        String id = potentialMatch.getReferenceId();
        return new IdMatchCorrelationPotentialMatchType(PrismContext.get())
                .uri(qNameToUri(
                        new QName(SchemaConstants.CORRELATION_NS, SchemaConstants.CORRELATION_OPTION_PREFIX + id)))
                .confidence(potentialMatch.getConfidence())
                .referenceId(id)
                .attributes(potentialMatch.getAttributes());
    }
}
