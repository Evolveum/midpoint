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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
public abstract class JunctionCondition extends Condition {

    protected List<Condition> components = new ArrayList<>();

    public JunctionCondition(RootHibernateQuery rootHibernateQuery, Condition... conditions) {
        super(rootHibernateQuery);
        for (Condition condition : conditions) {
            components.add(condition);
        }
    }

    public JunctionCondition(RootHibernateQuery rootHibernateQuery, Collection<Condition> conditions) {
        super(rootHibernateQuery);
        components.addAll(conditions);
    }

    public void add(Condition condition) {
        components.add(condition);
    }

    public void dumpToHql(StringBuilder sb, int indent, String logicalOperation) {
        if (components.isEmpty()) {
            // probably some programming bug
            throw new IllegalStateException("JunctionCondition with no components: " + this.getClass());
        } else if (components.size() == 1) {
            components.get(0).dumpToHql(sb, indent);
        } else {
            HibernateQuery.indent(sb, indent);
            sb.append("(\n");
            boolean first = true;
            for (Condition component : components) {
                if (first) {
                    first = false;
                } else {
                    sb.append(" ").append(logicalOperation).append("\n");
                }
                component.dumpToHql(sb, indent + 1);
            }
            sb.append("\n");
            HibernateQuery.indent(sb, indent);
            sb.append(")");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JunctionCondition that = (JunctionCondition) o;

        return components.equals(that.components);

    }

    @Override
    public int hashCode() {
        return components.hashCode();
    }
}
