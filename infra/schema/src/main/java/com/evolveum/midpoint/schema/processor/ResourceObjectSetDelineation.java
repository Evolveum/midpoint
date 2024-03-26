/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Collection;

/**
 * Generalized specification of a set of resource objects.
 *
 * TODO consider if auxiliary object class(es) belong here
 *
 * TODO later, we may move filter(s) here as well
 */
public class ResourceObjectSetDelineation implements DebugDumpable, Serializable {

    /**
     * There is intentionally only a single class name here. The reason is that we cannot execute resource searches
     * for multiple classes, anyway. Hence, for simulated associations, multi-classes delineation beans are split into
     * multiple parsed delineations.
     */
    @NotNull final QName objectClassName;
    @Nullable final ResourceObjectReferenceType baseContext;
    @Nullable final SearchHierarchyScope searchHierarchyScope;
    @NotNull final Collection<ObjectFilter> filterClauses;

    ResourceObjectSetDelineation(
            @NotNull QName objectClassName,
            @Nullable ResourceObjectReferenceType baseContext,
            @Nullable SearchHierarchyScope searchHierarchyScope,
            @NotNull Collection<ObjectFilter> filterClauses) {
        this.objectClassName = objectClassName;
        this.baseContext = baseContext;
        this.searchHierarchyScope = searchHierarchyScope;
        this.filterClauses = filterClauses;
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

    public @NotNull Collection<ObjectFilter> getFilterClauses() {
        return filterClauses;
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "objectClassName", objectClassName, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "baseContext", baseContext, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "searchHierarchyScope", searchHierarchyScope, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "filterClauses", filterClauses, indent + 1);
        extendDebugDump(sb, indent);
        return sb.toString();
    }

    void extendDebugDump(StringBuilder sb, int indent) {
    }

    @Override
    public String toString() {
        return "ResourceObjectSetDelineation{" +
                "objectClassName=" + objectClassName +
                ", baseContext=" + baseContext +
                ", searchHierarchyScope=" + searchHierarchyScope +
                ", filterClauses=" + filterClauses +
                '}';
    }
}
