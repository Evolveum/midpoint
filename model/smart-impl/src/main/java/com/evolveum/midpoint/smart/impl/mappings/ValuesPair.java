package com.evolveum.midpoint.smart.impl.mappings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public record ValuesPair<S, F>(Collection<S> shadowValues, Collection<F> focusValues) {

    private static final Trace LOGGER = TraceManager.getTrace(ValuesPair.class);
    public SiSuggestMappingExampleType toSiExample(
            String applicationAttrDescriptivePath, String midPointPropertyDescriptivePath) {
        return new SiSuggestMappingExampleType()
                .application(toSiAttributeExample(applicationAttrDescriptivePath, shadowValues))
                .midPoint(toSiAttributeExample(midPointPropertyDescriptivePath, focusValues));
    }

    private @NotNull SiAttributeExampleType toSiAttributeExample(String path, Collection<?> values) {
        var example = new SiAttributeExampleType().name(path);
        example.getValue().addAll(stringify(values));
        return example;
    }

    private Collection<String> stringify(Collection<?> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();
    }

    /** Direction-aware accessor for source values from a pair. */
    public Collection<?> getSourceValues(MappingDirection direction) {
        var values = direction == MappingDirection.INBOUND ? shadowValues() : focusValues();
        return values != null ? values : List.of();
    }

    /** Direction-aware accessor for target values from a pair. */
    public Collection<?> getTargetValues(MappingDirection direction) {
        var values = direction == MappingDirection.INBOUND ? focusValues() : shadowValues();
        return values != null ? values : List.of();
    }

    /**
     * Checks if source values match their corresponding target values after type conversion.
     */
    public boolean doesConvertedSourceMatchTarget(
            MappingDirection direction,
            PrismPropertyDefinition<?> targetDef,
            Protector protector) {
        var sourceValues = getSourceValues(direction);
        var targetValues = getTargetValues(direction);

        if (sourceValues.size() != targetValues.size()) {
            return false;
        }

        var expectedTargetValues = new ArrayList<>(sourceValues.size());
        for (Object sourceValue : sourceValues) {
            try {
                Object converted = ExpressionUtil.convertValue(
                        targetDef.getTypeClass(), null, sourceValue, protector);
                if (converted != null) {
                    expectedTargetValues.add(converted);
                }
            } catch (Exception e) {
                LOGGER.trace("Value conversion failed ({}), assuming transformation is needed: {} (value: {})",
                        direction, e.getMessage(), sourceValue);
                return false;
            }
        }
        return MiscUtil.unorderedCollectionEquals(targetValues, expectedTargetValues);
    }

}
