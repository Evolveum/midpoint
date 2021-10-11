/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query2.matcher;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query2.restriction.ItemRestrictionOperation;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang3.StringUtils;

/**
 * @author lazyman
 */
public class StringMatcher extends Matcher<String> {

    private static final Trace LOGGER = TraceManager.getTrace(StringMatcher.class);

    //todo will be changed to QName later (after query api update)
    public static final String IGNORE_CASE = PrismConstants.STRING_IGNORE_CASE_MATCHING_RULE_NAME.getLocalPart();
    public static final String DEFAULT = PrismConstants.DEFAULT_MATCHING_RULE_NAME.getLocalPart();

    @Override
    public Condition match(RootHibernateQuery hibernateQuery, ItemRestrictionOperation operation, String propertyName, String value, String matcher)
            throws QueryException {

        boolean ignoreCase;
        if (StringUtils.isEmpty(matcher) || DEFAULT.equals(matcher)) {
            ignoreCase = false;
        } else if (IGNORE_CASE.equalsIgnoreCase(matcher)) {
            ignoreCase = true;
        } else {
            // TODO temporary code (switch to exception in 3.6)
            ignoreCase = false;
            LOGGER.error("Unknown matcher '{}'. The only supported explicit matcher for string values is '{}'. Ignoring for now, "
                    + "but may cause an exception in future midPoint versions. Property name: '{}', value: '{}'",
                    matcher, IGNORE_CASE, propertyName, value);
            //throw new QueryException("Unknown matcher '" + matcher + "'. The only supported explicit matcher for string values is '" + IGNORE_CASE + "'.");
        }

        return basicMatch(hibernateQuery, operation, propertyName, value, ignoreCase);
    }
}
