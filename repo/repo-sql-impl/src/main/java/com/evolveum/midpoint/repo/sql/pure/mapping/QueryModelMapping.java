package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.util.LinkedHashMap;
import java.util.Map;

import com.querydsl.sql.ColumnMetadata;
import org.jetbrains.annotations.NotNull;

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
public abstract class QueryModelMapping<M, Q> {

    private final String tableName;
    private final Class<M> modelType;
    private final Class<Q> queryType;

    private final Map<String, ColumnMetadata> columns = new LinkedHashMap<>();

    /**
     * Creates metamodel for the table described by designated type (Q-class) related to model type.
     * Allows registration of any number of columns - typically used for static properties
     * (non-extensions).
     */
    protected QueryModelMapping(
            @NotNull String tableName,
            @NotNull Class<M> modelType,
            @NotNull Class<Q> queryType,
            ColumnMetadata... columns) {
        this.tableName = tableName;
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

    public String tableName() {
        return tableName;
    }

    public Class<M> modelType() {
        return modelType;
    }

    public Class<Q> queryType() {
        return queryType;
    }
}
