/*
 * Copyright (C) 2010-2022 Evolveum and contributors
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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.PolyStringItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SimpleItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.TimestampItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Common supertype for mapping items/attributes between schema (prism) classes and tables.
 * See {@link #addItemMapping(QName, ItemSqlMapper)} for details about mapping mechanism.
 * See {@link #createRowTransformer(SqlQueryContext, JdbcSession, Collection)} for more about mapping
 * related to-many detail tables.
 *
 * The main goal of this type is to map object query conditions and ORDER BY to SQL.
 * Mappings also takes care of transformation between schema/prism objects and repository objects
 * (row beans or tuples).
 * Any logic specific for the particular type (table) should appear in the related mapping subclass.
 * This applies for filtering, fetching (e.g. additional detail tables) and transforming to midPoint
 * objets.
 * See also Javadoc in {@link SqlQueryContext} for additional info.
 *
 * Other important functions of mapping:
 *
 * * It allows creating "aliases" (entity path instances) {@link #newAlias(String)}.
 * * It knows how to traverse to other related entities, defined by {@link #addRelationResolver}
 *
 * Mapping for tables is initialized once and requires {@link SqlRepoContext}.
 * The mapping is accessible by static `get()` method; if multiple mapping instances exist
 * for the same type the method uses suffix to differentiate them.
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
     * Returns the mapper creating the string filter processor from context.
     */
    protected ItemSqlMapper<Q, R> stringMapper(
            Function<Q, StringPath> rootToQueryItem) {
        return new DefaultItemSqlMapper<>(
                ctx -> new SimpleItemFilterProcessor<>(ctx, rootToQueryItem),
                rootToQueryItem);
    }

    /**
     * Returns the mapper creating the integer filter processor from context.
     */
    public ItemSqlMapper<Q, R> integerMapper(
            Function<Q, NumberPath<Integer>> rootToQueryItem) {
        return new DefaultItemSqlMapper<>(ctx ->
                new SimpleItemFilterProcessor<>(ctx, rootToQueryItem), rootToQueryItem);
    }

    /**
     * Returns the mapper creating the long filter processor from context.
     */
    public ItemSqlMapper<Q, R> longMapper(
            Function<Q, NumberPath<Long>> rootToQueryItem) {
        return new DefaultItemSqlMapper<>(ctx ->
                new SimpleItemFilterProcessor<>(ctx, rootToQueryItem), rootToQueryItem);
    }

    /**
     * Returns the mapper creating the boolean filter processor from context.
     */
    protected ItemSqlMapper<Q, R> booleanMapper(
            Function<Q, BooleanPath> rootToQueryItem) {
        return new DefaultItemSqlMapper<>(ctx ->
                new SimpleItemFilterProcessor<>(ctx, rootToQueryItem), rootToQueryItem);
    }

    /**
     * Returns the mapper creating the OID (UUID) filter processor from context.
     */
    protected ItemSqlMapper<Q, R> uuidMapper(
            Function<Q, UuidPath> rootToQueryItem) {
        return new DefaultItemSqlMapper<>(ctx ->
                new SimpleItemFilterProcessor<>(ctx, rootToQueryItem), rootToQueryItem);
    }

    /**
     * Returns the mapper function creating the timestamp filter processor from context.
     *
     * @param <T> actual data type of the query path storing the timestamp
     */
    protected <T extends Comparable<T>> ItemSqlMapper<Q, R> timestampMapper(
            Function<Q, DateTimePath<T>> rootToQueryItem) {
        return new DefaultItemSqlMapper<>(context ->
                new TimestampItemFilterProcessor<>(context, rootToQueryItem), rootToQueryItem);
    }

    /**
     * Returns the mapper creating the string filter processor from context.
     */
    protected ItemSqlMapper<Q, R> polyStringMapper(
            Function<Q, StringPath> origMapping,
            Function<Q, StringPath> normMapping) {
        return new DefaultItemSqlMapper<>(ctx ->
                new PolyStringItemFilterProcessor<>(ctx, origMapping, normMapping), origMapping);
    }

    /**
     * Fetcher/mappers for detail tables take care of loading to-many details related to
     * this mapped entity (master).
     * One fetcher per detail type/table is registered under the related item name.
     *
     * Used only in old-repo audit, we will let it die with that - but don't use in new stuff.
     * Use {@link #createRowTransformer(SqlQueryContext, JdbcSession, Collection)} mechanism instead.
     *
     * @param itemName item name from schema type that is mapped to detail table in the repository
     * @param detailFetchMapper fetcher-mapper that handles loading of details
     * @see SqlDetailFetchMapper
     */
    @Deprecated
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

    public String tableName() {
        return tableName;
    }

    public String defaultAliasName() {
        return defaultAliasName;
    }

    public SqlRepoContext repositoryContext() {
        return repositoryContext;
    }

    public PrismContext prismContext() {
        return repositoryContext.prismContext();
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
     *
     * Used only in old-repo audit, we will let it die with that - but don't use in new stuff.
     * Use {@link #createRowTransformer(SqlQueryContext, JdbcSession, Collection)} mechanism instead.
     */
    @Deprecated
    public final Collection<SqlDetailFetchMapper<R, ?, ?, ?>> detailFetchMappers() {
        return detailFetchMappers.values();
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
     * By default, uses {@link #selectExpressionsWithCustomColumns} and does not use options.
     * Can be overridden to fulfil other needs, e.g. to select just full object.
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
     * Alternative is to dynamically generate the list of select expressions reading directly from
     * the {@link com.querydsl.core.Tuple} - see {@link #toSchemaObject(Tuple, FlexibleRelationalPathBase, JdbcSession, Collection)}.
     */
    public abstract S toSchemaObject(R row) throws SchemaException;

    /**
     * Transforms row Tuple containing attributes of {@link R} to schema type {@link S}.
     * Entity path can be used to access tuple elements.
     * This allows loading also dynamically defined columns (like extensions).
     * This is what is used by default in {@link SqlQueryContext}.
     */
    public S toSchemaObject(
            @NotNull Tuple tuple,
            @NotNull Q entityPath,
            @NotNull JdbcSession jdbcSession,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException {
        throw new UnsupportedOperationException("Not implemented for " + getClass());
    }

    /**
     * Returns result transformer that by default calls
     * {@link #toSchemaObject(Tuple, FlexibleRelationalPathBase, JdbcSession, Collection)} for each result row.
     * This can be overridden, see {@link ResultListRowTransformer} javadoc for details.
     * This is useful for stateful transformers where the whole result can be pre-/post-processed as well.
     */
    public ResultListRowTransformer<S, Q, R> createRowTransformer(
            SqlQueryContext<S, Q, R> sqlQueryContext, JdbcSession jdbcSession, Collection<SelectorOptions<GetOperationOptions>> options) {
        return (tuple, entityPath) -> {
            try {
                return toSchemaObject(tuple, entityPath, jdbcSession, options);
            } catch (SchemaException e) {
                throw new RepositoryMappingException(e);
            }
        };
    }

    public Collection<SelectorOptions<GetOperationOptions>> updateGetOptions(
            Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications) {
        return options;
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

    protected static Path<?>[] paths(Path<?>... path) {
        return path;
    }
}
