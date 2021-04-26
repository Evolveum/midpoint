/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Extension of {@link ItemRelationResolver}, this is a common contract for resolver
 * that helps with navigating over complex (non-single) item paths for both query
 * and application of delta modification.
 */
public interface SqaleItemRelationResolver<Q extends FlexibleRelationalPathBase<R>, R>
        extends ItemRelationResolver<Q, R> {

    /**
     * Resolves current query context to {@link ResolutionResult} with new context and mapping.
     * The information about the resolved item is captured in the instance resolver already
     * in a manner that is specific for various types of resolution (JOIN or nested mapping).
     */
    UpdateResolutionResult resolve(SqaleUpdateContext<?, Q, R> context);

    class UpdateResolutionResult {
        public final SqaleUpdateContext<?, ?, ?> context;
        public final QueryModelMapping<?, ?, ?> mapping;

        public UpdateResolutionResult(
                SqaleUpdateContext<?, ?, ?> context, QueryModelMapping<?, ?, ?> mapping) {
            this.context = context;
            this.mapping = mapping;
        }
    }
}
