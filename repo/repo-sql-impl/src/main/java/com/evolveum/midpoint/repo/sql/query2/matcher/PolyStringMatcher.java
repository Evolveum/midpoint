/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query2.matcher;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.restriction.ItemRestrictionOperation;
import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * @author lazyman
 */
public class PolyStringMatcher implements Matcher<PolyString> {

    public static final String STRICT = "strict";
    public static final String ORIG = "orig";
    public static final String NORM = "norm";

    @Override
    public Criterion match(ItemRestrictionOperation operation, String propertyName, PolyString value, String matcher)
            throws QueryException {

        if (StringUtils.isEmpty(matcher) || STRICT.equals(matcher)) {
            Conjunction conjunction = Restrictions.conjunction();
            conjunction.add(createOrigCriterion(propertyName, value));
            conjunction.add(createNormCriterion(propertyName, value));

            return conjunction;
        } else if (ORIG.equals(matcher)) {
            return createOrigCriterion(propertyName, value);
        } else if (NORM.equals(matcher)) {
            return createNormCriterion(propertyName, value);
        } else {
            throw new QueryException("Unknown matcher '" + matcher + "'.");
        }
    }

    private Criterion createNormCriterion(String propertyName, PolyString value) {
        return null;
    }

    private Criterion createOrigCriterion(String propertyName, PolyString value) {
        return null;
    }
}
