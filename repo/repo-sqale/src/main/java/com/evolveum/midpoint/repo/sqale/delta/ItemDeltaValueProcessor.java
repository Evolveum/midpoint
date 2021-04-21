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

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sqale.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;

/**
 * Applies item delta values to an item and arranges necessary SQL changes using update context.
 * This typically means adding set clauses to the update but can also mean adding rows
 * for containers, etc.
 * This kind of item delta processor does not resolve multi-part item paths, see other
 * subclasses of {@link ItemDeltaProcessor} for that.
 * The class also declares more specific methods for applying values (add, replace, delete),
 * because in some scenarios we work with items and not with item delta modifications anymore.
 *
 * @param <T> expected type of the real value for the modification (after optional conversion)
 */
public abstract class ItemDeltaValueProcessor<T> implements ItemDeltaProcessor {

    protected final SqaleUpdateContext<?, ?, ?> context;

    protected ItemDeltaValueProcessor(SqaleUpdateContext<?, ?, ?> context) {
        this.context = context;
    }

    /** Default process implementation, most generic case covering especially multi-values. */
    @Override
    public void process(ItemDelta<?, ?> modification) throws RepositoryException {
        if (modification.isReplace()) {
            setRealValues(modification.getRealValuesToReplace());
            return;
        }

        // if it was replace, we don't get here, but add+delete can be used together
        if (modification.isAdd()) {
            addRealValues(modification.getRealValuesToAdd());
        }
        if (modification.isDelete()) {
            deleteRealValues(modification.getRealValuesToDelete());
        }
    }

    /** Default conversion for one value is a mere type cast, override as necessary. */
    @Nullable
    public T convertRealValue(Object realValue) {
        //noinspection unchecked
        return (T) realValue;
    }

    /**
     * Sets the provided real values in the database, implements REPLACE modification.
     * This may involve setting the value of some columns or delete/insert of sub-entities.
     * This is a general case covering both multi-value and single-value items.
     */
    public void setRealValues(Collection<?> values) {
        // general scenario usable for multi-value cases
        delete();
        addRealValues(values);
    }

    /** Adds the provided real values to the database, implements ADD modification. */
    public void addRealValues(Collection<?> values) {
        addValues(values.stream().map(this::convertRealValue).collect(toList()));
    }

    public void addValues(Collection<T> values) {
        throw new UnsupportedOperationException("deleteRealValues not implemented");
    }

    /** Adds the provided real values to the database, implements ADD modification. */
    public void deleteRealValues(Collection<?> values) {
        deleteValues(values.stream().map(this::convertRealValue).collect(toList()));
    }

    public void deleteValues(Collection<T> values) {
        throw new UnsupportedOperationException("deleteRealValues not implemented");
    }

    /** Resets the database columns or deletes sub-entities like refs, containers, etc. */
    public abstract void delete();
}
