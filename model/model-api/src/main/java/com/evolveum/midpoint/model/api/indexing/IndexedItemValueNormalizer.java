/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.indexing;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IndexedItemNormalizationDefinitionType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * A {@link ValueNormalizer} configured for an indexed item.
 *
 * Terminological note: From the point of view of configuration, one may call this object a _normalization_.
 * It is used to do the actual normalization of values, hence the name of _normalizer_.
 *
 * @see IndexedItemNormalizationDefinitionType
 */
public interface IndexedItemValueNormalizer extends ValueNormalizer {

    /** Returns the name of the normalizer (normalization). */
    @NotNull String getName();

    /** Is this normalizer (normalization) the default one configured for the given item? */
    boolean isDefault();

    /** Returns the qualified name of the indexed version of the item, corresponding to this normalizer (normalization). */
    ItemName getIndexItemName();

    /** Returns the full path to the indexed version of the item. */
    ItemPath getIndexItemPath();

    /**
     * Returns the definition of the indexed version of the item.
     *
     * Currently, it is always a definition of a string. Later this may be changed.
     */
    @NotNull PrismPropertyDefinition<?> getIndexItemDefinition();
}
