/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import java.util.Objects;

/**
 * Specific for SQL Server.
 * EXPERIMENTAL
 *
 * @author mederly
 */
public class ExistsCondition extends Condition {

    private String innerQueryText;
    private String linkingCondition;

    public ExistsCondition(RootHibernateQuery rootHibernateQuery, String innerQueryText, String linkingCondition) {
        super(rootHibernateQuery);
        Validate.notNull(innerQueryText);
        Validate.notNull(linkingCondition);
        this.innerQueryText = innerQueryText;
        this.linkingCondition = linkingCondition;
    }

    @Override
    public void dumpToHql(StringBuilder sb, int indent) {
        HibernateQuery.indent(sb, indent);
        sb.append(" exists (").append(innerQueryText).append(" ").append(linkingCondition).append(")");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ExistsCondition))
            return false;
        ExistsCondition that = (ExistsCondition) o;
        return Objects.equals(innerQueryText, that.innerQueryText) &&
                Objects.equals(linkingCondition, that.linkingCondition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(innerQueryText, linkingCondition);
    }
}
