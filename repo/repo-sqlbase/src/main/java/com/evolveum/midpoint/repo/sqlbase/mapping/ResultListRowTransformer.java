/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping;

import java.util.Collection;

import com.querydsl.core.Tuple;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;

/**
 * Helps with row transformation of result lists, possibly in stateful context.
 * This kind of transformation still has access to low-level row data if necessary.
 * Typical example is container's owner OID which is lost in the container itself.
 *
 * @param <S> schema type
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public interface ResultListRowTransformer<S, Q extends FlexibleRelationalPathBase<R>, R> {

    S transform(Tuple rowTuple, Q entityPath,
            Collection<SelectorOptions<GetOperationOptions>> options);
}
