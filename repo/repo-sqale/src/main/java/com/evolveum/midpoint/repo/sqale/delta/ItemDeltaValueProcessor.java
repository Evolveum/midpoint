/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta;


import static java.util.stream.Collectors.toList;

import java.util.Collection;

import com.evolveum.midpoint.prism.ItemModifyResult;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Applies item delta values to an item and arranges necessary SQL changes using update context.
 * This typically means adding set clauses to the update but can also mean adding rows
 * for containers, etc.
 * This kind of item delta processor does not resolve multi-part item paths, see other
 * subclasses of {@link ItemDeltaProcessor} for that.
 * The class also declares more specific methods for applying values (add, replace, delete),
 * because in some scenarios we work with items and not with item delta modifications anymore.
 *
 * Implementations populate updates contained in the {@link #context} using
 * {@link SqaleUpdateContext#set} method (these will be executed later) or issue insert/delete
 * statements which are executed immediately which is responsibility of the processor.
 *
 * @param <T> expected type of the real value for the modification (after optional conversion)
 */
public abstract class ItemDeltaValueProcessor<T> implements ItemDeltaProcessor {

    protected final SqaleUpdateContext<?, ?, ?> context;

    protected ItemDeltaValueProcessor(SqaleUpdateContext<?, ?, ?> context) {
        this.context = context;
    }

    /**
     * Default process implementation, most generic case covering especially multi-values
     * stored in separate rows.
     * This works when implementations of {@link #deleteRealValues} and {@link #addRealValues}
     * are independent, it's not usable for array update where a single `SET` clause is allowed.
     */
    @Override
    public ProcessingHint process(ItemDelta<?, ?> modification) throws RepositoryException, SchemaException {
        if (modification.isReplace()) {
            setRealValues(modification.getRealValuesToReplace());
            return DEFAULT_PROCESSING;
        }


        var applyResults = modification.applyResults();


        if (applyResults != null && useRealDeltaApplyResults()) {
            for (var result : applyResults) {
                processResult(result);
            }
            // if apply results is empty, we may skip full object altogether
            return DEFAULT_PROCESSING;
        }


        // if it was replace, we don't get here, but delete+add can be used together
        if (modification.isDelete()) {
            deleteRealValues(modification.getValuesToDelete().stream().map(v -> v.getRealValue()).toList());
        }
        if (modification.isAdd()) {
            addRealValues(modification.getRealValuesToAdd());
        }
        return DEFAULT_PROCESSING;
    }

    protected boolean useRealDeltaApplyResults() {
        return true;
    }

    private void processResult(ItemModifyResult<?> result) throws SchemaException {
        switch (result.operation()) {
            case ADDED -> addRealValue(convertRealValue(result.finalRealValue()));
            case DELETED -> deleteRealValue(convertRealValue(result.finalRealValue()));
            case MODIFIED -> modifyRealValue(convertRealValue(result.finalRealValue()));

        }


    }

    protected void modifyRealValue(T realValue) throws SchemaException  {
        deleteRealValue(realValue);
        addRealValue(realValue);
    }

    protected void deleteRealValue(T realValue) {
        throw new UnsupportedOperationException("delete real value not implemented");
    }

    protected void addRealValue(T realValue) throws SchemaException {
        throw new UnsupportedOperationException("addValue not implemented");
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
    public void setRealValues(Collection<?> values) throws SchemaException {
        // general scenario usable for multi-value cases
        delete();
        addRealValues(values);
    }

    /** Adds the provided real values to the database, implements ADD modification. */
    public void addRealValues(Collection<?> values) throws SchemaException {
        addValues(values.stream().map(this::convertRealValue).collect(toList()));
    }

    public void addValues(Collection<T> values) throws SchemaException {
        throw new UnsupportedOperationException("addValues not implemented");
    }

    /** Adds the provided real values to the database, implements ADD modification. */
    public void deleteRealValues(Collection<?> values) {
        deleteValues(values.stream().map(this::convertRealValue).collect(toList()));
    }

    public void deleteValues(Collection<T> values) {
        throw new UnsupportedOperationException("deleteValues not implemented");
    }

    /**
     * Resets the database columns or deletes sub-entities like refs, containers, etc.
     * This must be implemented to support clearing the columns of single-value embedded containers.
     */
    public abstract void delete();
}
