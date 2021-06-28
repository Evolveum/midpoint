/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.function.Function;

import com.querydsl.core.types.dsl.StringPath;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

public class PolyStringItemDeltaProcessor extends ItemDeltaSingleValueProcessor<PolyString> {

    private final StringPath origPath;
    private final StringPath normPath;

    /**
     * @param <Q> entity query type from which the attribute is resolved
     * @param <R> row type related to {@link Q}
     */
    public <Q extends FlexibleRelationalPathBase<R>, R> PolyStringItemDeltaProcessor(
            SqaleUpdateContext<?, Q, R> context,
            Function<Q, StringPath> origMapping,
            Function<Q, StringPath> normMapping) {
        super(context);
        this.origPath = origMapping.apply(context.entityPath());
        this.normPath = normMapping.apply(context.entityPath());
    }

    @Override
    public void setValue(PolyString value) {
        context.set(origPath, value.getOrig());
        context.set(normPath, value.getNorm());
    }

    @Override
    public void delete() {
        context.setNull(origPath);
        context.setNull(normPath);
    }
}
