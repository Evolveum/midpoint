/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query2.hqm.condition;

import com.evolveum.midpoint.repo.sql.query2.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;

import org.apache.commons.lang.Validate;

import java.util.Collection;
import java.util.Objects;

/**
 * @author mederly
 */
public class InCondition extends PropertyCondition {

    private Collection<?> values;
    private String innerQueryText;

    public InCondition(RootHibernateQuery rootHibernateQuery, String propertyPath, String innerQueryText) {
        super(rootHibernateQuery, propertyPath);
        Validate.notNull(innerQueryText);
        this.innerQueryText = innerQueryText;
    }

    public InCondition(RootHibernateQuery rootHibernateQuery, String propertyPath, Collection<?> values) {
        super(rootHibernateQuery, propertyPath);
        Validate.notNull(values);
        this.values = values;
    }

    @Override
    public void dumpToHql(StringBuilder sb, int indent) {
        HibernateQuery.indent(sb, indent);
        if (values != null) {
            String parameterNamePrefix = createParameterName(propertyPath);
            String parameterName = rootHibernateQuery.addParameter(parameterNamePrefix, values);        // TODO special treatment of collections?
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
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }

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
