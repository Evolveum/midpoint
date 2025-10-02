package com.evolveum.midpoint.smart.impl.mappings;

import java.util.Collection;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.smart.impl.DescriptiveItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public record ValuesPair(Collection<?> shadowValues, Collection<?> focusValues) {

    public SiSuggestMappingExampleType toSiExample(
            DescriptiveItemPath applicationAttrNameBean, DescriptiveItemPath midPointPropertyNameBean) {
        return new SiSuggestMappingExampleType()
                .application(toSiAttributeExample(applicationAttrNameBean, shadowValues))
                .midPoint(toSiAttributeExample(midPointPropertyNameBean, focusValues));
    }

    private @NotNull SiAttributeExampleType toSiAttributeExample(DescriptiveItemPath path, Collection<?> values) {
        var example = new SiAttributeExampleType().name(path.asString());
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
