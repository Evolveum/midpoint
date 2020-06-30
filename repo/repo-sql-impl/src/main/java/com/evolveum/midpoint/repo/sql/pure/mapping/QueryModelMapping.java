package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import javax.xml.namespace.QName;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.sql.ColumnMetadata;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.pure.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditEventRecord;

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
public abstract class QueryModelMapping<M, Q extends FlexibleRelationalPathBase<?>> {

    private final String tableName;
    private final String defaultAliasName;
    private final Class<M> modelType;
    private final Class<Q> queryType;

    private final Map<String, ColumnMetadata> columns = new LinkedHashMap<>();
    private final Map<QName, ColumnMetadata> itemPathToColumn = new LinkedHashMap<>();

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

    protected void addItemProcessor(ItemName itemName, ColumnMetadata column, Function<Q, Path<?>> queryPathFunction) {
        // TODO
    }

    // TODO extension columns + null default alias after every change - synchronized!
}
