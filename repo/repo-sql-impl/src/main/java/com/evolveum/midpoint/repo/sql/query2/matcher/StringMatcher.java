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

import com.evolveum.midpoint.prism.match.*;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query2.restriction.ItemRestrictionOperation;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang3.StringUtils;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lazyman
 */
public class StringMatcher extends Matcher<String> {

	private static final Trace LOGGER = TraceManager.getTrace(StringMatcher.class);

    //todo will be changed to QName later (after query api update)
    public static final String IGNORE_CASE = StringIgnoreCaseMatchingRule.NAME.getLocalPart();
    public static final String DEFAULT = DefaultMatchingRule.NAME.getLocalPart();

    private static final List<QName> SUPPORTED_MATCHING_RULES = Arrays.asList(DefaultMatchingRule.NAME, StringIgnoreCaseMatchingRule.NAME);
	private static final Map<QName, QName> MATCHING_RULES_CONVERGENCE_MAP = new HashMap<>();
    static {
    	MATCHING_RULES_CONVERGENCE_MAP.put(DistinguishedNameMatchingRule.NAME, DefaultMatchingRule.NAME);	// temporary code (TODO change in 3.6)
		MATCHING_RULES_CONVERGENCE_MAP.put(UuidMatchingRule.NAME, DefaultMatchingRule.NAME);				// temporary code (TODO change in 3.6)
    	//MATCHING_RULES_CONVERGENCE_MAP.put(DistinguishedNameMatchingRule.NAME, StringIgnoreCaseMatchingRule.NAME);
		//MATCHING_RULES_CONVERGENCE_MAP.put(UuidMatchingRule.NAME, StringIgnoreCaseMatchingRule.NAME);
    	MATCHING_RULES_CONVERGENCE_MAP.put(ExchangeEmailAddressesMatchingRule.NAME, DefaultMatchingRule.NAME);	// prefix is case sensitive
    	MATCHING_RULES_CONVERGENCE_MAP.put(XmlMatchingRule.NAME, DefaultMatchingRule.NAME);
	}

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

	public static QName getApproximateSupportedMatchingRule(QName originalMatchingRule) {
		return Matcher.getApproximateSupportedMatchingRule(originalMatchingRule, SUPPORTED_MATCHING_RULES, MATCHING_RULES_CONVERGENCE_MAP);
	}
}
