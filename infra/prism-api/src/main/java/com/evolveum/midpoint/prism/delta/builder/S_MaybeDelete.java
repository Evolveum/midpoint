/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.delta.builder;

import com.evolveum.midpoint.prism.PrismValue;

import java.util.Collection;

/**
 * @author mederly
 */
public interface S_MaybeDelete extends S_ItemEntry {
    S_ItemEntry delete(Object... realValues);
    S_ItemEntry delete(PrismValue... values);
    S_ItemEntry deleteRealValues(Collection<?> realValues);
    S_ItemEntry delete(Collection<? extends PrismValue> values);
}
