/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licenced under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.mappings;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.prism.path.ItemPath;

import java.util.Collection;
import java.util.List;

/**
 * Simple value object representing a shadow and its owner.
 */
public record OwnedShadow(ShadowType shadow, FocusType owner) {

    public ValuesPair toValuesPair(ItemPath shadowAttrPath, ItemPath focusPropPath) {
        return new ValuesPair(
                getItemRealValues(shadow, shadowAttrPath),
                getItemRealValues(owner, focusPropPath));
    }

    private static Collection<?> getItemRealValues(ObjectType objectable, ItemPath itemPath) {
        var item = objectable.asPrismObject().findItem(itemPath);
        return item != null ? item.getRealValues() : List.of();
    }
}
