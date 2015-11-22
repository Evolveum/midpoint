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

package com.evolveum.midpoint.repo.sql.query2.hqm;

import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import org.apache.commons.lang.Validate;

import java.util.List;

/**
 * @author mederly
 */
public class JoinSpecification {

    private String alias;
    private String path;
    private Condition condition;

    public JoinSpecification(String alias, String path, Condition condition) {
        Validate.notNull(alias);
        Validate.notNull(path);

        this.alias = alias;
        this.path = path;
        this.condition = condition;
    }

    public String getAlias() {
        return alias;
    }

    public String getPath() {
        return path;
    }

    public Condition getCondition() {
        return condition;
    }

    public static void dumpToHql(StringBuilder sb, List<JoinSpecification> joins, int indent) {
        boolean first = true;
        for (JoinSpecification join : joins) {
            if (first) {
                first = false;
            } else {
                sb.append("\n");
            }
            HibernateQuery.indent(sb, indent);
            join.dumpToHql(sb);
        }
    }

    private void dumpToHql(StringBuilder sb) {
        sb.append("left join ");
        sb.append(path).append(" ").append(alias);
        if (condition != null) {
            sb.append(" with ");
            condition.dumpToHql(sb, -1);
        }
    }
}
