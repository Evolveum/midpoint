/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.mappings;

import java.util.Collection;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.path.ItemPath;

public record ValuesPairSample<S, F>(ItemPath focusPropertyPath, ItemPath shadowAttributePath,
        Collection<ValuesPair<S, F>> pairs) {

    public static SampleOf of(ItemPath focusPropertyPath, ItemPath shadowAttributePath) {
        return ownedShadows -> new ValuesPairSample<>(focusPropertyPath, shadowAttributePath,
                ownedShadows.stream()
                        .map(os -> os.toValuesPair(shadowAttributePath, focusPropertyPath))
                        .collect(Collectors.toList()));
    }

    public interface SampleOf {
        ValuesPairSample<?, ?> from(Collection<OwnedShadow> ownedShadows);
    }
}
