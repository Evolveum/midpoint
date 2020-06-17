/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm;

import java.util.List;

/**
 * @author mederly
 */
public abstract class ProjectionElement {

    public static void dumpToHql(StringBuilder sb, List<ProjectionElement> projectionElements, int indent) {
        boolean first = true;
        for (ProjectionElement element : projectionElements) {
            if (first) {
                first = false;
            } else {
                sb.append(",\n");
            }
            HibernateQuery.indent(sb, indent);
            element.dumpToHql(sb);
        }
    }

    protected abstract void dumpToHql(StringBuilder sb);
}
