package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.xml.namespace.QName;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.sql.ColumnMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.pure.FilterProcessor;
import com.evolveum.midpoint.repo.sql.pure.SqlPathContext;
import com.evolveum.midpoint.repo.sql.pure.SqlTransformer;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * Common supertype for mapping between Q-classes and model (prism) classes.
 * See {@link #addItemMapping(ItemName, ItemSqlMapper)} for details about mapping mechanism.
 * <p>
 * Goals:
 * <ul>
 *     <li>Map object query conditions and ORDER BY to SQL.</li>
 * </ul>
 * <p>
 * Non-goals:
 * <ul>
 *     <li>Map objects from Q-type to prism and back.
 *     This is done by code, possibly static method from a DTO "assembler" class.</li>
 * </ul>
 *
 * @param <M> model type
 * @param <Q> entity type (entity path or type R)
 * @param <R> row/bean type used by the Q-class
 */
public abstract class QueryModelMapping<M, Q extends EntityPath<R>, R> {

    private final String tableName;
    private final String defaultAliasName;
    private final Class<M> modelType;
    private final Class<Q> queryType;

    private final Map<String, ColumnMetadata> columns = new LinkedHashMap<>();

    private final Map<QName, ItemSqlMapper> itemFilterProcessorMapping = new LinkedHashMap<>();

    private Q defaultAlias;

    /**
     * Creates metamodel for the table described by designated type (Q-class) related to model type.
     * Allows registration of any number of columns - typically used for static properties
     * (non-extensions).
     *
     * @param tableName database table name
     * @param defaultAliasName default alias name, some short abbreviation, must be unique
     * across mapped types
     */
    protected QueryModelMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<M> modelType,
            @NotNull Class<Q> queryType,
            ColumnMetadata... columns) {
        this.tableName = tableName;
        this.defaultAliasName = defaultAliasName;
        this.modelType = modelType;
        this.queryType = queryType;
        for (ColumnMetadata column : columns) {
            this.columns.put(column.getName(), column);
        }
    }

    public QueryModelMapping<M, Q, R> add(ColumnMetadata column) {
        columns.put(column.getName(), column);
        return this;
    }

    /**
     * Adds information how item (attribute) from model type is mapped to query,
     * especially for condition creating purposes.
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
    public void addItemMapping(ItemName itemName, ItemSqlMapper itemMapper) {
        itemFilterProcessorMapping.put(itemName, itemMapper);
    }

    /**
     * Lambda "wrapper" that helps with type inference when mapping paths from entity path.
     * The returned types are ambiguous just as they are used in {@code mapper()} static methods
     * on item filter processors.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected <A> Function<EntityPath<?>, Path<?>> path(
            Function<Q, Path<A>> rootToQueryItem) {
        return (Function) rootToQueryItem;
    }

    /**
     * Lambda "wrapper" but this time with explicit query type (otherwise unused by the method)
     * for paths not starting on the Q parameter used for this mapping instance.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected <OQ extends EntityPath<OR>, OR, A> Function<EntityPath<?>, Path<?>> path(
            @SuppressWarnings("unused") Class<OQ> queryType,
            Function<OQ, Path<A>> entityToQueryItem) {
        return (Function) entityToQueryItem;
    }

    /**
     * Helping lambda "wrapper" that helps with the type inference (namely the current Q type).
     */
    protected <DQ extends EntityPath<DR>, DR> BiFunction<Q, DQ, Predicate> joinOn(
            BiFunction<Q, DQ, Predicate> joinOnPredicateFunction) {
        return joinOnPredicateFunction;
    }

    // we want loose typing for client's sake, there is no other chance to get the right type here
    public <T extends ObjectFilter> @NotNull FilterProcessor<T> createItemFilterProcessor(
            ItemName itemName, SqlPathContext<?, ?> context)
            throws QueryException {
        return itemMapping(itemName).createFilterProcessor(context);
    }

    public @Nullable Path<?> primarySqlPath(ItemName itemName, SqlPathContext<?, ?> context)
            throws QueryException {
        return itemMapping(itemName).itemPath(context.path());
    }

    @NotNull
    public ItemSqlMapper itemMapping(ItemName itemName) throws QueryException {
        ItemSqlMapper itemMapping = QNameUtil.getByQName(itemFilterProcessorMapping, itemName);
        if (itemMapping == null) {
            throw new QueryException("Missing mapping for " + itemName
                    + " in mapping " + getClass().getSimpleName());
        }
        return itemMapping;
    }

    public String tableName() {
        return tableName;
    }

    public String defaultAliasName() {
        return defaultAliasName;
    }

    public Class<M> modelType() {
        return modelType;
    }

    public Class<Q> queryType() {
        return queryType;
    }

    public Q newAlias(String alias) throws QueryException {
        try {
            return queryType.getConstructor(String.class).newInstance(alias);
        } catch (ReflectiveOperationException e) {
            throw new QueryException("Invalid constructor for type " + queryType, e);
        }
    }

    public synchronized Q defaultAlias() throws QueryException {
        if (defaultAlias == null) {
            defaultAlias = newAlias(defaultAliasName);
        }
        return defaultAlias;
    }

    /**
     * Creates {@link SqlTransformer} of row bean to model/schema type.
     */
    public abstract SqlTransformer<M, R> createTransformer(PrismContext prismContext);

    /**
     * Returns collection of {@link SqlDetailFetchMapper}s that know how to fetch
     * to-many details related to this mapped entity (master) - fetcher per detail type/table.
     */
    public Collection<SqlDetailFetchMapper<R, ?, ?, ?>> detailFetchMappers() {
        return Collections.emptyList();
    }

    // TODO extension columns + null default alias after every change - synchronized!
}
