/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import java.util.Objects;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query.hqm.RootHibernateQuery;

public class SimpleComparisonCondition extends PropertyCondition {

    private final Object value;
    private final String operator;
    private final boolean ignoreCase;

    public SimpleComparisonCondition(RootHibernateQuery rootHibernateQuery, String propertyPath, Object value, String operator, boolean ignoreCase) {
        super(rootHibernateQuery, propertyPath);
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(operator, "operator");

        this.value = value;
        this.operator = operator;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public void dumpToHql(StringBuilder sb, int indent) {
        HibernateQuery.indent(sb, indent);

        String finalPropertyPath;
        Object finalPropertyValue;
        if (ignoreCase) {
            finalPropertyPath = "lower(" + propertyPath + ")";
            if (value instanceof String) {
                finalPropertyValue = ((String) value).toLowerCase();
            } else {
                throw new IllegalStateException("Non-string values cannot be compared with ignoreCase option: " + value);
            }
        } else {
            finalPropertyPath = propertyPath;
            finalPropertyValue = value;
        }

        String parameterNamePrefix = createParameterName(propertyPath);
        String parameterName = rootHibernateQuery.addParameter(parameterNamePrefix, finalPropertyValue);
        sb.append(finalPropertyPath).append(" ").append(operator).append(" :").append(parameterName);
        // See RootHibernateQuery.createLike for the other part of the solution.
        // Design note: probably a bit cyclic dependency, but the knowledge about escaping still
        // needs to be on both places anyway, so it's less messy than an additional parameter.
        if (operator.equals("like")) {
            sb.append(" escape '" + RootHibernateQuery.LIKE_ESCAPE_CHAR + '\'');
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }

        SimpleComparisonCondition that = (SimpleComparisonCondition) o;

        return ignoreCase == that.ignoreCase
                && Objects.equals(value, that.value)
                && operator.equals(that.operator);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + operator.hashCode();
        result = 31 * result + (ignoreCase ? 1 : 0);
        return result;
    }
}
