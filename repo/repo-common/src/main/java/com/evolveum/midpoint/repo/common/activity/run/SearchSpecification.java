/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecification;
import com.evolveum.midpoint.repo.common.activity.definition.RepositoryObjectSetSpecificationImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.GetOperationOptionsUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * "Compiled" specification of items that are to be processed by {@link SearchBasedActivityRun}.
 * Basically, this is a grouping of type + query + options + "use repo" flag.
 *
 * This object has writable fields, as it can be modified during activity run.
 *
 * There is a subclass dedicated to searching for objects on a resource (`ResourceSearchSpecification`).
 *
 * Note that the archetype present in {@link ObjectSetType} is intentionally not present here.
 * If present, it is included in the query.
 *
 * @param <C> Type of items
 */
public class SearchSpecification<C extends Containerable> implements DebugDumpable, Cloneable {

    /**
     * Container type provided when counting and retrieving objects.
     */
    @NotNull private final Class<C> type;

    /** Query specifying what objects to process. */
    @Nullable private ObjectQuery query;

    /**
     * Options to be used during counting and searching.
     */
    @Nullable private Collection<SelectorOptions<GetOperationOptions>> searchOptions;

    /**
     * Whether we want to use repository directly when counting/searching.
     * Can be "built-in" in the activity, or requested explicitly by the user.
     * In the latter case the raw authorization is checked. (Unless overridden by activity.)
     *
     * Note that this flag is really used only if model processing is available.
     */
    @Nullable private Boolean useRepository;

    public SearchSpecification(
            @NotNull Class<C> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> searchOptions,
            @Nullable Boolean useRepository) {
        this.type = type;
        this.query = query;
        this.searchOptions = searchOptions;
        this.useRepository = useRepository;
    }

    protected SearchSpecification(@NotNull SearchSpecification<C> prototype) {
        this(prototype.type,
                CloneUtil.clone(prototype.query),
                CloneUtil.cloneCollectionMembers(prototype.searchOptions),
                prototype.useRepository);
    }

    @NotNull static <C extends Containerable> SearchSpecification<C> fromRepositoryObjectSetSpecification(
            @NotNull RepositoryObjectSetSpecificationImpl objectSetSpecification) throws SchemaException, ConfigurationException {
        //noinspection unchecked
        Class<C> containerType = (Class<C>) determineContainerType(objectSetSpecification);
        return new SearchSpecification<>(
                containerType,
                createObjectQuery(containerType, objectSetSpecification),
                GetOperationOptionsUtil.optionsBeanToOptions(objectSetSpecification.getSearchOptionsBean()),
                objectSetSpecification.isUseRepositoryDirectly());
    }

    private static @NotNull ObjectQuery createObjectQuery(
            @NotNull Class<? extends Containerable> containerType,
            @NotNull RepositoryObjectSetSpecificationImpl objectSetSpecification)
            throws SchemaException, ConfigurationException {

        PrismContext prismContext = PrismContext.get();
        ObjectQuery bareQuery = ObjectQueryUtil.emptyIfNull(
                prismContext.getQueryConverter().createObjectQuery(
                        containerType, objectSetSpecification.getQueryBean()));
        var archetypeOid = objectSetSpecification.getArchetypeOid();
        if (archetypeOid != null) {
            return ObjectQueryUtil.addConjunctions(bareQuery, prismContext.queryFor(containerType)
                    .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(archetypeOid)
                    .buildFilter());
        } else {
            return bareQuery;
        }
    }

    private static @NotNull Class<?> determineContainerType(@NotNull ObjectSetSpecification set) {
        return getTypeFromName(
                MoreObjects.firstNonNull(
                        set.getObjectType(), ObjectType.COMPLEX_TYPE));
    }

    private static @NotNull Class<?> getTypeFromName(@NotNull QName typeName) {
        Class<?> targetTypeClass = ObjectTypes.getObjectTypeClassIfKnown(typeName);
        if (targetTypeClass == null) {
            PrismContainerDefinition<Containerable> def = PrismContext.get().getSchemaRegistry().findContainerDefinitionByType(typeName);
            if (def == null) {
                throw new IllegalArgumentException("Unsupported container type " + typeName);
            }
            targetTypeClass = def.getTypeClass();
        }
        return targetTypeClass;
    }

    public @NotNull Class<C> getType() {
        return type;
    }

    public @Nullable ObjectQuery getQuery() {
        return query;
    }

    public void setQuery(@Nullable ObjectQuery query) {
        this.query = query;
    }

    public void addFilter(ObjectFilter filter) {
        setQuery(
                ObjectQueryUtil.addConjunctions(query, filter));
    }

    public @Nullable Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return searchOptions;
    }

    public void setSearchOptions(@Nullable Collection<SelectorOptions<GetOperationOptions>> searchOptions) {
        this.searchOptions = searchOptions;
    }

    Boolean getUseRepository() {
        return useRepository;
    }

    public boolean isUseRepository() {
        return Boolean.TRUE.equals(getUseRepository());
    }

    void setUseRepository(@Nullable Boolean useRepository) {
        this.useRepository = useRepository;
    }

    boolean concernsShadows() {
        return ShadowType.class.equals(requireNonNull(type));
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

    void setNoFetchOption() {
        searchOptions = GetOperationOptions.updateToNoFetch(searchOptions);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "type", type, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "query", query, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "searchOptions", searchOptions, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "useRepository", useRepository, indent + 1);
        return sb.toString();
    }

    @SuppressWarnings({ "MethodDoesntCallSuperMethod" })
    @Override
    public SearchSpecification<C> clone() {
        return new SearchSpecification<>(this);
    }
}
