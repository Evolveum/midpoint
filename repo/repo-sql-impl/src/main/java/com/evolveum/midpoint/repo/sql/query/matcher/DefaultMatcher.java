/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
