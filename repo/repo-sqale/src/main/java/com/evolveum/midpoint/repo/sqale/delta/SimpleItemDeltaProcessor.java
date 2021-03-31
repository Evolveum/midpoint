/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta;

import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sqale.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;

public class SimpleItemDeltaProcessor<P extends Path<?>> extends ItemDeltaProcessor {

    protected final P path;

    public SimpleItemDeltaProcessor(
            SqaleUpdateContext<?, ?, ?> context, Function<EntityPath<?>, P> rootToQueryItem) {
        super(context);
        this.path = rootToQueryItem.apply(context.path());
    }

    @Override
    public void process(ItemDelta<?, ?> modification) throws RepositoryException {
        System.out.println(path + " <= applying modification: " + modification); // TODO out
        if (modification.isDelete()) {
//            context.update(path, NULL);
        }
//        context.update(path, modification.getv)
    }
}
