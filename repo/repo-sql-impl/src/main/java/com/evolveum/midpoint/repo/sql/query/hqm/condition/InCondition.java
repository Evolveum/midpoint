/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import java.util.Collection;
import java.util.Objects;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;

public class InCondition extends PropertyCondition {

    private Collection<?> values;
    private String innerQueryText;

    public InCondition(HibernateQuery hibernateQuery, String propertyPath, String innerQueryText) {
        super(hibernateQuery, propertyPath);
        Objects.requireNonNull(innerQueryText);
        this.innerQueryText = innerQueryText;
    }

    public InCondition(HibernateQuery rootHibernateQuery, String propertyPath, Collection<?> values) {
        super(rootHibernateQuery, propertyPath);
        Objects.requireNonNull(values);
        this.values = values;
    }

    @Override
    public void dumpToHql(StringBuilder sb, int indent) {
        HibernateQuery.indent(sb, indent);
        if (values != null) {
            String parameterNamePrefix = createParameterName(propertyPath);
            String parameterName = hibernateQuery.addParameter(parameterNamePrefix, values); // TODO special treatment of collections?
            // these parentheses are here because of hibernate bug, manifesting itself as MID-3390
            boolean useParentheses = values.size() != 1;        // just a (quite dubious) optimization
            sb.append(propertyPath).append(" in ")
                    .append(useParentheses ? "(" : "")
                    .append(":").append(parameterName)
                    .append(useParentheses ? ")" : "");
        } else {
            sb.append(propertyPath).append(" in (").append(innerQueryText).append(")");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        InCondition that = (InCondition) o;

        return Objects.equals(values, that.values)
                && Objects.equals(innerQueryText, that.innerQueryText);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (values != null ? values.hashCode() : 0);
        result = 31 * result + (innerQueryText != null ? innerQueryText.hashCode() : 0);
        return result;
    }
}
