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
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.Validate;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * @author lazyman
 */
public abstract class Matcher<T> {

    private static final Trace LOGGER = TraceManager.getTrace(Matcher.class);

    /**
     * Create hibernate {@link Criterion} based on matcher defined in filter.
     *
     *
     * @param hibernateQuery
     * @param operation
     * @param propertyPath
     * @param value
     * @param matcher      Now type of {@link String}, but will be updated to {@link javax.xml.namespace.QName}
     *                     type after query-api update
     * @return
     * @throws QueryException
     */
    public abstract Condition match(RootHibernateQuery hibernateQuery, ItemRestrictionOperation operation, String propertyPath, T value, String matcher)
            throws QueryException;

    protected Condition basicMatch(RootHibernateQuery hibernateQuery, ItemRestrictionOperation operation, String propertyPath, Object value,
                                   boolean ignoreCase) throws QueryException {
        Validate.notNull(hibernateQuery, "hibernateQuery");

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
                condition = hibernateQuery.createIsNotNull(propertyPath);
                break;
            case NULL:
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

    static QName getApproximateSupportedMatchingRule(QName originalMatchingRule, List<QName> supportedMatchingRules,
			Map<QName, QName> matchingRulesConvergenceMap) {
        if (originalMatchingRule == null || supportedMatchingRules.contains(originalMatchingRule)) {
            return originalMatchingRule;
        }
        return matchingRulesConvergenceMap.entrySet().stream()
                .filter(entry -> QNameUtil.match(entry.getKey(), originalMatchingRule))
                .map(Map.Entry::getValue)
                .findFirst()
                // if nothing applicable we return the original one - query interpreter will take care of it
                // (logging error or throwing an exception) ... TODO rethink this
                .orElse(originalMatchingRule);
    }
}
