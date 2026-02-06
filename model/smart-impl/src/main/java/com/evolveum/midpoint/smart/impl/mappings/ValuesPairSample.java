/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.mappings;

import java.util.Collection;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public record ValuesPairSample<S, F>(ItemPath focusPropertyPath, ItemPath shadowAttributePath,
        Collection<ValuesPair<S, F>> pairs, MappingDirection direction) {

    private static final Trace LOGGER = TraceManager.getTrace(ValuesPairSample.class);

    public static SampleOf of(ItemPath focusPropertyPath, ItemPath shadowAttributePath, MappingDirection direction) {
        return ownedShadows -> new ValuesPairSample<>(focusPropertyPath, shadowAttributePath,
                ownedShadows.stream()
                        .map(os -> os.toValuesPair(shadowAttributePath, focusPropertyPath))
                        .collect(Collectors.toList()), direction);
    }

    /** Direction-aware accessor for target definition. */
    public PrismPropertyDefinition<?> getTargetDefinition(
            PrismObjectDefinition<?> focusTypeDefinition,
            ResourceObjectTypeDefinition objectTypeDefinition) {
        if (direction == MappingDirection.INBOUND) {
            return focusTypeDefinition.findPropertyDefinition(focusPropertyPath());
        } else {
            var shadowAttrName = shadowAttributePath().rest().asSingleNameOrFail();
            return objectTypeDefinition.findSimpleAttributeDefinition(shadowAttrName);
        }
    }

    /**
     * Returns true if target data is missing.
     * Data is considered missing if less than MISSING_DATA_THRESHOLD of pairs have non-empty target values.
     */
    public boolean isTargetDataMissing(float missingDataThreshold) {
        if (pairs.isEmpty()) {
            return true;
        }
        long countWithValues = pairs.stream()
                .filter(pair -> !pair.getTargetValues(direction).isEmpty())
                .count();
        double percentageWithData = (double) countWithValues / pairs.size();
        return percentageWithData < missingDataThreshold;
    }

    /**
     * Returns true if source data is missing.
     * Data is considered missing if less than MISSING_DATA_THRESHOLD of pairs have non-empty source values.
     */
    public boolean isSourceDataMissing(float missingDataThreshold) {
        if (pairs.isEmpty()) {
            return true;
        }
        long countWithValues = pairs.stream()
                .filter(pair -> !pair.getSourceValues(direction).isEmpty())
                .count();
        double percentageWithData = (double) countWithValues / pairs.size();
        return percentageWithData < missingDataThreshold;
    }

    /**
     * Checks if all source values match their corresponding target values after type conversion.
     */
    public boolean allSourcesMatchTargets(
            PrismObjectDefinition<?> focusTypeDefinition,
            ResourceObjectTypeDefinition objectTypeDefinition,
            Protector protector) {
        PrismPropertyDefinition<?> targetDef = getTargetDefinition(focusTypeDefinition, objectTypeDefinition);
        if (targetDef == null) {
            LOGGER.trace("No definition available; cannot verify value matching.");
            return false;
        }
        return pairs.stream().allMatch(pair -> pair.doesConvertedSourceMatchTarget(direction, targetDef, protector));
    }

    public interface SampleOf {
        ValuesPairSample<?, ?> from(Collection<OwnedShadow> ownedShadows);
    }
}
