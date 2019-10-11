/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.enforcer.api.FilterGizmo;
import com.evolveum.midpoint.util.DebugDumpable;

import javax.xml.namespace.QName;

public class FilterGizmoObjectFilterImpl implements FilterGizmo<ObjectFilter> {

    private final PrismContext prismContext;

    public FilterGizmoObjectFilterImpl(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    @Override
    public ObjectFilter and(ObjectFilter a, ObjectFilter b) {
        return ObjectQueryUtil.filterAnd(a, b, prismContext);
    }

    @Override
    public ObjectFilter or(ObjectFilter a, ObjectFilter b) {
        return ObjectQueryUtil.filterOr(a, b, prismContext);
    }

    @Override
    public ObjectFilter not(ObjectFilter subfilter) {
        return prismContext.queryFactory().createNot(subfilter);
    }

    @Override
    public ObjectFilter adopt(ObjectFilter objectFilter, Authorization autz) {
        return objectFilter;
    }

    @Override
    public ObjectFilter createDenyAll() {
        return prismContext.queryFactory().createNone();
    }

    @Override
    public boolean isAll(ObjectFilter filter) {
        return ObjectQueryUtil.isAll(filter);
    }

    @Override
    public boolean isNone(ObjectFilter filter) {
        return ObjectQueryUtil.isNone(filter);
    }

    @Override
    public ObjectFilter simplify(ObjectFilter filter) {
        return ObjectQueryUtil.simplify(filter, prismContext);
    }

    @Override
    public ObjectFilter getObjectFilter(ObjectFilter filter) {
        return filter;
    }

    @Override
    public String debugDumpFilter(ObjectFilter filter, int indent) {
        return filter==null ? null : filter.debugDump(indent);
    }
}
