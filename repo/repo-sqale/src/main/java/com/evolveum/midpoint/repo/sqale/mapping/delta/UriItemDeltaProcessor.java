/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping.delta;

import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.dsl.NumberPath;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sqale.SqaleUpdateContext;

public class UriItemDeltaProcessor
        extends SinglePathItemDeltaProcessor<Integer, NumberPath<Integer>> {

    public UriItemDeltaProcessor(SqaleUpdateContext<?, ?, ?> context,
            Function<EntityPath<?>, NumberPath<Integer>> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    protected @Nullable Integer transformRealValue(Object realValue) {
        return context.processCacheableUri((String) realValue);
    }
}
