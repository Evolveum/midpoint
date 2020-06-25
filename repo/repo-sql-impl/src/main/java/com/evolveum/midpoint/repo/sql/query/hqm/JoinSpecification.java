/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm;

import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
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
