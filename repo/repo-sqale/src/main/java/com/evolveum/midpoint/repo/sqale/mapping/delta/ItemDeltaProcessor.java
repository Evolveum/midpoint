/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping.delta;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sqale.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;

/**
 * Processes delta modification and arranges necessary SQL changes using update context.
 * This typically means adding set clauses to the update but can also mean adding rows
 * for containers, etc.
 *
 * @param <T> expected type of the real value for the modification (after optional conversion)
 */
public abstract class ItemDeltaProcessor<T> {

    protected final SqaleUpdateContext<?, ?, ?> context;

    protected ItemDeltaProcessor(SqaleUpdateContext<?, ?, ?> context) {
        this.context = context;
    }

    public abstract void process(ItemDelta<?, ?> modification) throws RepositoryException;

    /**
     * Often the single real value is necessary, optionally transformed using
     * {@link #transformRealValue(Object)} to get expected type.
     * Either method can be overridden or not used at all depending on the complexity
     * of the concrete delta processor.
     */
    protected T getAnyValue(ItemDelta<?, ?> modification) {
        PrismValue anyValue = modification.getAnyValue();
        return anyValue != null ? transformRealValue(anyValue.getRealValue()) : null;
    }

    protected T transformRealValue(Object realValue) {
        //noinspection unchecked
        return (T) realValue;
    }
}
