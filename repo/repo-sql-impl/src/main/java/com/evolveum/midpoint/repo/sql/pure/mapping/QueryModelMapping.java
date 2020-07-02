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
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * Common supertype for mapping between Q-classes and model (prism) classes.
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

    // Is one column enough? For polystring we will have to map two anyway...
//    private final Map<QName, ColumnMetadata> itemToColumn = new LinkedHashMap<>();

    private final Map<QName, ItemMapping> itemFilterProcessorMapping = new LinkedHashMap<>();

    // E=entity, A=attribute
    private static class ItemMapping<E extends EntityPath<?>, A> {
        public final Function<E, Path<A>> rootToItem;
        public final Function<Path<A>, FilterProcessor<?>> processorFactory;

        private ItemMapping(
                Function<E, Path<A>> rootToItem,
                Function<Path<A>, FilterProcessor<?>> processorFactory) {
            this.rootToItem = rootToItem;
            this.processorFactory = processorFactory;
        }

        public FilterProcessor<?> createFilterProcessor(Path<?> entityPath) {
            //noinspection unchecked
            Path<A> itemPath = rootToItem.apply((E) entityPath);
            return processorFactory.apply(itemPath);
        }
    }

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

    public <A> void addItemMapping(
            ItemName itemName,
            Function<Q, Path<A>> rootToQueryItem,
            Function<Path<A>, FilterProcessor<?>> processorFactory) {
        itemFilterProcessorMapping.put(itemName, new ItemMapping<>(rootToQueryItem, processorFactory));
    }

    // we want loose typing for client's sake, there is no other chance to get the right type here
    public <T extends ObjectFilter> @NotNull FilterProcessor<T> getFilterProcessor(
            ItemName itemName, Path<?> entityPath)
            throws QueryException {
        ItemMapping<?, ?> itemMapping = QNameUtil.getByQName(itemFilterProcessorMapping, itemName);
        if (itemMapping == null) {
            throw new QueryException("Missing mapping for " + itemName
                    + " in mapping " + getClass().getSimpleName());
        }

        //noinspection unchecked
        return (FilterProcessor<T>) itemMapping.createFilterProcessor(entityPath);
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

    public Q newAlias(String alias) {
        try {
            return queryType.getConstructor(String.class).newInstance(alias);
        } catch (ReflectiveOperationException e) {
            // TODO MID-6319
            throw new RuntimeException(e);
        }
    }

    public synchronized Q defaultAlias() {
        if (defaultAlias == null) {
            defaultAlias = newAlias(defaultAliasName);
        }
        return defaultAlias;
    }

    // TODO extension columns + null default alias after every change - synchronized!
}
