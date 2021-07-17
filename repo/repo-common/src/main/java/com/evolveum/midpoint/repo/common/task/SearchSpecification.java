/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class SearchSpecification<O extends ObjectType> implements DebugDumpable, Cloneable {

    /**
     * Object type provided when counting and retrieving objects.
     */
    private Class<O> objectType;

    /** Object query specifying what objects to process. */
    private ObjectQuery query;

    /**
     * Options to be used during counting and searching.
     */
    private Collection<SelectorOptions<GetOperationOptions>> searchOptions;

    /**
     * Whether we want to use repository directly when counting/searching.
     * Can be "built-in" in the activity, or requested explicitly by the user.
     * In the latter case the raw authorization is checked. (Unless overridden by activity.)
     *
     * Note that this flag is really used only if model processing is available.
     */
    private Boolean useRepository;

    public SearchSpecification(Class<O> objectType, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> searchOptions, Boolean useRepository) {
        this.objectType = objectType;
        this.query = query;
        this.searchOptions = searchOptions;
        this.useRepository = useRepository;
    }

    @SuppressWarnings("CopyConstructorMissesField")
    protected SearchSpecification(SearchSpecification<O> prototype) {
        this(prototype.objectType,
                CloneUtil.clone(prototype.query),
                CloneUtil.cloneCollectionMembers(prototype.searchOptions),
                prototype.useRepository);
    }

    @NotNull static <O extends ObjectType> SearchSpecification<O> fromRepositoryObjectSetSpecification(
            @NotNull RepositoryObjectSetSpecificationImpl objectSetSpecification) throws SchemaException {
        //noinspection unchecked
        Class<O> objectType = (Class<O>) determineObjectType(objectSetSpecification);
        return new SearchSpecification<>(
                objectType,
                createObjectQuery(objectType, objectSetSpecification.getQueryBean()),
                MiscSchemaUtil.optionsTypeToOptions(objectSetSpecification.getSearchOptionsBean(), PrismContext.get()),
                objectSetSpecification.isUseRepositoryDirectly());
    }

    /**
     * TODO move to prism-api
     */
    private static @NotNull ObjectQuery createObjectQuery(@NotNull Class<? extends ObjectType> objectType,
            @Nullable QueryType query) throws SchemaException {
        return Objects.requireNonNullElseGet(
                PrismContext.get().getQueryConverter().createObjectQuery(objectType, query),
                () -> PrismContext.get().queryFactory().createQuery());
    }

    private static @NotNull Class<?> determineObjectType(@NotNull ObjectSetSpecification set) {
        return getTypeFromName(
                MoreObjects.firstNonNull(
                        set.getObjectType(), ObjectType.COMPLEX_TYPE));
    }

    private static @NotNull Class<?> getTypeFromName(@NotNull QName typeName) {
        return ObjectTypes.getObjectTypeFromTypeQName(typeName).getClassDefinition();
    }

    public Class<O> getObjectType() {
        return objectType;
    }

    public void setObjectType(Class<O> objectType) {
        this.objectType = objectType;
    }

    public ObjectQuery getQuery() {
        return query;
    }

    public void setQuery(ObjectQuery query) {
        this.query = query;
    }

    public Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return searchOptions;
    }

    public void setSearchOptions(Collection<SelectorOptions<GetOperationOptions>> searchOptions) {
        this.searchOptions = searchOptions;
    }

    public Boolean getUseRepository() {
        return useRepository;
    }

    public void setUseRepository(Boolean useRepository) {
        this.useRepository = useRepository;
    }

    public boolean concernsShadows() {
        return ShadowType.class.equals(requireNonNull(objectType));
    }

    public boolean isNoFetch() {
        return GetOperationOptions.isNoFetch(getRootOptions());
    }

    public boolean isRaw() {
        return GetOperationOptions.isRaw(getRootOptions());
    }

    @Nullable public GetOperationOptions getRootOptions() {
        return SelectorOptions.findRootOptions(searchOptions);
    }

    public void setNoFetchOption() {
        searchOptions = GetOperationOptions.updateToNoFetch(searchOptions);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "objectType", objectType, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "query", query, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "searchOptions", searchOptions, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "useRepository", useRepository, indent + 1);
        return sb.toString();
    }

    @SuppressWarnings({ "MethodDoesntCallSuperMethod" })
    @Override
    public SearchSpecification<O> clone() {
        return new SearchSpecification<>(this);
    }
}
