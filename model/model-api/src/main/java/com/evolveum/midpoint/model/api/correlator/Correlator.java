/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;

/**
 * Finds a focus object for given resource object.
 *
 * TODO Plus other responsibilities
 */
public interface Correlator {

    /**
     * Finds matching focus object (or potentially matching objects) for given resource object.
     *
     * We assume that the correlator is already configured. See {@link CorrelatorFactory}.
     *
     * @param correlationContext Additional information about the overall context for correlation (e.g. type of focal object`s)
     * @param result Operation result where the method should record its operation
     */
    @NotNull CorrelationResult correlate(
            @NotNull CorrelationContext correlationContext,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Checks whether the provided candidate focus object is the owner for given resource object.
     *
     * We assume that the correlator is already configured. See {@link CorrelatorFactory}.
     *
     * @param correlationContext Additional information about the overall context for correlation.
     * @param result Operation result where the method should record its operation
     *
     * @return The confidence value of the match.
     */
    double checkCandidateOwner(
            @NotNull CorrelationContext correlationContext,
            @NotNull FocusType candidateOwner,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Updates the internal state of the correlator with the "fresh" data from the resource.
     */
    @Experimental
    default void update(
            @NotNull CorrelationContext correlationContext,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        // Nothing to do by default. This method is needed only in very specific cases, e.g. when
        // there is an external state that needs to be updated.
    }

    /**
     * Resolves a correlation case using provided work item output.
     *
     * This includes the processing that needs to be done in the correlator.
     * For the majority of correlators, there's nothing to be done here.
     *
     * Correlators with external and/or internal state (like ID Match) can update that state here.
     *
     * @param outcomeUri It is the same value as in the case. It is mentioned explicitly just to show it's not null.
     */
    default void resolve(
            @NotNull CaseType aCase,
            @NotNull String outcomeUri,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, CommunicationException, SecurityViolationException, ObjectNotFoundException,
            ExpressionEvaluationException, ConfigurationException {
        // Nothing to do by default.
    }
}
