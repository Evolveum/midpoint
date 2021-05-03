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

import com.querydsl.core.Tuple;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.PolyStringItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SimpleItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.TimestampItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Common supertype for mapping items/attributes between schema (prism) classes and tables.
 * See {@link #addItemMapping(QName, ItemSqlMapper)} for details about mapping mechanism.
 * See {@link #addDetailFetchMapper(ItemName, SqlDetailFetchMapper)} for more about mapping
 * related to-many detail tables.
 *
 * The main goal of this type is to map object query conditions and ORDER BY to SQL.
 * Mappings also takes care of transformation between schema/prism objects and repository objects
 * (row beans or tuples).
 *
 * Other important functions of mapping:
 *
 * * It allows creating "aliases" (entity path instances) {@link #newAlias(String)}.
 * * It knows how to traverse to other related entities, defined by {@link #addRelationResolver}
 *
 * Mapper factory methods like {@link #stringMapper} return mappers that may or may not be bound
 * to the same schema type because of the nested mappings.
 * E.g. attribute `name` is part of the `S` object, but `metadata/createChannel` is based on nested
 * mapping for `metadata` for which `S` is `MetadataType`.
 * That's why these methods have flexible schema type parameter.
 * Because such nested mapping still uses the same table, types `Q` and `R` remain the same.
 *
 * @param <S> schema type
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public abstract class QueryTableMapping<S, Q extends FlexibleRelationalPathBase<R>, R>
        extends QueryModelMapping<S, Q, R> {

    private final String tableName;
    private final String defaultAliasName;
    private final SqlRepoContext repositoryContext;

    /**
     * Extension columns, key = propertyName which may differ from ColumnMetadata.getName().
     */
    private final Map<String, ColumnMetadata> extensionColumns = new LinkedHashMap<>();

    private final Map<QName, SqlDetailFetchMapper<R, ?, ?, ?>> detailFetchMappers = new HashMap<>();

    /** Instantiated lazily as needed, see {@link #defaultAlias()}. */
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
            @NotNull Class<Q> queryType,
            @NotNull SqlRepoContext repositoryContext) {
        super(schemaType, queryType);
        this.tableName = tableName;
        this.defaultAliasName = defaultAliasName;
        this.repositoryContext = repositoryContext;
    }

    /**
     * Creates metamodel for the table described by designated type (Q-class) related to schema type.
     * Allows registration of any number of columns - typically used for static properties
     * (non-extensions).
     *
     * @param tableName database table name
     * @param defaultAliasName default alias name, some short abbreviation, must be unique
     * across mapped types
     */
    @Deprecated
    protected QueryTableMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType) {
        super(schemaType, queryType);
        this.tableName = tableName;
        this.defaultAliasName = defaultAliasName;

        // TODO remove this constructor, use the one above only
        this.repositoryContext = null;
    }

    /**
     * Returns the mapper creating the string filter processor from context.
     *
     * @param <MS> mapped schema type, see javadoc for the class
     */
    protected <MS> ItemSqlMapper<MS, Q, R> stringMapper(
            Function<Q, StringPath> rootToQueryItem) {
        return new ItemSqlMapper<>(
                ctx -> new SimpleItemFilterProcessor<>(ctx, rootToQueryItem),
                rootToQueryItem);
    }

    /**
     * Returns the mapper creating the integer filter processor from context.
     *
     * @param <MS> mapped schema type, see javadoc for the class
     */
    public <MS> ItemSqlMapper<MS, Q, R> integerMapper(
            Function<Q, NumberPath<Integer>> rootToQueryItem) {
        return new ItemSqlMapper<>(ctx ->
                new SimpleItemFilterProcessor<>(ctx, rootToQueryItem), rootToQueryItem);
    }

    /**
     * Returns the mapper creating the long filter processor from context.
     *
     * @param <MS> mapped schema type, see javadoc for the class
     */
    public <MS> ItemSqlMapper<MS, Q, R> longMapper(
            Function<Q, NumberPath<Long>> rootToQueryItem) {
        return new ItemSqlMapper<>(ctx ->
                new SimpleItemFilterProcessor<>(ctx, rootToQueryItem), rootToQueryItem);
    }

    /**
     * Returns the mapper creating the boolean filter processor from context.
     *
     * @param <MS> mapped schema type, see javadoc for the class
     */
    protected <MS> ItemSqlMapper<MS, Q, R> booleanMapper(
            Function<Q, BooleanPath> rootToQueryItem) {
        return new ItemSqlMapper<>(ctx ->
                new SimpleItemFilterProcessor<>(ctx, rootToQueryItem), rootToQueryItem);
    }

    /**
     * Returns the mapper creating the OID (UUID) filter processor from context.
     *
     * @param <MS> mapped schema type, see javadoc for the class
     */
    protected <MS> ItemSqlMapper<MS, Q, R> uuidMapper(
            Function<Q, UuidPath> rootToQueryItem) {
        return new ItemSqlMapper<>(ctx ->
                new SimpleItemFilterProcessor<>(ctx, rootToQueryItem), rootToQueryItem);
    }

    /**
     * Returns the mapper function creating the timestamp filter processor from context.
     *
     * @param <MS> mapped schema type, see javadoc for the class
     * @param <T> actual data type of the query path storing the timestamp
     */
    protected <MS, T extends Comparable<T>> ItemSqlMapper<MS, Q, R> timestampMapper(
            Function<Q, DateTimePath<T>> rootToQueryItem) {
        return new ItemSqlMapper<>(context ->
                new TimestampItemFilterProcessor<>(context, rootToQueryItem), rootToQueryItem);
    }

    /**
     * Returns the mapper creating the string filter processor from context.
     *
     * @param <MS> mapped schema type, see javadoc for the class
     */
    protected <MS> ItemSqlMapper<MS, Q, R> polyStringMapper(
            Function<Q, StringPath> origMapping,
            Function<Q, StringPath> normMapping) {
        return new ItemSqlMapper<>(ctx ->
                new PolyStringItemFilterProcessor(ctx, origMapping, normMapping), origMapping);
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
     * Lambda "wrapper" that helps with the type inference (namely the current Q type).
     * Returned bi-function returns {@code ON} condition predicate for two entity paths.
     *
     * @param <TQ> query type for the JOINed (target) table
     * @param <TR> row type related to the {@link TQ}
     */
    protected <TQ extends FlexibleRelationalPathBase<TR>, TR> BiFunction<Q, TQ, Predicate> joinOn(
            BiFunction<Q, TQ, Predicate> joinOnPredicateFunction) {
        return joinOnPredicateFunction;
    }

    public final @Nullable Path<?> primarySqlPath(
            ItemName itemName, SqlQueryContext<S, Q, R> context)
            throws RepositoryException {
        return itemMapper(itemName).itemPrimaryPath(context.path());
    }

    public String tableName() {
        return tableName;
    }

    public String defaultAliasName() {
        return defaultAliasName;
    }

    public SqlRepoContext repositoryContext() {
        return repositoryContext != null
                ? repositoryContext
                // TODO remove this branch
                : SqlRepoContext.getInstance();
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

    /**
     * Returns default alias - use only once per query, e.g. not for two different joins.
     * Also, don't cache it yourself, always use this method which ensures that the alias has
     * all the extension columns configured properly.
     */
    public synchronized Q defaultAlias() {
        if (defaultAlias == null) {
            defaultAlias = newAlias(defaultAliasName);
        }
        return defaultAlias;
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

    public R newRowObject() {
        throw new UnsupportedOperationException(
                "Row bean creation not implemented for query type " + queryType().getName());
    }

    /**
     * Transforms row of {@link R} type to schema type {@link S}.
     * If pre-generated bean is used as row it does not include extension (dynamic) columns,
     * which is OK if extension columns are used only for query and their information
     * is still contained in the object somehow else (e.g. full object LOB).
     *
     * Alternative would be dynamically generated list of select expressions and transforming
     * row to M object directly from {@link com.querydsl.core.Tuple}.
     */
    public abstract S toSchemaObject(R row) throws SchemaException;

    /**
     * Transforms row Tuple containing attributes of {@link R} to schema type {@link S}.
     * Entity path can be used to access tuple elements.
     * This allows loading also dynamically defined columns (like extensions).
     */
    public abstract S toSchemaObject(Tuple row, Q entityPath,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException;

    public S toSchemaObjectSafe(Tuple tuple, Q entityPath,
            Collection<SelectorOptions<GetOperationOptions>> options) {
        try {
            return toSchemaObject(tuple, entityPath, options);
        } catch (SchemaException e) {
            throw new RepositoryMappingException(e);
        }
    }

    @Override
    public String toString() {
        return "QueryTableMapping{" +
                "tableName='" + tableName + '\'' +
                ", defaultAliasName='" + defaultAliasName + '\'' +
                ", schemaType=" + schemaType() +
                ", queryType=" + queryType() +
                '}';
    }
}
