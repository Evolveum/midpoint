/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query;

import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.repo.sql.util.RUtil;

import jakarta.persistence.Query;
import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;

/**
 * @author lazyman
 */
public class RQueryImpl implements RQuery {

    private final HibernateQuery querySource; // only for diagnostic purposes
    private final Query query;

    public RQueryImpl(Query query, HibernateQuery querySource) {
        Objects.requireNonNull(query, "Query must not be null.");
        this.query = query;
        this.querySource = querySource;
    }

    @Override
    public <T> List<T> list() throws HibernateException {
        //noinspection unchecked
        return (List<T>) query.getResultList();
    }

    @Override
    public <T> T uniqueResult() throws HibernateException {
        //noinspection unchecked
        return (T) RUtil.getSingleResultOrNull(query);
    }

    @Override
    public ScrollableResults<?> scroll(ScrollMode mode) throws HibernateException {
        return query.unwrap(org.hibernate.query.Query.class).scroll(mode);
    }

    public Query getQuery() {
        return query;
    }

    public HibernateQuery getQuerySource() {
        return querySource;
    }
}
