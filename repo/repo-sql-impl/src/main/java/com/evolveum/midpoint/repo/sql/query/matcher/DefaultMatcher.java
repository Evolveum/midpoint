/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.matcher;

import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query.restriction.ItemRestrictionOperation;

/**
 * @author lazyman
 */
public class DefaultMatcher<T> extends Matcher<T> {

    @Override
    public Condition match(
            HibernateQuery hibernateQuery, ItemRestrictionOperation operation,
            String propertyName, boolean extension,
            T value, String matchingRule)
            throws QueryException {

        return basicMatch(hibernateQuery, operation, toActualHqlName(propertyName, extension), value, false);
    }
}
