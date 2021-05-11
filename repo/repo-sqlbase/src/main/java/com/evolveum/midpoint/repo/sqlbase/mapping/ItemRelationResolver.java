/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping;

import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Common contract for resolver that helps with navigating over complex (non-single) item paths
 * for query purposes.
 *
 * @param <Q> query type with the mapping
 * @param <R> row type related to {@link Q}
 */
public interface ItemRelationResolver<Q extends FlexibleRelationalPathBase<R>, R> {

    /**
     * Resolves a query context to {@link ResolutionResult} with new context and mapping.
     * The information about the resolved item is captured in the instance resolver already
     * in a manner that is specific for various types of resolution (JOIN or nested mapping).
     */
    ResolutionResult resolve(SqlQueryContext<?, Q, R> context);

    class ResolutionResult {
        public final SqlQueryContext<?, ?, ?> context;
        public final QueryModelMapping<?, ?, ?> mapping;

        public ResolutionResult(
                SqlQueryContext<?, ?, ?> context, QueryModelMapping<?, ?, ?> mapping) {
            this.context = context;
            this.mapping = mapping;
        }
    }
}
