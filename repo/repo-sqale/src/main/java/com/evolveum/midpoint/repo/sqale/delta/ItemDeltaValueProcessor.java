/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

/**
 * Applies item delta values to an item and arranges necessary SQL changes using update context.
 * This typically means adding set clauses to the update but can also mean adding rows
 * for containers, etc.
 * This kind of item delta processor does not resolve multi-part item paths, see other
 * subclasses of {@link ItemDeltaProcessor} for that.
 *
 * @param <T> expected type of the real value for the modification (after optional conversion)
 */
public interface ItemDeltaValueProcessor<T> extends ItemDeltaProcessor {

    /** Default conversion for one value is a mere type cast, override as necessary. */
    @Nullable
    default T convertRealValue(Object realValue) {
        //noinspection unchecked
        return (T) realValue;
    }

    /**
     * Sets the database columns to reflect the provided real values (may be conversion).
     * This is a general case covering both multi-value and single-value items.
     */
    void setRealValues(Collection<?> value);

    /** Resets the database columns, exposed for the needs of container processing. */
    void delete();
}
