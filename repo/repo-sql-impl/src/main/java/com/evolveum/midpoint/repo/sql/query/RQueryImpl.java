/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.query;

import java.util.List;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;

import com.evolveum.midpoint.repo.sql.query.hqm.RootHibernateQuery;

/**
 * @author lazyman
 */
public class RQueryImpl implements RQuery {

    private final RootHibernateQuery querySource; // only for diagnostic purposes
    private final Query<?> query;

    public RQueryImpl(Query<?> query, RootHibernateQuery querySource) {
        Objects.requireNonNull(query, "Query must not be null.");
        this.query = query;
        this.querySource = querySource;
    }

    @Override
    public <T> List<T> list() throws HibernateException {
        //noinspection unchecked
        return (List<T>) query.list();
    }

    @Override
    public <T> T uniqueResult() throws HibernateException {
        //noinspection unchecked
        return (T) query.uniqueResult();
    }

    @Override
    public ScrollableResults scroll(ScrollMode mode) throws HibernateException {
        return query.scroll(mode);
    }

    public Query<?> getQuery() {
        return query;
    }

    public RootHibernateQuery getQuerySource() {
        return querySource;
    }
}
