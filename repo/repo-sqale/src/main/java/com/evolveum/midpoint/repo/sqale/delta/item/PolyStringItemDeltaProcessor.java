/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.dsl.StringPath;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;

public class PolyStringItemDeltaProcessor extends ItemDeltaSingleValueProcessor<PolyString> {

    private final StringPath origPath;
    private final StringPath normPath;

    public PolyStringItemDeltaProcessor(
            SqaleUpdateContext<?, ?, ?> context,
            Function<EntityPath<?>, StringPath> origMapping,
            Function<EntityPath<?>, StringPath> normMapping) {
        super(context);
        this.origPath = origMapping.apply(context.path());
        this.normPath = normMapping.apply(context.path());
    }

    @Override
    public void process(ItemDelta<?, ?> modification) throws RepositoryException {
        // See implementation comments in SinglePathItemDeltaProcessor#process for logic details.
        PolyString polyString = getAnyValue(modification);
        if (modification.isDelete() || polyString == null) {
            delete();
        } else {
            setValue(polyString);
        }
    }

    @Override
    public void setValue(PolyString value) {
        context.set(origPath, value.getOrig());
        context.set(normPath, value.getNorm());
    }

    @Override
    public void delete() {
        context.set(origPath, null);
        context.set(normPath, null);
    }
}
