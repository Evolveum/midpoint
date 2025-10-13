/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import java.util.Objects;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;

public abstract class PropertyCondition extends Condition {

    protected String propertyPath;

    public PropertyCondition(HibernateQuery rootHibernateQuery, String propertyPath) {
        super(rootHibernateQuery);
        Objects.requireNonNull(propertyPath, "propertyPath");
        this.propertyPath = propertyPath;
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    protected String createParameterName(String propertyPath) {
        Objects.requireNonNull(propertyPath, "propertyPath");
        int i = propertyPath.lastIndexOf('.');
        if (i < 0) {
            return propertyPath;
        } else {
            String name = propertyPath.substring(i + 1);
            if (!name.isEmpty()) {
                return name;
            } else {
                return "param";     // shouldn't occur
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        PropertyCondition that = (PropertyCondition) o;

        return propertyPath.equals(that.propertyPath);

    }

    @Override
    public int hashCode() {
        return propertyPath.hashCode();
    }
}
