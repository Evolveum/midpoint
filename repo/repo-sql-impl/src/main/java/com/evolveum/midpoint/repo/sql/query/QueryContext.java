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

import com.evolveum.midpoint.prism.PropertyPath;
import org.apache.commons.lang.Validate;
import org.hibernate.Criteria;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lazyman
 */
public class QueryContext {

    private Map<PropertyPath, Criteria> criterions = new HashMap<PropertyPath, Criteria>();
    private Map<PropertyPath, String> aliases = new HashMap<PropertyPath, String>();

    public Criteria getCriteria(PropertyPath path) {
        return criterions.get(path);
    }

    public void setCriteria(PropertyPath path, Criteria criteria) {
        Validate.notNull(criteria, "Criteria must not be null.");
        if (criterions.containsKey(path)) {
            throw new IllegalArgumentException("Already has criteria with this path '" + path + "'");
        }
        
        criterions.put(path, criteria);
    }
}
