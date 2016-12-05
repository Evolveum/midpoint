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

import com.evolveum.midpoint.repo.sql.query2.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import org.apache.commons.lang.Validate;

import java.util.Collection;

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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        InCondition that = (InCondition) o;

        if (values != null ? !values.equals(that.values) : that.values != null) return false;
        return !(innerQueryText != null ? !innerQueryText.equals(that.innerQueryText) : that.innerQueryText != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (values != null ? values.hashCode() : 0);
        result = 31 * result + (innerQueryText != null ? innerQueryText.hashCode() : 0);
        return result;
    }
}
