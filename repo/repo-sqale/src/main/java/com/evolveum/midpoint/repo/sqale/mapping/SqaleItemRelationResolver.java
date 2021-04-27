/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Extension of {@link ItemRelationResolver}, this is a common contract for resolver
 * that helps with navigating over complex (non-single) item paths for both query
 * and application of delta modification.
 */
public interface SqaleItemRelationResolver<Q extends FlexibleRelationalPathBase<R>, R>
        extends ItemRelationResolver<Q, R> {

    /**
     * Resolves current query context to a new context (mapping is always part of context).
     * The information about the resolved item is captured in the instance resolver already
     * in a manner that is specific for various types of resolution (JOIN or nested mapping).
     */
    SqaleUpdateContext<?, ?, ?> resolve(SqaleUpdateContext<?, Q, R> context);
}
