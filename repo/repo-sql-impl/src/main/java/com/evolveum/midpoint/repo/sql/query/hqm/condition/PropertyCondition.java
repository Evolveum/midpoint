/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import com.evolveum.midpoint.repo.sql.query.hqm.RootHibernateQuery;
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
