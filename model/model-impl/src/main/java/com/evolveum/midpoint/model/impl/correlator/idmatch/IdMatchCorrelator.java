/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch;

import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.api.correlator.Correlator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdMatchCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A correlator based on an external service providing ID Match API.
 * (https://spaces.at.internet2.edu/display/cifer/SOR-Registry+Strawman+ID+Match+API)
 */
class IdMatchCorrelator implements Correlator {

    private static final Trace LOGGER = TraceManager.getTrace(IdMatchCorrelator.class);

    /**
     * Configuration of the correlator.
     */
    @NotNull private final IdMatchCorrelatorType configuration;

    /**
     * Service that resolves "reference ID" for resource objects being correlated.
     */
    @NotNull private final IdMatchService service;

    /**
     * @param serviceOverride An instance of {@link IdMatchService} that should be used instead of the default one.
     *                        Used for unit testing.
     */
    IdMatchCorrelator(
            @NotNull IdMatchCorrelatorType configuration,
            @Nullable IdMatchService serviceOverride) throws ConfigurationException {
        this.configuration = configuration;
        this.service = serviceOverride != null ?
                serviceOverride : IdMatchServiceImpl.instantiate(configuration);

        LOGGER.trace("Instantiated the correlator with the configuration:\n{}", configuration.debugDumpLazily(1));
    }

    @Override
    public CorrelationResult correlate(@NotNull ShadowType resourceObject, @NotNull Task task, @NotNull OperationResult result) {

        LOGGER.trace("Correlating the resource object:\n{}", resourceObject.debugDumpLazily(1));

        MatchingResult mResult = service.executeMatch(resourceObject.getAttributes(), result);
        LOGGER.trace("Matching result:\n{}", mResult.debugDumpLazily(1));

        if (mResult.getReferenceId() != null) {
            return correlateUsingKnownReferenceId(mResult.getReferenceId(), task, result);
        } else {
            // later we will create a case object here
            return CorrelationResult.uncertain();
        }
    }

    private CorrelationResult correlateUsingKnownReferenceId(String referenceId, Task task, OperationResult result) {
        assert configuration.getCorrelator() != null;
        // TODO invoke the embedded correlator
        return CorrelationResult.uncertain();
    }
}
