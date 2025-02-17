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
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Applies single item delta value to an item.
 * This class implements {@link #setRealValues} (for multiple values) and requires
 * {@link #setValue} (for a single value) to be implemented by the subclasses.
 *
 * This hierarchy branch should not need {@link #addRealValues} and {@link #deleteRealValues},
 * so it's not overridden and throws {@link UnsupportedOperationException}.
 *
 * @param <T> expected type of the real value for the modification (after optional conversion)
 */
public abstract class ItemDeltaSingleValueProcessor<T> extends ItemDeltaValueProcessor<T> {

    protected final boolean excludeFromFullObject;

    protected ItemDeltaSingleValueProcessor(SqaleUpdateContext<?, ?, ?> context) {
        this(context, false);
    }

    protected ItemDeltaSingleValueProcessor(SqaleUpdateContext<?, ?, ?> context, boolean excludeFromFullObject) {
        super(context);
        this.excludeFromFullObject = excludeFromFullObject;
    }

    @Override
    public ProcessingHint process(ItemDelta<?, ?> modification) throws RepositoryException, SchemaException {
        T value = getAnyValue(modification);

        if ((modification.isReplace() || modification.isAdd()) && value != null) {
            // We treat add and replace the same way for single-value properties.
            setValue(value);
        } else if (modification.isDelete() || value == null) {
            // Repo does not check deleted value for single-value properties.
            // This should be handled already by narrowing the modifications.
            delete();
        }
        return new ProcessingHint(excludeFromFullObject);
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
    public abstract void setValue(T value) throws SchemaException;

    @Override
    public void setRealValues(Collection<?> values) throws SchemaException {
        if (values.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple values when single value is expected: " + values);
        }

        setValue(values.isEmpty() ? null : convertRealValue(values.iterator().next()));
    }
}
