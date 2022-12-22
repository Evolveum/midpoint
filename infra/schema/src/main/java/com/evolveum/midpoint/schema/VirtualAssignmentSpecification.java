/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;

public class VirtualAssignmentSpecification<R extends AbstractRoleType> {

    private ObjectFilter filter;
    private Class<R> type;

    public ObjectFilter getFilter() {
        return filter;
    }

    public Class<R> getType() {
        return type;
    }

    public void setFilter(ObjectFilter filter) {
        this.filter = filter;
    }

    public void setType(Class<R> type) {
        this.type = type;
    }
}
