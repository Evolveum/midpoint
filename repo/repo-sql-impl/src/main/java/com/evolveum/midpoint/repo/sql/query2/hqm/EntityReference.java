/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query2.hqm;

import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.List;

/**
 * Specifies an entity that is to be used in the query (its name and its alias),
 * along with any entities joined by associations.
 *
 *
 *
 * @author mederly
 */
public class EntityReference {

    private String alias;
    private String name;

    /**
     * Joined entities, e.g. for RUser u here could be:
     *  - u.assignments a
     *  - u.longs l with l.ownerType = EXTENSION and l.name = 'http://example.com/p#intType'
     * etc.
     */
    private List<JoinSpecification> joins = new ArrayList<>();

    public EntityReference(String alias, String name) {
        Validate.notEmpty(alias);
        Validate.notEmpty(name);

        this.alias = alias;
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<JoinSpecification> getJoins() {
        return joins;
    }

    public void addJoin(JoinSpecification join) {
        joins.add(join);
    }

    public void dumpToHql(StringBuilder sb, int indent) {
        HibernateQuery.indent(sb, indent);
        sb.append(name).append(" ").append(alias);
        if (!joins.isEmpty()) {
            sb.append("\n");
            JoinSpecification.dumpToHql(sb, joins, indent + 1);
        }

    }

    public boolean containsAlias(String alias) {
        if (this.alias.equals(alias)) {
            return true;
        }
        for (JoinSpecification join : joins) {
            if (join.getAlias().equals(alias)) {
                return true;
            }
        }
        return false;
    }

    public JoinSpecification findJoinFor(String path) {
        for (JoinSpecification join : joins) {
            if (path.equals(join.getPath())) {
                return join;
            }
        }
        return null;
    }
}
