/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelatorConfiguration;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelationResult;

public class CorrelatorUtil {

    @NotNull
    public static CorrelationResult createCorrelationResult(List<? extends ObjectType> candidates) {
        if (candidates.isEmpty()) {
            return CorrelationResult.noOwner();
        } else if (candidates.size() == 1) {
            return CorrelationResult.existingOwner(candidates.get(0));
        } else {
            return CorrelationResult.uncertain();
        }
    }

    public static <F extends ObjectType> void addCandidates(List<F> allCandidates, List<F> candidates, Trace logger) {
        for (F candidate : candidates) {
            if (!containsOid(allCandidates, candidate.getOid())) {
                logger.trace("Found candidate owner {}", candidate);
                allCandidates.add(candidate);
            } else {
                logger.trace("Candidate owner {} already processed", candidate);
            }
        }
    }

    public static <F extends ObjectType> boolean containsOid(List<F> allCandidates, String oid) {
        for (F existing : allCandidates) {
            if (existing.getOid().equals(oid)) {
                return true;
            }
        }
        return false;
    }

    public static VariablesMap getVariablesMap(
            ObjectType focus,
            ShadowType resourceObject,
            CorrelationContext correlationContext) {
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(
                focus,
                resourceObject,
                correlationContext.getResource(),
                correlationContext.getSystemConfiguration());
        variables.put(ExpressionConstants.VAR_CORRELATION_CONTEXT, correlationContext, CorrelationContext.class);
        variables.put(ExpressionConstants.VAR_CORRELATION_STATE, correlationContext.getCorrelationState(),
                AbstractCorrelationStateType.class);
        return variables;
    }

    /**
     * Extracts {@link CorrelatorConfiguration} objects from given "correlators" structure (both typed and untyped).
     */
    public static @NotNull Collection<CorrelatorConfiguration> getConfigurations(@NotNull CorrelatorsType correlation) {
        List<CorrelatorConfiguration> configurations =
                Stream.of(
                                correlation.getNone().stream(),
                                correlation.getFilter().stream(),
                                correlation.getExpression().stream(),
                                correlation.getIdMatch().stream())
                        .flatMap(s -> s)
                        .map(CorrelatorConfiguration.TypedCorrelationConfiguration::new)
                        .collect(Collectors.toCollection(ArrayList::new));

        if (correlation.getExtension() != null) {
            //noinspection unchecked
            Collection<Item<?, ?>> items = correlation.getExtension().asPrismContainerValue().getItems();
            for (Item<?, ?> item : items) {
                for (PrismValue value : item.getValues()) {
                    // TODO better type safety checks (provide specific exceptions)
                    if (value instanceof PrismContainerValue) {
                        //noinspection unchecked
                        configurations.add(
                                new CorrelatorConfiguration.UntypedCorrelationConfiguration(
                                        item.getElementName(),
                                        ((PrismContainerValue<AbstractCorrelatorType>) value)));
                    }
                }
            }
        }
        return configurations;
    }
}
