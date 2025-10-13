/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqlbase.mapping;

import com.querydsl.sql.SQLQuery;

import com.evolveum.midpoint.prism.query.ObjectOrdering;

/**
 * Special comparing expression that does not conform to
 * {@link com.querydsl.core.types.dsl.ComparableExpressionBase}, but we need to order by it for some reason.
 *
 * This is used as a support for iterative reference search, where we act like we're ordering by the table expression.
 * Implementation of {@link #orderBy} than has to take care of the ordering and add necessary columns.
 */
public interface SqlOrderableExpression {

    void orderBy(SQLQuery<?> sqlQuery, ObjectOrdering ordering);
}
