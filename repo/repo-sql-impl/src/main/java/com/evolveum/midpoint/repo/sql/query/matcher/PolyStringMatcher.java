/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query.matcher;

import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.midpoint.prism.match.PolyStringStrictMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.restriction.ItemRestrictionOperation;
import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * @author lazyman
 */
public class PolyStringMatcher extends Matcher<PolyString> {

    //todo will be changed to QNames later (after query api update)
    public static final String STRICT = PolyStringStrictMatchingRule.NAME.getLocalPart();
    public static final String ORIG = PolyStringOrigMatchingRule.NAME.getLocalPart();
    public static final String NORM = PolyStringNormMatchingRule.NAME.getLocalPart();

    public static final String STRICT_IGNORE_CASE = "strictIgnoreCase";
    public static final String ORIG_IGNORE_CASE = "origIgnoreCase";
    public static final String NORM_IGNORE_CASE = "normIgnoreCase";

    @Override
    public Criterion match(ItemRestrictionOperation operation, String propertyName, PolyString value, String matcher)
            throws QueryException {

        boolean ignoreCase = STRICT_IGNORE_CASE.equals(matcher)
                || ORIG_IGNORE_CASE.equals(matcher)
                || NORM_IGNORE_CASE.equals(matcher);

        if (StringUtils.isEmpty(matcher)
                || STRICT.equals(matcher) || STRICT_IGNORE_CASE.equals(matcher)) {
            Conjunction conjunction = Restrictions.conjunction();
            conjunction.add(createOrigMatch(operation, propertyName, value, ignoreCase));
            conjunction.add(createNormMatch(operation, propertyName, value, ignoreCase));

            return conjunction;
        } else if (ORIG.equals(matcher) || ORIG_IGNORE_CASE.equals(matcher)) {
            return createOrigMatch(operation, propertyName, value, ignoreCase);
        } else if (NORM.equals(matcher) || NORM_IGNORE_CASE.equals(matcher)) {
            return createNormMatch(operation, propertyName, value, ignoreCase);
        } else {
            throw new QueryException("Unknown matcher '" + matcher + "'.");
        }
    }

    private Criterion createNormMatch(ItemRestrictionOperation operation, String propertyName, PolyString value,
                                      boolean ignoreCase) throws QueryException {

        String realValue = value != null ? value.getNorm() : null;
        return basicMatch(operation, propertyName + '.' + RPolyString.F_NORM, realValue, ignoreCase);
    }

    private Criterion createOrigMatch(ItemRestrictionOperation operation, String propertyName, PolyString value,
                                      boolean ignoreCase) throws QueryException {

        String realValue = value != null ? value.getOrig() : null;
        return basicMatch(operation, propertyName + '.' + RPolyString.F_ORIG, realValue, ignoreCase);
    }
}
