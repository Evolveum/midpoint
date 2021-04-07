/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta;

import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.dsl.DateTimePath;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sqale.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.QuerydslUtils;

public class TimestampItemDeltaProcessor<T extends Comparable<T>>
        extends SinglePathItemDeltaProcessor<T, DateTimePath<T>> {

    public TimestampItemDeltaProcessor(SqaleUpdateContext<?, ?, ?> context,
            Function<EntityPath<?>, DateTimePath<T>> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    protected @Nullable T transformRealValue(Object realValue) {
        return QuerydslUtils.convertTimestampToPathType(realValue, path);
    }
}
