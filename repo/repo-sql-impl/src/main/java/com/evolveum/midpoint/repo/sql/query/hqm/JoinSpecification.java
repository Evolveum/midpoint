/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm;

import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;

/**
 * @author mederly
 */
public class JoinSpecification {

    private final String alias;
    private final String path;
    private final Condition condition;

    public JoinSpecification(String alias, String path, Condition condition) {
        Objects.requireNonNull(alias);
        Objects.requireNonNull(path);

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
