/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.update;

import com.querydsl.core.types.Path;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Update context for nested containers stored in the same table used by the parent context.
 *
 * @param <S> schema type of the object mapped by nested mapping
 * @param <Q> entity query type that holds the data for the mapped attributes
 * @param <R> row type related to the {@link Q}
 */
public class NestedContainerUpdateContext<S extends Containerable, Q extends FlexibleRelationalPathBase<R>, R>
        extends SqaleUpdateContext<S, Q, R> {

    public NestedContainerUpdateContext(
            SqaleUpdateContext<?, Q, R> parentContext, S object, R rootRow) {
        super(parentContext, object, rootRow);

        // TODO
    }

    @Override
    public Q path() {
        return null; // TODO
    }

    @Override
    public <P extends Path<T>, T> void set(P path, T value) {
        // TODO delegate to parent
    }
}
