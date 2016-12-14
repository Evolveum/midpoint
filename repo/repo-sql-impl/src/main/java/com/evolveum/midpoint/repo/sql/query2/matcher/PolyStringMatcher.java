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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.AndCondition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query2.restriction.ItemRestrictionOperation;
import org.apache.commons.lang.StringUtils;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lazyman
 */
public class PolyStringMatcher extends Matcher<PolyString> {

    //todo will be changed to QNames later (after query api update)
    public static final String STRICT = PolyStringStrictMatchingRule.NAME.getLocalPart();
    public static final String ORIG = PolyStringOrigMatchingRule.NAME.getLocalPart();
    public static final String NORM = PolyStringNormMatchingRule.NAME.getLocalPart();
    public static final String DEFAULT = DefaultMatchingRule.NAME.getLocalPart();

    public static final String STRICT_IGNORE_CASE = "strictIgnoreCase";
    public static final String ORIG_IGNORE_CASE = "origIgnoreCase";
    public static final String NORM_IGNORE_CASE = "normIgnoreCase";

	private static final List<QName> SUPPORTED_MATCHING_RULES = Arrays
			.asList(DefaultMatchingRule.NAME,
					PolyStringStrictMatchingRule.NAME,
					PolyStringOrigMatchingRule.NAME,
					PolyStringNormMatchingRule.NAME,
					new QName(STRICT_IGNORE_CASE),
					new QName(ORIG_IGNORE_CASE),
					new QName(NORM_IGNORE_CASE));
	private static final Map<QName, QName> MATCHING_RULES_CONVERGENCE_MAP = new HashMap<>();
	static {
		// Nothing here - the below (String-specific) matching rules should NOT be used for polystrings
		// TODO think again ... currently the approximate matching rule is the same as original one
//		MATCHING_RULES_CONVERGENCE_MAP.put(DistinguishedNameMatchingRule.NAME, PolyStringNormMatchingRule.NAME);	//ok?
//		MATCHING_RULES_CONVERGENCE_MAP.put(ExchangeEmailAddressesMatchingRule.NAME, PolyStringNormMatchingRule.NAME); //ok?
//		MATCHING_RULES_CONVERGENCE_MAP.put(UuidMatchingRule.NAME, PolyStringNormMatchingRule.NAME);
//		MATCHING_RULES_CONVERGENCE_MAP.put(XmlMatchingRule.NAME, DefaultMatchingRule.NAME);		//ok?
	}

	@Override
    public Condition match(RootHibernateQuery hibernateQuery, ItemRestrictionOperation operation, String propertyName, PolyString value, String matcher)
            throws QueryException {

        boolean ignoreCase = STRICT_IGNORE_CASE.equals(matcher)
                || ORIG_IGNORE_CASE.equals(matcher)
                || NORM_IGNORE_CASE.equals(matcher);

        if (StringUtils.isEmpty(matcher) || DEFAULT.equals(matcher)
                || STRICT.equals(matcher) || STRICT_IGNORE_CASE.equals(matcher)) {
            AndCondition conjunction = hibernateQuery.createAnd();
            conjunction.add(createOrigMatch(hibernateQuery, operation, propertyName, value, ignoreCase));
            conjunction.add(createNormMatch(hibernateQuery, operation, propertyName, value, ignoreCase));
            return conjunction;
        } else if (ORIG.equals(matcher) || ORIG_IGNORE_CASE.equals(matcher)) {
            return createOrigMatch(hibernateQuery, operation, propertyName, value, ignoreCase);
        } else if (NORM.equals(matcher) || NORM_IGNORE_CASE.equals(matcher)) {
            return createNormMatch(hibernateQuery, operation, propertyName, value, ignoreCase);
        } else {
            throw new QueryException("Unknown matcher '" + matcher + "'.");
        }
    }

    private Condition createNormMatch(RootHibernateQuery hibernateQuery, ItemRestrictionOperation operation, String propertyName, PolyString value,
                                      boolean ignoreCase) throws QueryException {

        String realValue = value != null ? value.getNorm() : null;
        return basicMatch(hibernateQuery, operation, propertyName + '.' + RPolyString.F_NORM, realValue, ignoreCase);
    }

    private Condition createOrigMatch(RootHibernateQuery hibernateQuery, ItemRestrictionOperation operation, String propertyName, PolyString value,
                                      boolean ignoreCase) throws QueryException {

        String realValue = value != null ? value.getOrig() : null;
        return basicMatch(hibernateQuery, operation, propertyName + '.' + RPolyString.F_ORIG, realValue, ignoreCase);
    }

	public static QName getApproximateSupportedMatchingRule(QName originalMatchingRule) {
		return Matcher.getApproximateSupportedMatchingRule(originalMatchingRule, SUPPORTED_MATCHING_RULES, MATCHING_RULES_CONVERGENCE_MAP);
	}
}
