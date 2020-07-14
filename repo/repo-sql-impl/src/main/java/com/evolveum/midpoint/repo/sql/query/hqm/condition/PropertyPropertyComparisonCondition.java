/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import java.util.Objects;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query.hqm.RootHibernateQuery;

public class PropertyPropertyComparisonCondition extends PropertyCondition {

    private final String rightSidePath;
    private final String operator;
    private final boolean ignoreCase;

    public PropertyPropertyComparisonCondition(RootHibernateQuery rootHibernateQuery, String propertyPath, String rightSidePath, String operator, boolean ignoreCase) {
        super(rootHibernateQuery, propertyPath);
        Objects.requireNonNull(rightSidePath, "rightSidePath");
        Objects.requireNonNull(operator, "operator");

        this.rightSidePath = rightSidePath;
        this.operator = operator;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public void dumpToHql(StringBuilder sb, int indent) {
        HibernateQuery.indent(sb, indent);

        String finalPropertyPath;
        String finalRightSidePropertyPath;
        if (ignoreCase) {
            finalPropertyPath = "lower(" + propertyPath + ")";
            finalRightSidePropertyPath = "lower(" + rightSidePath + ")";
        } else {
            finalPropertyPath = propertyPath;
            finalRightSidePropertyPath = rightSidePath;
        }

        sb.append(finalPropertyPath).append(" ").append(operator).append(" ").append(finalRightSidePropertyPath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }

        PropertyPropertyComparisonCondition that = (PropertyPropertyComparisonCondition) o;

        if (ignoreCase != that.ignoreCase) { return false; }
        if (!rightSidePath.equals(that.rightSidePath)) { return false; }
        return operator.equals(that.operator);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + rightSidePath.hashCode();
        result = 31 * result + operator.hashCode();
        result = 31 * result + (ignoreCase ? 1 : 0);
        return result;
    }
}
