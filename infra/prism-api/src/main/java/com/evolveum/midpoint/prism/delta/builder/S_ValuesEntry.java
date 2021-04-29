/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.delta.builder;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;

/**
 * Note: When dealing with PolyStrings, the real values should be of PolyString, not of PolyStringType type.
 */
public interface S_ValuesEntry {

    // Note: the names in this interface are to be kept as simple as possible.
    //
    // An exception is addRealValues, deleteRealValues, replaceRealValues: they must have a different name because
    // Java cannot distinguish between Collection<? extends PrismValue> and Collection<?>.

    S_MaybeDelete add(Object... realValues);
    S_MaybeDelete addRealValues(Collection<?> realValues);
    S_MaybeDelete add(PrismValue... values);
    S_MaybeDelete add(Collection<? extends PrismValue> values);

    S_MaybeAdd delete(Object... realValues);
    S_MaybeAdd deleteRealValues(Collection<?> realValues);
    S_MaybeAdd delete(PrismValue... values);
    S_MaybeAdd delete(Collection<? extends PrismValue> values);

    S_ItemEntry replace(Object... realValues);
    S_ItemEntry replaceRealValues(Collection<?> realValues);
    S_ItemEntry replace(PrismValue... values);
    S_ItemEntry replace(Collection<? extends PrismValue> values);

    /**
     * Create proper modification type based on parameter. Plus means add, minus delete, zero means replace.
     */
    S_ItemEntry mod(PlusMinusZero plusMinusZero, Object... realValues);
    S_ItemEntry modRealValues(PlusMinusZero plusMinusZero, Collection<?> realValues);
    S_ItemEntry mod(PlusMinusZero plusMinusZero, Collection<? extends PrismValue> values);
    S_ItemEntry mod(PlusMinusZero plusMinusZero, PrismValue... values);

    S_ValuesEntry old(Object... realValues);
    S_ValuesEntry oldRealValues(Collection<?> realValues);
    <T> S_ValuesEntry oldRealValue(T realValue);
    S_ValuesEntry old(PrismValue... values);
    S_ValuesEntry old(Collection<? extends PrismValue> values);
}
