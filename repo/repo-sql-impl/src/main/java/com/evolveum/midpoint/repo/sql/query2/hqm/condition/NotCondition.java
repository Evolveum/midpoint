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
public class NotCondition extends Condition {

    protected Condition child;

    public NotCondition(RootHibernateQuery rootHibernateQuery, Condition child) {
        super(rootHibernateQuery);
        Validate.notNull(child, "child");
        this.child = child;
    }

    @Override
    public void dumpToHql(StringBuilder sb, int indent) {
        HibernateQuery.indent(sb, indent);
        sb.append("not ");
        child.dumpToHql(sb, -1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NotCondition that = (NotCondition) o;

        return child.equals(that.child);

    }

    @Override
    public int hashCode() {
        return child.hashCode();
    }
}
