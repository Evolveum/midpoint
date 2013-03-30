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

import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.restriction.ItemRestrictionOperation;
import org.hibernate.criterion.Criterion;

/**
 * @author lazyman
 */
public class StringMatcher extends Matcher<String> {

    public static final String CASE_SENSITIVE = "caseSensitive";
    public static final String IGNORE_CASE = "ignoreCase";

    @Override
    public Criterion match(ItemRestrictionOperation operation, String propertyName, String value, String matcher)
            throws QueryException {

        boolean ignoreCase = IGNORE_CASE.equalsIgnoreCase(matcher);

        return basicMatch(operation, propertyName, value, ignoreCase);
    }
}
