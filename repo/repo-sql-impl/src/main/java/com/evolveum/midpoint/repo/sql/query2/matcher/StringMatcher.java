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

import com.evolveum.midpoint.prism.match.StringIgnoreCaseMatchingRule;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query2.restriction.ItemRestrictionOperation;
import org.apache.commons.lang3.StringUtils;

/**
 * @author lazyman
 */
public class StringMatcher extends Matcher<String> {

    //todo will be changed to QName later (after query api update)
    public static final String IGNORE_CASE = StringIgnoreCaseMatchingRule.NAME.getLocalPart();

    @Override
    public Condition match(RootHibernateQuery hibernateQuery, ItemRestrictionOperation operation, String propertyName, String value, String matcher)
            throws QueryException {

        boolean ignoreCase;
        if (StringUtils.isEmpty(matcher)) {
        	ignoreCase = false;
		} else if (IGNORE_CASE.equalsIgnoreCase(matcher)) {
        	ignoreCase = true;
		} else {
			throw new QueryException("Unknown matcher '" + matcher + "'. The only supported explicit matcher for string values is '" + IGNORE_CASE + "'.");
		}

        return basicMatch(hibernateQuery, operation, propertyName, value, ignoreCase);
    }
}
