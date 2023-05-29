/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.matcher;

import java.util.Objects;

import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query.restriction.ItemRestrictionOperation;
import com.evolveum.midpoint.repo.sql.query.restriction.MatchMode;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author lazyman
 */
public abstract class Matcher<T> {

    private static final Trace LOGGER = TraceManager.getTrace(Matcher.class);

    public abstract Condition match(
            HibernateQuery hibernateQuery, ItemRestrictionOperation operation,
            String propertyPath, T value, String matcher)
            throws QueryException;

    protected Condition basicMatch(
            HibernateQuery hibernateQuery, ItemRestrictionOperation operation,
            String propertyPath, Object value, boolean ignoreCase) throws QueryException {
        Objects.requireNonNull(hibernateQuery, "hibernateQuery");

        if (ignoreCase && !(value instanceof String)) {
            LOGGER.warn("Ignoring ignoreCase setting for non-string value of {}", value);
            ignoreCase = false;
        }

        Condition condition;
        switch (operation) {
            case EQ:
                if (value == null) {
                    condition = hibernateQuery.createIsNull(propertyPath);
                } else {
                    condition = hibernateQuery.createEq(propertyPath, value, ignoreCase);
                }
                break;
            case GT:
            case GE:
            case LT:
            case LE:
                condition = hibernateQuery.createSimpleComparisonCondition(propertyPath, value, operation.symbol(), ignoreCase);
                break;
            case NOT_NULL:
                // TODO never used so it seems (never assigned to operation)
                condition = hibernateQuery.createIsNotNull(propertyPath);
                break;
            case NULL:
                // TODO never used so it seems (never assigned to operation)
                condition = hibernateQuery.createIsNull(propertyPath);
                break;
            case STARTS_WITH:
                condition = hibernateQuery.createLike(propertyPath, (String) value, MatchMode.START, ignoreCase);
                break;
            case ENDS_WITH:
                condition = hibernateQuery.createLike(propertyPath, (String) value, MatchMode.END, ignoreCase);
                break;
            case SUBSTRING:
                condition = hibernateQuery.createLike(propertyPath, (String) value, MatchMode.ANYWHERE, ignoreCase);
                break;
            default:
                throw new QueryException("Unknown operation '" + operation + "'.");
        }

        return condition;
    }
}
