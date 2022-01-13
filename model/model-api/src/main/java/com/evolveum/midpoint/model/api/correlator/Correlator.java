/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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
     * @param resourceObject Resource object to correlate (should contain attributes, and be shadowed)
     * @param correlationContext Additional information about the overall context for correlation (e.g. type of focal objects)
     * @param task Task in context of which the correlation takes place
     * @param result Operation result where the method should record its operation
     */
    CorrelationResult correlate(@NotNull ShadowType resourceObject, @NotNull CorrelationContext correlationContext,
            @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException;
}
