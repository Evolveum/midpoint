/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query2;

import com.evolveum.midpoint.repo.sql.query.RQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import org.apache.commons.lang.Validate;
import org.hibernate.HibernateException;
import org.hibernate.query.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

import java.util.List;

/**
 * @author lazyman
 */
public class RQueryImpl implements RQuery {

    private RootHibernateQuery querySource;        // only for diagnostic purposes
    private org.hibernate.query.Query query;

    public RQueryImpl(Query query, RootHibernateQuery querySource) {
        Validate.notNull(query, "Query must not be null.");
        this.query = query;
        this.querySource = querySource;
    }

    @Override
    public List list() throws HibernateException {
        return query.list();
    }

    @Override
    public Object uniqueResult() throws HibernateException {
        return query.uniqueResult();
    }

    @Override
    public ScrollableResults scroll(ScrollMode mode) throws HibernateException {
        return query.scroll(mode);
    }

    public org.hibernate.query.Query getQuery() {
        return query;
    }

    public RootHibernateQuery getQuerySource() {
        return querySource;
    }
}
