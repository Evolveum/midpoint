/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.BaseContextClassificationUseType.IF_APPLICABLE;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugUtil;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Delineates the boundary of a resource object type (i.e. objects that belong to that type).
 *
 * @see ResourceObjectTypeDelineationType
 */
public class ResourceObjectTypeDelineation
        extends ResourceObjectSetDelineation {

    @NotNull private final Collection<QName> auxiliaryObjectClassNames;
    @Nullable private final ExpressionType condition;

    /** Contains all the other configuration items (not present above). Immutable. */
    @NotNull private final ResourceObjectTypeDelineationType delineationBean;

    private ResourceObjectTypeDelineation(
            @NotNull QName objectClassName,
            @NotNull Collection<QName> auxiliaryObjectClassNames,
            @Nullable ResourceObjectReferenceType baseContext,
            @Nullable SearchHierarchyScope searchHierarchyScope,
            @Nullable ExpressionType condition,
            @NotNull Collection<ObjectFilter> filterClauses,
            @Nullable ResourceObjectTypeDelineationType delineationBean) {
        super(objectClassName, baseContext, searchHierarchyScope, filterClauses);
        this.auxiliaryObjectClassNames = auxiliaryObjectClassNames;
        this.condition = condition;
        this.delineationBean = delineationBean != null ? delineationBean.clone() : new ResourceObjectTypeDelineationType();
        this.delineationBean.freeze();
    }

    public static ResourceObjectTypeDelineation of(@NotNull QName objectClassName) {
        return new ResourceObjectTypeDelineation(
                objectClassName, List.of(), null, null, null, List.of(), null);
    }

    public static ResourceObjectTypeDelineation of(
            @NotNull ResourceObjectTypeDelineationType bean,
            @NotNull QName objectClassName,
            @NotNull List<QName> auxiliaryObjectClassNames,
            @NotNull ResourceObjectDefinition objectDefinitionForFilterParsing)
            throws ConfigurationException {

        List<ObjectFilter> filterClauses;
        try {
            filterClauses = ShadowQueryConversionUtil.parseFilters(bean.getFilter(), objectDefinitionForFilterParsing);
            filterClauses.forEach(f -> f.freeze()); // just to be sure
        } catch (SchemaException e) {
            throw new ConfigurationException("Couldn't parse filter clauses: " + e.getMessage(), e);
        }

        return new ResourceObjectTypeDelineation(
                objectClassName,
                auxiliaryObjectClassNames,
                bean.getBaseContext(),
                SearchHierarchyScope.fromBeanValue(bean.getSearchHierarchyScope()),
                bean.getClassificationCondition(),
                List.copyOf(filterClauses),
                bean);
    }

    public static ResourceObjectTypeDelineation of(
            @Nullable ResourceObjectReferenceType baseContext,
            @Nullable SearchHierarchyScopeType searchHierarchyScope,
            @NotNull QName objectClassName,
            @NotNull List<QName> auxiliaryObjectClassNames) {
        return new ResourceObjectTypeDelineation(
                objectClassName,
                auxiliaryObjectClassNames,
                baseContext,
                SearchHierarchyScope.fromBeanValue(searchHierarchyScope),
                null,
                List.of(),
                null);
    }

    public @Nullable ExpressionType getClassificationCondition() {
        return condition;
    }

    public @NotNull BaseContextClassificationUseType getBaseContextClassificationUse() {
        return Objects.requireNonNullElse(delineationBean.getBaseContextClassificationUse(), IF_APPLICABLE);
    }

    @NotNull ResourceObjectTypeDelineation classificationCondition(ExpressionType condition) {
        return new ResourceObjectTypeDelineation(
                objectClassName, auxiliaryObjectClassNames, baseContext, searchHierarchyScope,
                condition, filterClauses, delineationBean);
    }

    Integer getClassificationOrder() {
        return delineationBean.getClassificationOrder();
    }

    @Override
    void extendDebugDump(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "auxiliaryObjectClassNames", auxiliaryObjectClassNames, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "condition", String.valueOf(condition), indent + 1); // TODO
        DebugUtil.debugDumpWithLabel(sb, "classificationOrder", getClassificationOrder(), indent + 1);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("auxiliaryObjectClassNames", auxiliaryObjectClassNames)
                .add("condition", condition)
                .add("delineationBean", delineationBean)
                .add("objectClassName", objectClassName)
                .add("baseContext", baseContext)
                .add("searchHierarchyScope", searchHierarchyScope)
                .add("filterClauses", filterClauses)
                .toString();
    }
}
