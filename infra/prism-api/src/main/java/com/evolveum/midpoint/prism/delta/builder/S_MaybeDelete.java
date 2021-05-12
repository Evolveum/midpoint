/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.delta.builder;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismValue;

/**
 * Using DELETE after ADD in fluent builder goes against the actual semantics that first
 * executes DELETE and then ADD - use the correct order to avoid deprecated methods.
 */
public interface S_MaybeDelete extends S_ItemEntry {
    @Deprecated
    S_ItemEntry delete(Object... realValues);
    @Deprecated
    S_ItemEntry delete(PrismValue... values);
    @Deprecated
    S_ItemEntry deleteRealValues(Collection<?> realValues);
    @Deprecated
    S_ItemEntry delete(Collection<? extends PrismValue> values);
}
