/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import java.util.List;

import com.evolveum.midpoint.schema.util.ObjectSet;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class CorrelatorUtil {

    public static <F extends ObjectType> void addCandidates(ObjectSet<F> allCandidates, List<F> candidates, Trace logger) {
        for (F candidate : candidates) {
            if (allCandidates.add(candidate)) {
                logger.trace("Found candidate owner {}", candidate);
            } else {
                logger.trace("Candidate owner {} already processed", candidate);
            }
        }
    }

    public static VariablesMap getVariablesMap(
            ObjectType focus,
            ShadowType resourceObject,
            CorrelationContext correlationContext) {
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(
                focus,
                resourceObject,
                correlationContext instanceof CorrelationContext.Shadow shadow ? shadow.getResource() : null,
                correlationContext.getSystemConfiguration());
        variables.put(ExpressionConstants.VAR_CORRELATION_CONTEXT, correlationContext, CorrelationContext.class);
        variables.put(ExpressionConstants.VAR_CORRELATOR_STATE,
                correlationContext.getCorrelatorState(), AbstractCorrelatorStateType.class);
        return variables;
    }

    /** We assume to get the "full" shadow, i.e. shadowed resource object. */
    public static @NotNull ShadowType getShadowFromCorrelationCase(@NotNull CaseType aCase) throws SchemaException {
        return MiscUtil.requireNonNull(
                MiscUtil.castSafely(
                        ObjectTypeUtil.getObjectFromReference(aCase.getTargetRef()),
                        ShadowType.class),
                () -> new IllegalStateException("No shadow object in " + aCase));
    }

    public static @NotNull FocusType getPreFocusFromCorrelationCase(@NotNull CaseType aCase) throws SchemaException {
        CaseCorrelationContextType correlationContext =
                MiscUtil.requireNonNull(
                        aCase.getCorrelationContext(),
                        () -> new IllegalStateException("No correlation context in " + aCase));
        return MiscUtil.requireNonNull(
                MiscUtil.castSafely(
                        ObjectTypeUtil.getObjectFromReference(correlationContext.getPreFocusRef()),
                        FocusType.class),
                () -> new IllegalStateException("No pre-focus object in " + aCase));
    }
}
