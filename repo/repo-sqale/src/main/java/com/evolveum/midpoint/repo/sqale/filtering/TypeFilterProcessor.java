/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.QuerydslUtils;

/**
 * Filter processor that resolves {@link TypeFilter}.
 *
 * @param <Q> query type of the original context
 * @param <R> row type related to {@link Q}
 * @param <TQ> target query type for the type filter
 * @param <TR> row type related to {@link TQ}
 */
public class TypeFilterProcessor<Q extends QObject<R>, R extends MObject, TQ extends QObject<TR>, TR extends MObject>
        implements FilterProcessor<TypeFilter> {

    private final SqaleQueryContext<?, Q, R> context;

    public TypeFilterProcessor(SqaleQueryContext<?, ?, ?> context) throws QueryException {
        FlexibleRelationalPathBase<?> path = context.root();
        if (!(path instanceof QObject)) {
            throw new QueryException("Type filter can only be used for objects,"
                    + " not for path " + path + " of type " + path.getClass());
        }

        //noinspection unchecked
        this.context = (SqaleQueryContext<?, Q, R>) context;
    }

    @Override
    public Predicate process(TypeFilter filter) throws RepositoryException {
        Q path = context.path();
        //noinspection unchecked
        Class<TQ> filterQueryType = (Class<TQ>)
                MObjectType.fromTypeQName(filter.getType()).getQueryType();

        ObjectFilter innerFilter = filter.getFilter();

        if (path.getClass().equals(filterQueryType)) {
            // Unexpected, but let's take the shortcut.
            return innerFilter != null
                    ? context.process(innerFilter)
                    : QuerydslUtils.EXPRESSION_TRUE;
            // We can't just return null, we need some condition meaning "everything" for cases
            // when the filter is in OR. It can't be just no-op in such cases.
        } else {
            SqaleQueryContext<?, TQ, TR> filterContext =
                    (SqaleQueryContext<?, TQ, TR>) context.subquery(filterQueryType);
            filterContext.sqlQuery().where(filterContext.path().oid.eq(path.oid));

            // Here we call processFilter that actually adds the WHERE conditions to the subquery.
            filterContext.processFilter(innerFilter);
            return filterContext.sqlQuery().exists(); // and this fulfills the processor contract
        }
    }
}
