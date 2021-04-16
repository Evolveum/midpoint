/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta;

import static java.util.stream.Collectors.toList;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

/**
 * Applies item delta values to an item and arranges necessary SQL changes using update context.
 * This typically means adding set clauses to the update but can also mean adding rows
 * for containers, etc.
 * This kind of item delta processor does not resolve multi-part item paths, see other
 * subclasses of {@link ItemDeltaProcessor} for that.
 * The interface also declares more specific methods for applying values (add, replace, delete),
 * because in some scenarios we work with items and not with item delta modifications anymore.
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
     * Sets the provided real values in the database, implements REPLACE modification.
     * This may involve setting the value of some columns or delete/insert of sub-entities.
     * This is a general case covering both multi-value and single-value items.
     */
    default void setRealValues(Collection<?> values) {
        // general scenario usable for multi-value cases
        delete();
        addRealValues(values);
    }

    /** Adds the provided real values to the database, implements ADD modification. */
    default void addRealValues(Collection<?> values) {
        addValues(values.stream().map(this::convertRealValue).collect(toList()));
    }

    default void addValues(Collection<T> values) {
        throw new UnsupportedOperationException("deleteRealValues not implemented");
    }

    /** Adds the provided real values to the database, implements ADD modification. */
    default void deleteRealValues(Collection<?> values) {
        deleteValues(values.stream().map(this::convertRealValue).collect(toList()));
    }

    default void deleteValues(Collection<T> values) {
        throw new UnsupportedOperationException("deleteRealValues not implemented");
    }

    /** Resets the database columns or deletes sub-entities like refs, containers, etc. */
    void delete();
}
