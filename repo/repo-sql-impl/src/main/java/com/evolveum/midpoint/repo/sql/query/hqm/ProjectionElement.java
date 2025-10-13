/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.hqm;

import java.util.List;

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
