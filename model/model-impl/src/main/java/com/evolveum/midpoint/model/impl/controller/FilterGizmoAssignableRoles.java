/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.enforcer.api.FilterGizmo;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

public class FilterGizmoAssignableRoles implements FilterGizmo<RoleSelectionSpecification> {

    @Override
    public RoleSelectionSpecification and(RoleSelectionSpecification a, RoleSelectionSpecification b) {
        return a != null ? a.and(b) : b;
    }

    @Override
    public RoleSelectionSpecification or(RoleSelectionSpecification a, RoleSelectionSpecification b) {
        return a != null ? a.or(b) : b;
    }

    @Override
    public RoleSelectionSpecification not(RoleSelectionSpecification a) {
        return a != null ? a.not() : null;
    }

    @Override
    public RoleSelectionSpecification adopt(ObjectFilter objectFilter, Authorization autz) {
        RoleSelectionSpecification spec = new RoleSelectionSpecification();
        spec.setFilters(
                getRelations(autz),
                objectFilter);
        return spec;
    }

    private @NotNull List<QName> getRelations(Authorization autz) {
        List<QName> relations = autz.getRelation();
        // TODO: What about "any" relation?
        if (!relations.isEmpty()) {
            return relations;
        } else {
            return List.of(SchemaConstants.ORG_DEFAULT);
        }
    }

    @Override
    public RoleSelectionSpecification createDenyAll() {
        RoleSelectionSpecification spec = new RoleSelectionSpecification();
        spec.setGlobalFilter(PrismContext.get().queryFactory().createNone());
        return spec;
    }

    @Override
    public boolean isAll(RoleSelectionSpecification spec) {
        return spec == null || spec.isAll();
    }

    @Override
    public boolean isNone(RoleSelectionSpecification spec) {
        return spec != null && spec.isNone();
    }

    @Override
    public RoleSelectionSpecification simplify(RoleSelectionSpecification spec) {
        return spec != null ? spec.simplify() : null;
    }

    @Override
    public ObjectFilter getObjectFilter(RoleSelectionSpecification filter) {
        return null;
    }

    @Override
    public String debugDumpFilter(RoleSelectionSpecification filter, int indent) {
        return DebugUtil.debugDump(filter, indent);
    }

}
