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

package com.evolveum.midpoint.repo.sql.query2.hqm.condition;

import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import org.apache.commons.lang.Validate;

/**
 * @author mederly
 */
public abstract class PropertyCondition extends Condition {

    protected String propertyPath;

    public PropertyCondition(RootHibernateQuery rootHibernateQuery, String propertyPath) {
        super(rootHibernateQuery);
        Validate.notNull(propertyPath, "propertyPath");
        this.propertyPath = propertyPath;
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    protected String createParameterName(String propertyPath) {
        Validate.notEmpty(propertyPath, "propertyPath");
        int i = propertyPath.lastIndexOf('.');
        if (i < 0) {
            return propertyPath;
        } else {
            String name = propertyPath.substring(i+1);
            if (!name.isEmpty()) {
                return name;
            } else {
                return "param";     // shouldn't occur
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertyCondition that = (PropertyCondition) o;

        return propertyPath.equals(that.propertyPath);

    }

    @Override
    public int hashCode() {
        return propertyPath.hashCode();
    }
}
