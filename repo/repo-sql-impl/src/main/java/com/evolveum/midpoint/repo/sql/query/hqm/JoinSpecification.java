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

import org.jetbrains.annotations.Nullable;

public class JoinSpecification {

    /**
     * If specified, we want to create "explicit root joins" (kind of SQL-style), in order to tell Hibernate
     * what type of entity we want to join. This is needed for e.g. `archetypeRef/@/name` queries. See MID-9472.
     * See https://docs.jboss.org/hibernate/orm/6.4/querylanguage/html_single/Hibernate_Query_Language.html#root-join.
     */
    @Nullable private final String explicitJoinedType;

    private final String alias;
    private final String path;
    private final Condition condition;

    public JoinSpecification(
            @Nullable String explicitJoinedType, String alias, String path, Condition condition) {
        Objects.requireNonNull(alias);
        Objects.requireNonNull(path);

        this.explicitJoinedType = explicitJoinedType;
        this.alias = alias;
        this.path = path;
        this.condition = condition;
    }

    public @Nullable String getExplicitJoinedType() {
        return explicitJoinedType;
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
        if (explicitJoinedType != null) {
            sb.append(explicitJoinedType).append(" ").append(alias);
            sb.append(" on ").append(path).append(" = ").append(alias);
            if (condition != null) {
                sb.append(" and (");
                condition.dumpToHql(sb, -1);
                sb.append(")");
            }
        } else {
            sb.append(path).append(" ").append(alias);
            if (condition != null) {
                sb.append(" with ");
                condition.dumpToHql(sb, -1);
            }
        }
    }
}
