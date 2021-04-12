/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping.delta;

import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.dsl.EnumPath;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sqale.SqaleUpdateContext;

/**
 * Delta processor for an attribute path (Prism item) of enum type that is mapped to matching
 * PostgreSQL enum type - this allows to use schema enums directly.
 */
public class EnumItemDeltaProcessor<E extends Enum<E>>
        extends SinglePathItemDeltaProcessor<E, EnumPath<E>> {

    public EnumItemDeltaProcessor(SqaleUpdateContext<?, ?, ?> context,
            Function<EntityPath<?>, EnumPath<E>> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    protected @Nullable E transformRealValue(Object realValue) {
        //noinspection unchecked
        return (E) realValue;
    }
}
