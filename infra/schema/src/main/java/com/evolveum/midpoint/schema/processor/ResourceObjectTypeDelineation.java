/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.BaseContextClassificationUseType.IF_APPLICABLE;

/**
 * Delineates the boundary of a resource object type (i.e. objects that belong to that type).
 *
 * @see ResourceObjectTypeDelineationType
 */
public class ResourceObjectTypeDelineation implements Serializable {

    @NotNull private final QName objectClassName;
    @NotNull private final Collection<QName> auxiliaryObjectClassNames;
    @Nullable private final ResourceObjectReferenceType baseContext;
    @Nullable private final SearchHierarchyScope searchHierarchyScope;
    @Nullable private final ExpressionType condition;

    /** Contains all the other configuration items (not present above). Immutable. */
    @NotNull private final ResourceObjectTypeDelineationType delineationBean;

    private ResourceObjectTypeDelineation(
            @NotNull QName objectClassName,
            @NotNull Collection<QName> auxiliaryObjectClassNames,
            @Nullable ResourceObjectReferenceType baseContext,
            @Nullable SearchHierarchyScope searchHierarchyScope,
            @Nullable ExpressionType condition,
            @Nullable ResourceObjectTypeDelineationType delineationBean) {
        this.objectClassName = objectClassName;
        this.auxiliaryObjectClassNames = auxiliaryObjectClassNames;
        this.baseContext = baseContext;
        this.searchHierarchyScope = searchHierarchyScope;
        this.condition = condition;
        this.delineationBean = delineationBean != null ? delineationBean.clone() : new ResourceObjectTypeDelineationType();
        this.delineationBean.freeze();
    }

    public static ResourceObjectTypeDelineation of(@NotNull QName objectClassName) {
        return new ResourceObjectTypeDelineation(
                objectClassName, List.of(), null, null, null, null);
    }

    public static ResourceObjectTypeDelineation of(
            @NotNull ResourceObjectTypeDelineationType bean,
            @NotNull QName objectClassName,
            @NotNull List<QName> auxiliaryObjectClassNames) throws ConfigurationException {
        return new ResourceObjectTypeDelineation(
                objectClassName,
                auxiliaryObjectClassNames,
                bean.getBaseContext(),
                SearchHierarchyScope.fromBeanValue(bean.getSearchHierarchyScope()),
                bean.getClassificationCondition(),
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
                null);
    }

    public @NotNull QName getObjectClassName() {
        return objectClassName;
    }

    public @Nullable ResourceObjectReferenceType getBaseContext() {
        return baseContext;
    }

    public @Nullable SearchHierarchyScope getSearchHierarchyScope() {
        return searchHierarchyScope;
    }

    public @NotNull List<SearchFilterType> getFilterClauses() {
        return delineationBean.getFilter();
    }

    public @Nullable ExpressionType getClassificationCondition() {
        return condition;
    }

    public @NotNull BaseContextClassificationUseType getBaseContextClassificationUse() {
        return Objects.requireNonNullElse(delineationBean.getBaseContextClassificationUse(), IF_APPLICABLE);
    }

    @NotNull ResourceObjectTypeDelineation classificationCondition(ExpressionType condition) {
        return new ResourceObjectTypeDelineation(
                objectClassName, auxiliaryObjectClassNames, baseContext, searchHierarchyScope, condition, delineationBean);
    }

    Integer getClassificationOrder() {
        return delineationBean.getClassificationOrder();
    }
}
