package com.evolveum.midpoint.smart.impl.mappings;

import java.util.Collection;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public record ValuesPair<S, F>(Collection<S> shadowValues, Collection<F> focusValues) {
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
}
