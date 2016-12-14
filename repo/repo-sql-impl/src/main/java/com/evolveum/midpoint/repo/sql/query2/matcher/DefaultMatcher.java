/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query2.matcher;

import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query2.restriction.ItemRestrictionOperation;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class DefaultMatcher<T> extends Matcher<T> {

    @Override
    public Condition match(RootHibernateQuery hibernateQuery, ItemRestrictionOperation operation, String propertyName, T value, String matcher)
            throws QueryException {

        return basicMatch(hibernateQuery, operation, propertyName, value, false);
    }

    public static QName getApproximateSupportedMatchingRule(QName originalMatchingRule) {
        return originalMatchingRule;    // TODO ok?
    }
}
