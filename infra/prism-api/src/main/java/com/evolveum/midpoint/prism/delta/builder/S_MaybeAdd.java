/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.delta.builder;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismValue;

public interface S_MaybeAdd extends S_ItemEntry {

    S_ItemEntry add(Object... realValues);
    S_ItemEntry addRealValues(Collection<?> realValues);
    S_ItemEntry add(PrismValue... values);
    S_ItemEntry add(Collection<? extends PrismValue> values);
}
