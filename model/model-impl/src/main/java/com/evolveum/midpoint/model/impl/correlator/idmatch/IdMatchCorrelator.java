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
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdMatchCorrelationStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdMatchCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;

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
        this.service = serviceOverride != null ?
                serviceOverride : IdMatchServiceImpl.instantiate(configuration);
        this.beans = beans;

        this.followOnCorrelatorConfiguration = getFollowOnConfiguration(configuration);

        LOGGER.trace("Instantiated the correlator with the configuration:\n{}", configuration.debugDumpLazily(1));
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
            return correlateUsingKnownReferenceId(resourceObject, correlationContext, task, result);
        } else {
            // later we will create a case object here
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
}
