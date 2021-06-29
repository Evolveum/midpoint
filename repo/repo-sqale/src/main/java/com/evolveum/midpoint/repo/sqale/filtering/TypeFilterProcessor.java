/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

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

        SqaleQueryContext<?, ? extends QObject<?>, ?> filterContext = context;
        if (!path.getClass().equals(filterQueryType)) {
            filterContext = (SqaleQueryContext<?, ? extends QObject<?>, ?>)
                    context.leftJoin(filterQueryType, (oldQ, newQ) -> oldQ.oid.eq(newQ.oid));
        }

        filterContext.processFilter(filter.getFilter());
        return null; // is ignored by WHERE in the parent context
    }
}
