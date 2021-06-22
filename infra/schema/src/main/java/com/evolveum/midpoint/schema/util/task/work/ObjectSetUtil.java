/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task.work;

import static com.evolveum.midpoint.schema.util.task.work.ResourceObjectSetUtil.getItemRealValue;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.FilterUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.SelectorQualifiedGetOptionsUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.Nullable;

public class ObjectSetUtil {

    public static @NotNull ObjectSetType fromLegacySource(@NotNull LegacyWorkDefinitionSource source) {
        @Nullable PrismContainerValue<?> extension = source.getTaskExtension();
        return new ObjectSetType(PrismContext.get())
                .type(getItemRealValue(extension, SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE, QName.class))
                .query(getQueryLegacy(source))
                .searchOptions(getSearchOptionsLegacy(extension))
                .useRepositoryDirectly(getUseRepositoryDirectly(extension));
    }

    private static Boolean getUseRepositoryDirectly(PrismContainerValue<?> extension) {
        return getItemRealValue(extension, SchemaConstants.MODEL_EXTENSION_USE_REPOSITORY_DIRECTLY, Boolean.class);
    }

    static QueryType getQueryLegacy(@NotNull LegacyWorkDefinitionSource source) {
        QueryType fromObjectRef = getQueryFromTaskObjectRef(source.getObjectRef());
        if (fromObjectRef != null) {
            return fromObjectRef;
        }

        return source.getExtensionItemRealValue(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY, QueryType.class);
    }

    private static QueryType getQueryFromTaskObjectRef(ObjectReferenceType objectRef) {
        if (objectRef == null) {
            return null;
        }
        SearchFilterType filterType = objectRef.getFilter();
        if (filterType == null || FilterUtil.isFilterEmpty(filterType)) {
            return null;
        }
        QueryType queryType = new QueryType();
        queryType.setFilter(filterType);
        return queryType;
    }

    static @NotNull SelectorQualifiedGetOptionsType getSearchOptionsLegacy(PrismContainerValue<?> extension) {
        SelectorQualifiedGetOptionsType optionsBean = java.util.Objects.requireNonNullElseGet(
                getItemRealValue(extension, SchemaConstants.MODEL_EXTENSION_SEARCH_OPTIONS, SelectorQualifiedGetOptionsType.class),
                () -> new SelectorQualifiedGetOptionsType(PrismContext.get()));

        IterationMethodType iterationMethod
                = getItemRealValue(extension, SchemaConstants.MODEL_EXTENSION_ITERATION_METHOD, IterationMethodType.class);
        if (iterationMethod != null) {
            SelectorQualifiedGetOptionsUtil.merge(optionsBean, iterationMethod);
        }
        return optionsBean;
    }

    public static ObjectSetType fromRef(ObjectReferenceType ref, QName defaultTypeName) {
        if (ref == null) {
            return new ObjectSetType(PrismContext.get())
                    .type(defaultTypeName);
        } else if (ref.getOid() != null) {
            return new ObjectSetType(PrismContext.get())
                    .type(getTypeName(ref, defaultTypeName))
                    .query(createOidQuery(ref.getOid()));
        } else {
            return new ObjectSetType(PrismContext.get())
                    .type(getTypeName(ref, defaultTypeName))
                    .query(new QueryType()
                            .filter(ref.getFilter()));
        }
    }

    private static QueryType createOidQuery(@NotNull String oid) {
        try {
            return PrismContext.get().getQueryConverter().createQueryType(
                    PrismContext.get().queryFor(ObjectType.class)
                            .id(oid)
                            .build());
        } catch (SchemaException e) {
            throw new SystemException(e);
        }
    }

    private static QName getTypeName(ObjectReferenceType ref, QName defaultTypeName) {
        return ref.getType() != null ? ref.getType() : defaultTypeName;
    }

    /**
     * Fills-in the expected type or checks that provided one is not contradicting it.
     */
    public static void assumeObjectType(@NotNull ObjectSetType set, @NotNull QName superType) {
        if (superType.equals(set.getType())) {
            return;
        }
        if (set.getType() == null || QNameUtil.match(set.getType(), superType)) {
            set.setType(superType);
            return;
        }
        argCheck(PrismContext.get().getSchemaRegistry().isAssignableFrom(superType, set.getType()),
                "Activity requires object type of %s, but %s was provided in the work definition",
                superType, set.getType());
    }

    public static void applyDefaultObjectType(@NotNull ObjectSetType set, @NotNull QName type) {
        if (set.getType() == null) {
            set.setType(type);
        }
    }
}
