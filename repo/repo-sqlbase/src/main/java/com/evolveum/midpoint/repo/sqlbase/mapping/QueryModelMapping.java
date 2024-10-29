/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemNameUtil;
import com.evolveum.midpoint.util.SingleLocalizableMessage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemValueFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * Common mapping functionality that covers the need for mapping from item paths
 * to table columns, but also to nested embedded mappings (e.g. metadata).
 *
 * This also works as implementation for nested mappings like `metadata` that contain attributes
 * resolved to the same query type - e.g. `metadata/createTimestamp`.
 * While `metadata` is resolved on the master mapping (for the query type representing table)
 * the nested `createTimestamp` is resolved by nested mapper implemented by this type.
 * Nested mapping can still contain relations, so {@link #addRelationResolver} is available.
 *
 * Supertype {@link QName} type is used instead of {@link ItemName} for registration keys to
 * support also technical paths like parent (..).
 *
 * @param <S> schema type
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public class QueryModelMapping<S, Q extends FlexibleRelationalPathBase<R>, R> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Class<S> schemaType;
    private final Class<Q> queryType;
    private final ItemDefinition<?> itemDefinition;

    private final Map<QName, ItemSqlMapper<Q, R>> itemMappings = new LinkedHashMap<>();
    private final Map<QName, ItemRelationResolver<Q, R, ?, ?>> itemRelationResolvers = new HashMap<>();

    public QueryModelMapping(
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType) {
        this.schemaType = schemaType;
        this.queryType = queryType;
        this.itemDefinition = PrismContext.get().getSchemaRegistry()
                .findItemDefinitionByCompileTimeClass(schemaType, ItemDefinition.class);
    }

    /** Returns schema type as class - refers to midPoint schema, not DB schema. */
    public Class<S> schemaType() {
        return schemaType;
    }

    public Class<Q> queryType() {
        return queryType;
    }

    /**
     * Adds information how item (attribute) from schema type is mapped to query,
     * especially for condition creating purposes.
     * This is not usable for complex item path resolution,
     * see {@link #addRelationResolver(QName, ItemRelationResolver)} for that purpose.
     *
     * The {@link ItemSqlMapper} works as a factory for {@link FilterProcessor} that can process
     * {@link ObjectFilter} related to the {@link QName} specified as the first parameter.
     * It is not possible to use filter processor directly because at the time of mapping
     * specification we don't have the actual query path representing the entity or the column.
     * These paths are non-static properties of query class instances.
     *
     * The {@link ItemSqlMapper} also provides so-called "primary mapping" to a column for ORDER BY
     * part of the filter.
     * But there can be additional column mappings specified as for some types (e.g. poly-strings)
     * there may be other than 1-to-1 mapping.
     *
     * Construction of the {@link ItemSqlMapper} is typically simplified by static methods
     * {@code #mapper()} provided on various {@code *ItemFilterProcessor} classes.
     * This works as a "processor factory-factory" and makes table mapping specification simpler.
     *
     * @param itemName item name from schema type (see {@code F_*} constants on schema types)
     * @param itemMapper mapper wrapping the information about column mappings working also
     * as a factory for {@link FilterProcessor}
     */
    public QueryModelMapping<S, Q, R> addItemMapping(
            @NotNull QName itemName,
            @NotNull ItemSqlMapper<Q, R> itemMapper) {
        itemMappings.put(itemName, itemMapper);
        return this;
    }

    /**
     * Adds information how {@link QName} (attribute) from schema type is to be resolved
     * when it appears as a component of a complex (non-single) {@link ItemPath}.
     * This is in contrast with "item mapping" that is used for single (or last) component
     * of the item path and helps with query interpretation.
     */
    public QueryModelMapping<S, Q, R> addRelationResolver(
            @NotNull QName itemName,
            @NotNull ItemRelationResolver<Q, R, ?, ?> itemRelationResolver) {
        itemRelationResolvers.put(itemName, itemRelationResolver);
        return this;
    }

    /**
     * Returns {@link ItemSqlMapper} for provided {@link QName} or throws.
     * This is later used to create {@link ItemValueFilterProcessor}.
     *
     * @throws QueryException if the mapper for the item is not found
     */
    public final @NotNull ItemSqlMapper<Q, R> itemMapper(QName itemName) throws QueryException {
        ItemSqlMapper<Q, R> itemMapping = getItemMapper(itemName);
        if (itemMapping == null) {
            String technicalMessage = "Missing item mapping for '" + itemName
                    + "' in mapping " + getClass().getSimpleName();
            SingleLocalizableMessage message = new SingleLocalizableMessage(
                    "QueryModelMapping.item.not.searchable",
                    new Object[]{itemName},
                    technicalMessage);
            throw new QueryException(message);
        }
        return itemMapping;
    }

    /**
     * Returns {@link ItemSqlMapper} for provided {@link QName} or `null`.
     */
    public @Nullable ItemSqlMapper<Q, R> getItemMapper(QName itemName) {
        return ItemNameUtil.getByQName(this.itemMappings, itemName);
    }

    /**
     * Returns {@link ItemRelationResolver} for the first component of the provided {@link ItemPath}
     * or throws if the resolver is not found.
     * Relation resolver helps with traversal over all-but-last components of item paths.
     * ItemPath is used instead of QName to encapsulate corner cases like parent segment.
     *
     * @param <TQ> type of target entity path
     * @param <TR> row type related to the target entity path {@link TQ}
     * @throws QueryException if the resolver for the item is not found
     */
    public final @NotNull <TQ extends FlexibleRelationalPathBase<TR>, TR>
    ItemRelationResolver<Q, R, TQ, TR> relationResolver(ItemPath path)
            throws QueryException {
        QName itemName = path.isEmpty()
                ? PrismConstants.T_SELF
                : ItemPath.isParent(path.first())
                ? PrismConstants.T_PARENT
                : ItemPath.isObjectReference(path.first())
                ? PrismConstants.T_OBJECT_REFERENCE
                : path.firstName();
        ItemRelationResolver<Q, R, TQ, TR> resolver = getRelationResolver(itemName);
        if (resolver == null) {
            var technicalMessage = "Missing relation resolver for '" + itemName
                    + "' in mapping " + getClass().getSimpleName();
            SingleLocalizableMessage message = new SingleLocalizableMessage(
                    "QueryModelMapping.item.not.searchable",
                    new Object[]{itemName},
                    technicalMessage);
            throw new QueryException(message);
        }
        return resolver;
    }

    /**
     * Returns {@link ItemRelationResolver} for provided {@link ItemName} or `null`.
     *
     * @param <TQ> type of target entity path
     * @param <TR> row type related to the target entity path {@link TQ}
     */
    public @Nullable <TQ extends FlexibleRelationalPathBase<TR>, TR>
    ItemRelationResolver<Q, R, TQ, TR> getRelationResolver(QName itemName) {
        //noinspection unchecked
        return (ItemRelationResolver<Q, R, TQ, TR>)
                ItemNameUtil.getByQName(itemRelationResolvers, itemName);
    }

    /** Returns copy of the map of the item mappings. */
    public final @NotNull Map<QName, ItemSqlMapper<Q, R>> getItemMappings() {
        return new LinkedHashMap<>(itemMappings);
    }

    public ItemDefinition<?> itemDefinition() {
        return itemDefinition;
    }
}
