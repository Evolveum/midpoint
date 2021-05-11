/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaValueProcessor;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;

/**
 * Applies single item delta value to an item.
 * This class implements {@link #setRealValues} (for multiple values) and requires
 * {@link #setValue} (for a single value) to be implemented by the subclasses.
 *
 * This hierarchy branch should not need {@link #addRealValues} and {@link #deleteRealValues},
 * so it's not overridden and throws {@link UnsupportedOperationException}.
 */
public abstract class ItemDeltaSingleValueProcessor<T> extends ItemDeltaValueProcessor<T> {

    protected ItemDeltaSingleValueProcessor(SqaleUpdateContext<?, ?, ?> context) {
        super(context);
    }

    @Override
    public void process(ItemDelta<?, ?> modification) throws RepositoryException {
        T value = getAnyValue(modification);

        if (modification.isDelete() || value == null) {
            // Repo does not check deleted value for single-value properties.
            // This should be handled already by narrowing the modifications.
            delete();
        } else {
            // We treat add and replace the same way for single-value properties.
            setValue(value);
        }
    }

    /**
     * Often the single real value is necessary, optionally converted using
     * {@link #convertRealValue(Object)} to get expected type.
     * Either method can be overridden or not used at all depending on the complexity
     * of the concrete delta processor.
     */
    protected T getAnyValue(ItemDelta<?, ?> modification) {
        PrismValue anyValue = modification.getAnyValue();
        return anyValue != null ? convertRealValue(anyValue.getRealValue()) : null;
    }

    /** Sets the database columns to reflect the provided value (converted if necessary). */
    public abstract void setValue(T value);

    @Override
    public void setRealValues(Collection<?> values) {
        if (values.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple values when single value is expected: " + values);
        }

        setValue(values.isEmpty() ? null : convertRealValue(values.iterator().next()));
    }
}
