/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;

import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;

/**
 * This is a universal/generic filter processor that dispatches to the actual filter processor
 * based on the filter type.
 * It is used both as an entry point for the root filter of the query, but also when various
 * structural filters need to resolve their components (e.g. AND uses this for all its components).
 *
 * While the subtypes of {@link ObjectFilter} (from Prism API) are all known here, the concrete
 * types and implementations in the repository are not all known on the `repo-sqlbase` level.
 * That's why some processors are created directly and for others the factory methods
 * on the {@link SqlQueryContext} are used and concrete repo (like `repo-sqale` implements them).
 */
public class ObjectFilterProcessor implements FilterProcessor<ObjectFilter> {

    private final SqlQueryContext<?, ?, ?> context;

    public ObjectFilterProcessor(SqlQueryContext<?, ?, ?> context) {
        this.context = context;
    }

    @Override
    public Predicate process(ObjectFilter filter) throws RepositoryException {
        // To compare with old repo see: QueryInterpreter.findAndCreateRestrictionInternal
        if (filter instanceof NaryLogicalFilter) {
            return new NaryLogicalFilterProcessor(context)
                    .process((NaryLogicalFilter) filter);
        } else if (filter instanceof NotFilter) {
            return new NotFilterProcessor(context)
                    .process((NotFilter) filter);
        } else if (filter instanceof ValueFilter) {
            // here are the values applied (ref/property value filters)
            return new ValueFilterProcessor(context)
                    .process((ValueFilter<?, ?>) filter);
        } else if (filter instanceof InOidFilter) {
            return context.createInOidFilter()
                    .process((InOidFilter) filter);
        } else if (filter instanceof OrgFilter) {
            return context.createOrgFilter()
                    .process((OrgFilter) filter);
        } else if (filter instanceof FullTextFilter) {
            // TODO
            throw new QueryException("TODO filter " + filter);
        } else if (filter instanceof TypeFilter) {
            // TODO
            throw new QueryException("TODO filter " + filter);
        } else if (filter instanceof AllFilter) {
            // TODO throws in old repo, do we want to throw here too? (the same for NoneFilter and UndefinedFilter)
            return Expressions.asBoolean(true).isTrue();
        } else if (filter instanceof NoneFilter) {
            return Expressions.asBoolean(true).isFalse();
        } else {
            throw new QueryException("Unsupported filter " + filter);
        }
    }
}
