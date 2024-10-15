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
 * @param <TQ> type of target entity path
 * @param <TR> row type related to the target entity path {@link TQ}
 */
public interface ItemRelationResolver<
        Q extends FlexibleRelationalPathBase<R>, R,
        TQ extends FlexibleRelationalPathBase<TR>, TR> {

    /**
     * Resolves a query context to {@link ResolutionResult} with new context and mapping.
     * The information about the resolved item is captured in the instance resolver already
     * in a manner that is specific for various types of resolution (subquery or nested mapping).
     */
    default ResolutionResult<TQ, TR> resolve(SqlQueryContext<?, Q, R> context) {
        return resolve(context, false);
    }

    ResolutionResult<TQ, TR> resolve(SqlQueryContext<?, Q, R> context, boolean parent);

    default ResolutionResult<TQ, TR> resolveUsingJoin(SqlQueryContext<?, Q, R> context) {
        return resolve(context);
    }

    class ResolutionResult<TQ extends FlexibleRelationalPathBase<TR>, TR> {
        public final SqlQueryContext<?, TQ, TR> context;
        public final QueryModelMapping<?, TQ, TR> mapping;
        public final boolean subquery;

        public ResolutionResult(
                SqlQueryContext<?, TQ, TR> context, QueryModelMapping<?, TQ, TR> mapping) {
            this.context = context;
            this.mapping = mapping;
            subquery = false;
        }

        public ResolutionResult(SqlQueryContext<?, TQ, TR> context,
                QueryModelMapping<?, TQ, TR> mapping, boolean subquery) {
            this.context = context;
            this.mapping = mapping;
            this.subquery = subquery;
        }
    }
}
