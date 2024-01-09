/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.query.matcher;

import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;

import com.google.common.base.Strings;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.AndCondition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query.restriction.ItemRestrictionOperation;
import com.evolveum.midpoint.repo.sqlbase.QueryException;

public class PolyStringMatcher extends Matcher<PolyString> {

    //todo will be changed to QNames later (after query api update)
    public static final String STRICT = PrismConstants.POLY_STRING_STRICT_MATCHING_RULE_NAME.getLocalPart();
    public static final String ORIG = PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME.getLocalPart();
    public static final String NORM = PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME.getLocalPart();
    public static final String DEFAULT = PrismConstants.DEFAULT_MATCHING_RULE_NAME.getLocalPart();

    public static final String STRICT_IGNORE_CASE = "strictIgnoreCase";
    public static final String ORIG_IGNORE_CASE = "origIgnoreCase";
    public static final String NORM_IGNORE_CASE = "normIgnoreCase";

    @Override
    public Condition match(
            HibernateQuery hibernateQuery, ItemRestrictionOperation operation, String propertyName, boolean extension,
            PolyString value, String matchingRule)
            throws QueryException {

        boolean ignoreCase = STRICT_IGNORE_CASE.equals(matchingRule)
                || ORIG_IGNORE_CASE.equals(matchingRule)
                || NORM_IGNORE_CASE.equals(matchingRule);

        if (Strings.isNullOrEmpty(matchingRule) || DEFAULT.equals(matchingRule)
                || STRICT.equals(matchingRule) || STRICT_IGNORE_CASE.equals(matchingRule)) {
            AndCondition conjunction = hibernateQuery.createAnd();
            conjunction.add(createOrigMatch(hibernateQuery, operation, propertyName, extension, value, ignoreCase));
            conjunction.add(createNormMatch(hibernateQuery, operation, propertyName, value, ignoreCase));
            return conjunction;
        } else if (ORIG.equals(matchingRule) || ORIG_IGNORE_CASE.equals(matchingRule)) {
            return createOrigMatch(hibernateQuery, operation, propertyName, extension, value, ignoreCase);
        } else if (NORM.equals(matchingRule) || NORM_IGNORE_CASE.equals(matchingRule)) {
            return createNormMatch(hibernateQuery, operation, propertyName, value, ignoreCase);
        } else {
            throw new QueryException("Unknown matcher '" + matchingRule + "'.");
        }
    }

    private Condition createNormMatch(
            HibernateQuery hibernateQuery, ItemRestrictionOperation operation, String propertyName, PolyString value,
            boolean ignoreCase) throws QueryException {

        String realValue = PolyString.getNorm(value);
        return basicMatch(hibernateQuery, operation, propertyName + '.' + RPolyString.F_NORM, realValue, ignoreCase);
    }

    private Condition createOrigMatch(
            HibernateQuery hibernateQuery, ItemRestrictionOperation operation, String propertyName, boolean extension, PolyString value,
            boolean ignoreCase) throws QueryException {

        String realValue = PolyString.getOrig(value);
        return basicMatch(
                hibernateQuery, operation,
                propertyName + '.' + (extension ? RAnyValue.F_VALUE : RPolyString.F_ORIG),
                realValue, ignoreCase);
    }
}
