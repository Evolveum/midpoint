/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.function.Function;

import com.querydsl.core.types.dsl.EnumPath;

import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Delta processor for an attribute path (Prism item) of enum type that is mapped to matching
 * PostgreSQL enum type - this allows to use schema enums directly.
 *
 * @param <E> used enum type
 */
public class EnumItemDeltaProcessor<E extends Enum<E>>
        extends SinglePathItemDeltaProcessor<E, EnumPath<E>> {

    /**
     * @param <Q> entity query type from which the attribute is resolved
     * @param <R> row type related to {@link Q}
     */
    public <Q extends FlexibleRelationalPathBase<R>, R> EnumItemDeltaProcessor(
            SqaleUpdateContext<?, Q, R> context,
            Function<Q, EnumPath<E>> rootToQueryItem) {
        super(context, rootToQueryItem);
    }
}
