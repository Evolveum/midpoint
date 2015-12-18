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

/**
 * @author mederly
 */
public class PropertyPropertyComparisonCondition extends PropertyCondition {

    private String rightSidePath;
    private String operator;
    private boolean ignoreCase;

    public PropertyPropertyComparisonCondition(RootHibernateQuery rootHibernateQuery, String propertyPath, String rightSidePath, String operator, boolean ignoreCase) {
        super(rootHibernateQuery, propertyPath);
        Validate.notNull(rightSidePath, "rightSidePath");
        Validate.notNull(operator, "operator");
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PropertyPropertyComparisonCondition that = (PropertyPropertyComparisonCondition) o;

        if (ignoreCase != that.ignoreCase) return false;
        if (!rightSidePath.equals(that.rightSidePath)) return false;
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
