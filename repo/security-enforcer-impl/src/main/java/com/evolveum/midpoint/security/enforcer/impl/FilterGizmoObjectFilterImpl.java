/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.enforcer.api.FilterGizmo;
import com.evolveum.midpoint.util.DebugUtil;

public class FilterGizmoObjectFilterImpl implements FilterGizmo<ObjectFilter> {

    @Override
    public ObjectFilter and(ObjectFilter a, ObjectFilter b) {
        return ObjectQueryUtil.filterAnd(a, b);
    }

    @Override
    public ObjectFilter or(ObjectFilter a, ObjectFilter b) {
        return ObjectQueryUtil.filterOr(a, b);
    }

    @Override
    public ObjectFilter not(ObjectFilter subfilter) {
        return PrismContext.get().queryFactory().createNot(subfilter);
    }

    @Override
    public ObjectFilter adopt(ObjectFilter objectFilter, Authorization autz) {
        return objectFilter;
    }

    @Override
    public ObjectFilter createDenyAll() {
        return PrismContext.get().queryFactory().createNone();
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
        return ObjectQueryUtil.simplify(filter);
    }

    @Override
    public ObjectFilter getObjectFilter(ObjectFilter filter) {
        return filter;
    }

    @Override
    public String debugDumpFilter(ObjectFilter filter, int indent) {
        return DebugUtil.debugDump(filter, indent);
    }
}
