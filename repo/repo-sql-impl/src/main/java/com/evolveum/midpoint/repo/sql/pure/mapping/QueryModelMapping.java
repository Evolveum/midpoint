package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import javax.xml.namespace.QName;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.sql.ColumnMetadata;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.pure.FilterProcessor;
import com.evolveum.midpoint.repo.sql.pure.SqlPathContext;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * Common supertype for mapping between Q-classes and model (prism) classes.
 * See {@link #addItemMapping(ItemName, Function)} for details about mapping mechanism.
 * <p>
 * Goals:
 * <ul>
 *     <li>Map object query conditions to SQL.</li>
 * </ul>
 * <p>
 * Non-goals:
 * <ul>
 *     <li>Map objects from Q-type to prism and back.
 *     This is done by code, possibly static method from a DTO "assembler" class.</li>
 * </ul>
 */
public abstract class QueryModelMapping<M, Q extends EntityPath<?>> {

    private final String tableName;
    private final String defaultAliasName;
    private final Class<M> modelType;
    private final Class<Q> queryType;

    private final Map<String, ColumnMetadata> columns = new LinkedHashMap<>();

    private final Map<QName, Function<SqlPathContext, FilterProcessor<?>>>
            itemFilterProcessorMapping = new LinkedHashMap<>();

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

    public QueryModelMapping<M, Q> add(ColumnMetadata column) {
        columns.put(column.getName(), column);
        return this;
    }

    /**
     * Adds information how item (attribute) from model type is mapped to query,
     * especially for condition creating purposes.
     * <p>
     * The {@code processorFactory} function must provide {@link FilterProcessor} that can process
     * {@link ObjectFilter} related to the {@link ItemName} specified as the first parameter.
     * It must be "factory function" because at the time of mapping specification
     * we don't have the actual query path representing the entity or the column.
     * These paths are non-static properties of query class instances.
     * <p>
     * The actually provided {@code processorFactory} function must also somehow "wrap" the
     * information how the query path representing entity translates to the column that represents
     * the specified {@code itemName} from the model.
     * See usages how this is typically done - this may vary by column types and for some types
     * (e.g. poly-strings) there may be other than 1-to-1 mapping.
     * <p>
     * Creation of {@code processorFactory} is typically simplified by static methods
     * {@code #mapper()} provided on various {@code *ItemFilterProcessor} classes.
     * This works as a "factory function factory" (or "processor factory factory", if you will).
     *
     * @param itemName item name from schema type (see {@code F_*} constants on schema types)
     * @param processorFactory function creating {@link FilterProcessor} for the given itemName
     * using the current {@link SqlPathContext}
     */
    public void addItemMapping(
            ItemName itemName,
            Function<SqlPathContext, FilterProcessor<?>> processorFactory) {
        itemFilterProcessorMapping.put(itemName, processorFactory);
    }

    /**
     * Helping lambda "wrapper" that helps with type inference when mapping paths from entity path.
     * The returned types are ambiguous just as they are used in {@code mapper()} static methods
     * on item filter processors.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected <A> Function<EntityPath<?>, Path<?>> path(
            Function<Q, Path<A>> rootToQueryItem) {
        return (Function) rootToQueryItem;
    }

    // we want loose typing for client's sake, there is no other chance to get the right type here
    public <T extends ObjectFilter> @NotNull FilterProcessor<T> getFilterProcessor(
            ItemName itemName, SqlPathContext context)
            throws QueryException {
        Function<SqlPathContext, FilterProcessor<?>> itemMapping =
                QNameUtil.getByQName(itemFilterProcessorMapping, itemName);
        if (itemMapping == null) {
            throw new QueryException("Missing mapping for " + itemName
                    + " in mapping " + getClass().getSimpleName());
        }

        //noinspection unchecked
        return (FilterProcessor<T>) itemMapping.apply(context);
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

    // TODO extension columns + null default alias after every change - synchronized!
}
