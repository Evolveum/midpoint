/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Converts two-state property from "simulated" to "real" form.
 *
 * Used for activation status and lockout status conversion.
 */
class TwoStateSimulatedToRealConverter<T> {

    private static final Trace LOGGER = TraceManager.getTrace(TwoStateSimulatedToRealConverter.class);

    @NotNull private final List<?> simulatedPositiveValues;
    @NotNull private final List<?> simulatedNegativeValues;
    @NotNull private final T resolvedPositiveValue;
    @NotNull private final T resolvedNegativeValue;
    @NotNull private final String description;
    @NotNull private final ProvisioningContext ctx;

    TwoStateSimulatedToRealConverter(@NotNull List<?> simulatedPositiveValues, @NotNull List<?> simulatedNegativeValues,
            @NotNull T resolvedPositiveValue, @NotNull T resolvedNegativeValue, @NotNull String description,
            @NotNull ProvisioningContext ctx) {
        this.simulatedPositiveValues = simulatedPositiveValues;
        this.simulatedNegativeValues = simulatedNegativeValues;
        this.resolvedPositiveValue = resolvedPositiveValue;
        this.resolvedNegativeValue = resolvedNegativeValue;
        this.description = description;
        this.ctx = ctx;
    }

    public T convert(Collection<Object> simulatingAttributeValues, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        if (MiscUtil.isNoValue(simulatingAttributeValues)) {
            return convertNull(result);
        } else {
            return convertNonNull(simulatingAttributeValues, result);
        }
    }

    @Nullable
    private T convertNull(OperationResult result) {
        if (MiscUtil.hasNoValue(simulatedNegativeValues)) {
            return resolvedNegativeValue;
        }

        if (MiscUtil.hasNoValue(simulatedPositiveValues)) {
            return resolvedPositiveValue;
        }

        LOGGER.warn("The {} does not provide definition for null value of simulated '{}' attribute",
                ctx.getResource(), description);
        result.recordPartialError("The " + ctx.getResource()
                + " does not provide definition for null value of simulated '" + description + "' attribute");

        return null;
    }

    @Nullable
    private T convertNonNull(Collection<Object> simulatingAttributeValues, OperationResult result) {

        if (simulatingAttributeValues.size() > 1) {
            LOGGER.warn("An object on {} has {} values for simulated {} attribute, expecting just one value",
                    ctx.getResource(), description, simulatingAttributeValues.size());
            result.recordPartialError("An object on " + ctx.getResource() + " has " + simulatingAttributeValues.size() +
                    " values for simulated " + description + " attribute, expecting just one value");
        }
        Object simulatingAttributeValue = simulatingAttributeValues.iterator().next();

        for (Object negativeValue : simulatedNegativeValues) {
            if (negativeValue.equals(String.valueOf(simulatingAttributeValue))) { // TODO MID-3374: implement seriously
                return resolvedNegativeValue;
            }
        }

        for (Object positiveValue : simulatedPositiveValues) {
            if ("".equals(positiveValue) || positiveValue.equals(String.valueOf(simulatingAttributeValue))) { // TODO MID-3374: implement seriously
                return resolvedPositiveValue;
            }
        }
        return null;
    }
}
