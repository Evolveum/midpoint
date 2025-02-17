/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.Collection;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaValueProcessor;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Common delta processor logic for storing based on the final value of the property.
 * When the delta processor is run, the modification has been already applied to the prism object.
 * This is typically used for storing multi-value item in a single column.
 * This may not be efficient when the amount of values is too big, but it has to be really big
 * to be of consequence (my wild guess is 100-1000 can be still OK).
 *
 * Subclasses have to implement {@link #setRealValues(Collection)} and {@link #delete()} methods.
 *
 * @param <T> expected type of the real value for the modification (after optional conversion)
 */
public abstract class FinalValueDeltaProcessor<T> extends ItemDeltaValueProcessor<T> {

    /**
     * @param <Q> entity query type from which the attribute is resolved
     * @param <R> row type related to {@link Q}
     */
    public <Q extends FlexibleRelationalPathBase<R>, R> FinalValueDeltaProcessor(
            SqaleUpdateContext<?, Q, R> context) {
        super(context);
    }

    @Override
    public ProcessingHint process(ItemDelta<?, ?> modification) throws RepositoryException, SchemaException {
        Item<PrismValue, ?> item = context.findValueOrItem(modification.getPath());
        Collection<?> realValues = item != null ? item.getRealValues() : null;

        if (realValues == null || realValues.isEmpty()) {
            delete();
        } else {
            // Whatever the operation is, we just set the new value here.
            setRealValues(realValues);
        }
        return DEFAULT_PROCESSING;
    }
}
