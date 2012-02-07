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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query;

import com.evolveum.midpoint.repo.sql.data.common.RUserType;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.w3c.dom.Element;

/**
 * @author lazyman
 */
public class QueryProcessor {

    public Criteria createFilterCriteria(Criteria criteria1, Element filter) {
        Session session = null;
        
        Criteria criteria = session.createCriteria(RUserType.class);
        criteria.add(Restrictions.eq("givenName", "LDAP"));

        
        //todo create criteria from query filter

//        JAXBUtil.unmarshal(filter);

        return criteria;
    }
}
