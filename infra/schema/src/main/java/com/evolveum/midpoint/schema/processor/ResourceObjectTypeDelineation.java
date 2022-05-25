/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDelineationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchHierarchyScopeType;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Delineates the boundary of a resource object type (i.e. objects that belong to that type).
 *
 * @see ResourceObjectTypeDelineationType
 */
public class ResourceObjectTypeDelineation implements Serializable {

    @Nullable private final ResourceObjectReferenceType baseContext;
    @Nullable private final SearchHierarchyScope searchHierarchyScope;
    @Nullable private final SearchFilterType filter;
    @NotNull private final List<SearchFilterType> filterClauses;
    @Nullable private final ExpressionType classificationCondition;

    private ResourceObjectTypeDelineation(
            @Nullable ResourceObjectReferenceType baseContext,
            @Nullable SearchHierarchyScope searchHierarchyScope,
            @Nullable SearchFilterType filter,
            @NotNull List<SearchFilterType> filterClauses,
            @Nullable ExpressionType classificationCondition) {
        this.baseContext = baseContext;
        this.searchHierarchyScope = searchHierarchyScope;
        this.filter = filter;
        this.filterClauses = filterClauses;
        this.classificationCondition = classificationCondition;
    }

    public static ResourceObjectTypeDelineation none() {
        return new ResourceObjectTypeDelineation(
                null, null, null, List.of(), null);
    }

    public static ResourceObjectTypeDelineation of(
            @NotNull ResourceObjectTypeDelineationType bean) {
        return new ResourceObjectTypeDelineation(
                bean.getBaseContext(),
                SearchHierarchyScope.fromBeanValue(bean.getSearchHierarchyScope()),
                bean.getFilter(),
                bean.getFilterClause(),
                bean.getClassificationCondition());
    }

    public static ResourceObjectTypeDelineation of(
            @Nullable ResourceObjectReferenceType baseContext,
            @Nullable SearchHierarchyScopeType searchHierarchyScope) {
        return new ResourceObjectTypeDelineation(
                baseContext,
                SearchHierarchyScope.fromBeanValue(searchHierarchyScope),
                null,
                List.of(),
                null);
    }

    public @Nullable ResourceObjectReferenceType getBaseContext() {
        return baseContext;
    }

    public @Nullable SearchHierarchyScope getSearchHierarchyScope() {
        return searchHierarchyScope;
    }

    public @NotNull List<SearchFilterType> getAllFilterClauses() {
        List<SearchFilterType> all = new ArrayList<>();
        if (filter != null) {
            all.add(filter);
        }
        all.addAll(filterClauses);
        return all;
    }

    public @Nullable ExpressionType getClassificationCondition() {
        return classificationCondition;
    }

    @NotNull ResourceObjectTypeDelineation classificationCondition(ExpressionType condition) {
        return new ResourceObjectTypeDelineation(
                baseContext, searchHierarchyScope, filter, filterClauses, condition);
    }
}
