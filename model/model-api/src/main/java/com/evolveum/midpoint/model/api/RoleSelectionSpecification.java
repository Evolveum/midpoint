/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

import javax.xml.namespace.QName;

/**
 * @author semancik
 */
public class RoleSelectionSpecification implements DebugDumpable {

    ObjectFilter globalFilter = null;
    private final Map<QName,ObjectFilter> relationMap = new HashMap<>();

    public int size() {
        return relationMap.size();
    }

    public Map<QName, ObjectFilter> getRelationMap() {
        return relationMap;
    }

    public ObjectFilter getGlobalFilter() {
        return globalFilter;
    }

    public void setGlobalFilter(ObjectFilter filter) {
        globalFilter = filter;
    }

    public ObjectFilter getRelationFilter(QName relation) {
        return relationMap.get(relation);
    }

    public void setFilters(List<QName> relations, ObjectFilter objectFilter) {
        for (QName relation : relations) {
            ObjectFilter relationFilter = relationMap.get(relation);
            if (relationFilter == null) {
                relationFilter = objectFilter;
            }
            relationMap.put(relation, relationFilter);
        }
    }

    public boolean isAll() {
        // Is this correct?
        return ObjectQueryUtil.isAll(globalFilter);
    }

    public boolean isNone() {
        // Is this correct?
        return ObjectQueryUtil.isNone(globalFilter);
    }

    public RoleSelectionSpecification and(RoleSelectionSpecification other) {
        if (other == null) {
            return this;
        }
        if (other == this) {
            return this;
        }
        return applyBinary(other, (a, b) -> ObjectQueryUtil.filterAnd(a, b) );
    }

    public RoleSelectionSpecification or(RoleSelectionSpecification other) {
        if (other == null) {
            return this;
        }
        if (other == this) {
            return this;
        }
        return applyBinary(other, (a, b) -> ObjectQueryUtil.filterOr(a, b) );
    }

    public RoleSelectionSpecification not() {
        return applyUnary((a) -> PrismContext.get().queryFactory().createNot(a));
    }

    public RoleSelectionSpecification simplify() {
        return applyUnary((a) -> ObjectQueryUtil.simplify(a));
    }

    private RoleSelectionSpecification applyBinary(RoleSelectionSpecification other, BinaryOperator<ObjectFilter> operator) {
        RoleSelectionSpecification out = new RoleSelectionSpecification();
        out.globalFilter = operator.apply(this.globalFilter, other.globalFilter);
        out.relationMap.putAll(this.relationMap);
        other.relationMap.forEach(
                (otherKey, otherValue) -> out.relationMap.merge(otherKey, otherValue, operator)
        );
        return out;
    }

    private RoleSelectionSpecification applyUnary(UnaryOperator<ObjectFilter> operator) {
        RoleSelectionSpecification out = new RoleSelectionSpecification();
        out.globalFilter = operator.apply(this.globalFilter);
        this.relationMap.forEach(
                (thisKey, thisValue) -> out.relationMap.put(thisKey, operator.apply(thisValue))
        );
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleSelectionSpecification that = (RoleSelectionSpecification) o;
        return relationMap.equals(that.relationMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationMap);
    }

    @Override
    public String toString() {
        return "RoleSelectionSpecification(" + globalFilter + " : " + relationMap + ")";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(RoleSelectionSpecification.class, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "globalFilter", globalFilter, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "relationMap", relationMap, indent + 1);
        return sb.toString();
    }

}
