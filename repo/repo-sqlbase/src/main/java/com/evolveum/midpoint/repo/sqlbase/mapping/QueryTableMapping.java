/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.xml.namespace.QName;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.sql.ColumnMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * Common supertype for mapping items/attributes between schema (prism) classes and query types.
 * See {@link #addItemMapping(QName, ItemSqlMapper)} for details about mapping mechanism.
 * See {@link #addDetailFetchMapper(ItemName, SqlDetailFetchMapper)} for more about mapping
 * related to-many detail tables.
 *
 * The main goal of this type is to map object query conditions and ORDER BY to SQL.
 * Transformation between schema/prism objects and repository objects (row beans or tuples) is
 * delegated to {@link SqlTransformer}.
 * Objects of various {@link QueryTableMapping} subclasses are factories for the transformer.
 *
 * Other important functions of mapping:
 *
 * * It allows creating "aliases" (entity path instances) {@link #newAlias(String)}.
 * * It knows how to traverse to other related entities, defined by {@link #addRelationResolver}
 *
 * TODO: possible future use is to help with item delta applications (objectModify)
 *
 * @param <S> schema type
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public abstract class QueryTableMapping<S, Q extends FlexibleRelationalPathBase<R>, R>
        implements QueryModelMapping {

    private final String tableName;
    private final String defaultAliasName;
    private final Class<S> schemaType;
    private final Class<Q> queryType;

    /**
     * Extension columns, key = propertyName which may differ from ColumnMetadata.getName().
     */
    private final Map<String, ColumnMetadata> extensionColumns = new LinkedHashMap<>();

    private final Map<QName, ItemSqlMapper> itemFilterProcessorMapping = new LinkedHashMap<>();
    private final Map<QName, ItemRelationResolver<Q>> itemRelationResolvers = new HashMap<>();
    private final Map<QName, SqlDetailFetchMapper<R, ?, ?, ?>> detailFetchMappers = new HashMap<>();

    private Q defaultAlias;

    /**
     * Creates metamodel for the table described by designated type (Q-class) related to schema type.
     * Allows registration of any number of columns - typically used for static properties
     * (non-extensions).
     *
     * @param tableName database table name
     * @param defaultAliasName default alias name, some short abbreviation, must be unique
     * across mapped types
     */
    protected QueryTableMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType) {
        this.tableName = tableName;
        this.defaultAliasName = defaultAliasName;
        this.schemaType = schemaType;
        this.queryType = queryType;
    }

    /**
     * Adds information how item (attribute) from schema type is mapped to query,
     * especially for condition creating purposes.
     * This is not usable for complex item path resolution,
     * see {@link #addRelationResolver(ItemName, ItemRelationResolver)} for that purpose.
     * <p>
     * The {@link ItemSqlMapper} works as a factory for {@link FilterProcessor} that can process
     * {@link ObjectFilter} related to the {@link ItemName} specified as the first parameter.
     * It is not possible to use filter processor directly because at the time of mapping
     * specification we don't have the actual query path representing the entity or the column.
     * These paths are non-static properties of query class instances.
     * <p>
     * The {@link ItemSqlMapper} also provides so called "primary mapping" to a column for ORDER BY
     * part of the filter.
     * But there can be additional column mappings specified as for some types (e.g. poly-strings)
     * there may be other than 1-to-1 mapping.
     * <p>
     * Construction of the {@link ItemSqlMapper} is typically simplified by static methods
     * {@code #mapper()} provided on various {@code *ItemFilterProcessor} classes.
     * This works as a "processor factory factory" and makes table mapping specification simpler.
     *
     * @param itemName item name from schema type (see {@code F_*} constants on schema types)
     * @param itemMapper mapper wrapping the information about column mappings working also
     * as a factory for {@link FilterProcessor}
     */
    public final void addItemMapping(QName itemName, ItemSqlMapper itemMapper) {
        itemFilterProcessorMapping.put(itemName, itemMapper);
    }

    /**
     * Adds information how {@link ItemName} (attribute) from schema type is to be resolved
     * when it appears as a component of a complex (non-single) {@link ItemPath}.
     * This is in contrast with "item mapping" that is used for single (or last) component
     * of the item path and helps with query interpretation.
     */
    public final void addRelationResolver(
            @NotNull ItemName itemName,
            @NotNull ItemRelationResolver<Q> itemRelationResolver) {
        itemRelationResolvers.put(itemName, itemRelationResolver);
    }

    /**
     * Fetcher/mappers for detail tables take care of loading to-many details related to
     * this mapped entity (master).
     * One fetcher per detail type/table is registered under the related item name.
     *
     * @param itemName item name from schema type that is mapped to detail table in the repository
     * @param detailFetchMapper fetcher-mapper that handles loading of details
     * @see SqlDetailFetchMapper
     */
    public final void addDetailFetchMapper(
            ItemName itemName,
            SqlDetailFetchMapper<R, ?, ?, ?> detailFetchMapper) {
        detailFetchMappers.put(itemName, detailFetchMapper);
    }

    /**
     * Lambda "wrapper" that helps with type inference when mapping paths from entity path.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected <A extends Path<?>> Function<EntityPath<?>, A> path(
            Function<Q, A> rootToQueryItem) {
        return (Function) rootToQueryItem;
    }

    /**
     * Lambda "wrapper" but this time with explicit query type (otherwise unused by the method)
     * for paths not starting on the Q parameter used for this mapping instance.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected <OQ extends EntityPath<OR>, OR, A extends Path<?>> Function<EntityPath<?>, A> path(
            @SuppressWarnings("unused") Class<OQ> queryType,
            Function<OQ, A> entityToQueryItem) {
        return (Function) entityToQueryItem;
    }

    /**
     * Lambda "wrapper" that helps with the type inference (namely the current Q type).
     * Returned bi-function returns {@code ON} condition predicate for two entity paths.
     */
    protected <DQ extends EntityPath<DR>, DR> BiFunction<Q, DQ, Predicate> joinOn(
            BiFunction<Q, DQ, Predicate> joinOnPredicateFunction) {
        return joinOnPredicateFunction;
    }

    public final @Nullable Path<?> primarySqlPath(
            ItemName itemName, SqlQueryContext<?, ?, ?> context)
            throws QueryException {
        return itemMapper(itemName).itemPrimaryPath(context.path());
    }

    @Override
    public final @NotNull ItemSqlMapper itemMapper(QName itemName) throws QueryException {
        ItemSqlMapper itemMapping = QNameUtil.getByQName(itemFilterProcessorMapping, itemName);
        if (itemMapping == null) {
            throw new QueryException("Missing item mapping for " + itemName
                    + " in mapping " + getClass().getSimpleName());
        }
        return itemMapping;
    }

    @Override
    public final @NotNull ItemRelationResolver<?> relationResolver(ItemName itemName)
            throws QueryException {
        ItemRelationResolver<Q> resolver = QNameUtil.getByQName(itemRelationResolvers, itemName);
        if (resolver == null) {
            throw new QueryException("Missing relation resolver for " + itemName
                    + " in mapping " + getClass().getSimpleName());
        }
        return resolver;
    }

    public String tableName() {
        return tableName;
    }

    public String defaultAliasName() {
        return defaultAliasName;
    }

    /**
     * This refers to midPoint schema, not DB schema.
     */
    public Class<S> schemaType() {
        return schemaType;
    }

    public Class<Q> queryType() {
        return queryType;
    }

    /**
     * Creates new alias (entity path instance) with a defined name.
     * You can also use {@link #defaultAlias()} if one alias in a query is enough.
     * Entity path instance returned by this call is already enhanced by extension columns.
     */
    public Q newAlias(String alias) {
        Q entityPath = newAliasInstance(alias);
        for (Map.Entry<String, ColumnMetadata> entry : extensionColumns.entrySet()) {
            String propertyName = entry.getKey();
            ColumnMetadata columnMetadata = entry.getValue();
            // TODO any treatment of different types should be here, now String path is implied
            entityPath.createString(propertyName, columnMetadata);
        }
        return entityPath;
    }

    /**
     * Method returning new instance of {@link EntityPath} - to be implemented by sub-mapping.
     * This will create entity path without any extension columns, see {@link #newAlias} for that.
     */
    protected abstract Q newAliasInstance(String alias);

    public synchronized Q defaultAlias() {
        if (defaultAlias == null) {
            defaultAlias = newAlias(defaultAliasName);
        }
        return defaultAlias;
    }

    /**
     * Creates {@link SqlTransformer} of row bean to schema type, override if provided.
     */
    public SqlTransformer<S, Q, R> createTransformer(
            SqlTransformerContext sqlTransformerContext) {
        throw new UnsupportedOperationException("Bean transformer not supported for " + queryType);
    }

    /**
     * Returns collection of all registered {@link SqlDetailFetchMapper}s.
     */
    public final Collection<SqlDetailFetchMapper<R, ?, ?, ?>> detailFetchMappers() {
        return detailFetchMappers.values();
    }

    /**
     * Returns {@link SqlDetailFetchMapper} registered for the specified {@link ItemName}.
     */
    public final SqlDetailFetchMapper<R, ?, ?, ?> detailFetchMapper(ItemName itemName)
            throws QueryException {
        SqlDetailFetchMapper<R, ?, ?, ?> mapper =
                QNameUtil.getByQName(detailFetchMappers, itemName);
        if (mapper == null) {
            throw new QueryException("Missing detail fetch mapping for " + itemName
                    + " in mapping " + getClass().getSimpleName());
        }
        return mapper;
    }

    /**
     * Registers extension columns. At this moment all are treated as strings.
     */
    public synchronized void addExtensionColumn(
            String propertyName, ColumnMetadata columnMetadata) {
        extensionColumns.put(propertyName, columnMetadata);
        // to assure that the next defaultAlias() provides it with current extension columns
        defaultAlias = null;
    }

    public Map<String, ColumnMetadata> getExtensionColumns() {
        return Collections.unmodifiableMap(extensionColumns);
    }

    /**
     * By default uses {@link #selectExpressionsWithCustomColumns} and does not use options.
     * Can be overridden to fulfil other needs, e.g. to select just full object..
     */
    public @NotNull Path<?>[] selectExpressions(
            Q entity,
            @SuppressWarnings("unused") Collection<SelectorOptions<GetOperationOptions>> options) {
        return selectExpressionsWithCustomColumns(entity);
    }

    public @NotNull Path<?>[] selectExpressionsWithCustomColumns(Q entity) {
        List<Path<?>> expressions = new ArrayList<>();
        expressions.add(entity);
        for (String extensionPropertyName : extensionColumns.keySet()) {
            expressions.add(entity.getPath(extensionPropertyName));
        }
        return expressions.toArray(new Path[0]);
    }

    @Override
    public String toString() {
        return "QueryModelMapping{" +
                "tableName='" + tableName + '\'' +
                ", defaultAliasName='" + defaultAliasName + '\'' +
                ", schemaType=" + schemaType +
                ", queryType=" + queryType +
                '}';
    }

    public R newRowObject() {
        throw new UnsupportedOperationException(
                "Row bean creation not implemented for query type " + queryType.getName());
    }
}
