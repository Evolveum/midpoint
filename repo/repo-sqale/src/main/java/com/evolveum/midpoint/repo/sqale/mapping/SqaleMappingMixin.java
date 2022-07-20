/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.xml.namespace.QName;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.delta.item.*;
import com.evolveum.midpoint.repo.sqale.filtering.RefItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.RefTableItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.*;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Mix of common mapping support methods that is needed on both {@link SqaleNestedMapping}
 * and {@link SqaleTableMapping} which are in separate branches of the hierarchy starting in
 * repo-sqlbase - which is out of reach and sqale-specific functionality can't go there.
 *
 * @param <S> schema type
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
@SuppressWarnings("UnusedReturnValue")
public interface SqaleMappingMixin<S, Q extends FlexibleRelationalPathBase<R>, R> {

    Class<Q> queryType();

    @SuppressWarnings("UnusedReturnValue")
    QueryModelMapping<S, Q, R> addRelationResolver(
            @NotNull QName itemName,
            @NotNull ItemRelationResolver<Q, R, ?, ?> itemRelationResolver);

    QueryModelMapping<S, Q, R> addItemMapping(
            @NotNull QName itemName, @NotNull ItemSqlMapper<Q, R> itemMapper);

    /**
     * Defines nested mapping for container embedded in the same table.
     * This includes path resolver for queries and delta processor for modifications.
     *
     * @param <N> schema type of the nested container
     */
    default <N extends Containerable> SqaleNestedMapping<N, Q, R> addNestedMapping(
            @NotNull QName itemName, @NotNull Class<N> nestedSchemaType) {
        SqaleNestedMapping<N, Q, R> nestedMapping =
                new SqaleNestedMapping<>(nestedSchemaType, queryType());
        addRelationResolver(itemName, new NestedMappingResolver<>(nestedMapping));
        addItemMapping(itemName, new SqaleItemSqlMapper<>(
                ctx -> new EmbeddedContainerDeltaProcessor<>(ctx, nestedMapping)));
        return nestedMapping;
    }

    /** Defines multi-value reference mapping (refs in table) for both query and modifications. */
    default <TQ extends QReference<TR, R>, TR extends MReference>
    SqaleMappingMixin<S, Q, R> addRefMapping(
            @NotNull QName itemName, @NotNull QReferenceMapping<TQ, TR, Q, R> referenceMapping) {
        Objects.requireNonNull(referenceMapping, "referenceMapping");
        addItemMapping(itemName, new SqaleItemSqlMapper<>(
                ctx -> new RefTableItemFilterProcessor<>(ctx, referenceMapping),
                ctx -> new RefTableItemDeltaProcessor<>(ctx, referenceMapping)));

        // Needed for queries with ref/@/... paths, this resolves the "ref/" part before @.
        addRelationResolver(itemName, TableRelationResolver.usingSubquery(
                referenceMapping, referenceMapping.correlationPredicate()));
        return this;
    }

    /** Defines single-value reference mapping for both query and modifications, columns embedded in the table. */
    default <TS, TQ extends QObject<TR>, TR extends MObject> SqaleMappingMixin<S, Q, R> addRefMapping(
            @NotNull QName itemName,
            @NotNull Function<Q, UuidPath> rootToOidPath,
            @Nullable Function<Q, EnumPath<MObjectType>> rootToTypePath,
            @Nullable Function<Q, NumberPath<Integer>> rootToRelationIdPath,
            @NotNull Supplier<QueryTableMapping<TS, TQ, TR>> targetMappingSupplier) {
        ItemSqlMapper<Q, R> referenceMapping = new SqaleItemSqlMapper<>(
                ctx -> new RefItemFilterProcessor(ctx,
                        rootToOidPath, rootToTypePath, rootToRelationIdPath, null),
                ctx -> new RefItemDeltaProcessor(ctx,
                        rootToOidPath, rootToTypePath, rootToRelationIdPath));
        addItemMapping(itemName, referenceMapping);

        // Needed for queries with ref/@/... paths, this resolves the "ref/" part before @
        // and inside EmbeddedReferenceResolver is the magic resolving the @ part.
        addRelationResolver(itemName, new EmbeddedReferenceResolver<>(
                queryType(), rootToOidPath, targetMappingSupplier));
        return this;
    }

    /** Defines single-value reference mapping for query, columns embedded in the table. */
    default <TS, TQ extends QObject<TR>, TR extends MObject> SqaleMappingMixin<S, Q, R> addAuditRefMapping(
            @NotNull QName itemName,
            @NotNull Function<Q, UuidPath> rootToOidPath,
            @Nullable Function<Q, EnumPath<MObjectType>> rootToTypePath,
            @NotNull Function<Q, StringPath> rootToTargetNamePath,
            @NotNull Supplier<QueryTableMapping<TS, TQ, TR>> targetMappingSupplier) {
        ItemSqlMapper<Q, R> referenceMapping = new DefaultItemSqlMapper<>(
                ctx -> new RefItemFilterProcessor(ctx,
                        rootToOidPath, rootToTypePath, null, rootToTargetNamePath));
        addItemMapping(itemName, referenceMapping);

        // Needed for queries with ref/@/... paths, this resolves the "ref/" part before @
        // and inside EmbeddedReferenceResolver is the magic resolving the @ part.
        addRelationResolver(itemName, new EmbeddedReferenceResolver<>(
                queryType(), rootToOidPath, targetMappingSupplier));
        return this;
    }

    /**
     * Defines table mapping for multi-value container owned by an object or another container.
     * This includes path resolver for queries and delta processor for modifications.
     *
     * @param <C> schema type of the container mapped to the target table
     * @param <TQ> entity query type of the container table
     * @param <TR> row type of the container table, related to {@link TQ}
     */
    default <C extends Containerable, TQ extends QContainer<TR, R>, TR extends MContainer>
    SqaleMappingMixin<S, Q, R> addContainerTableMapping(
            @NotNull ItemName itemName,
            @NotNull QContainerMapping<C, TQ, TR, R> containerMapping,
            @NotNull BiFunction<Q, TQ, Predicate> joinPredicate) {
        addRelationResolver(itemName,
                new ContainerTableRelationResolver<>(containerMapping, joinPredicate));

        addItemMapping(itemName, new SqaleItemSqlMapper<>(
                ctx -> new ContainerTableDeltaProcessor<>(ctx, containerMapping)));
        return this;
    }

    default SqaleMappingMixin<S, Q, R> addExtensionMapping(
            @NotNull ItemName itemName,
            @NotNull MExtItemHolderType holderType,
            @NotNull Function<Q, JsonbPath> rootToPath,
            @NotNull SqaleRepoContext repoContext) {
        ExtensionMapping<Q, R> mapping =
                new ExtensionMapping<>(holderType, queryType(), rootToPath, repoContext);
        addRelationResolver(itemName, new ExtensionMappingResolver<>(mapping, rootToPath));
        addItemMapping(itemName, new SqaleItemSqlMapper<>(
                ctx -> new ExtensionContainerDeltaProcessor<>(ctx, mapping, rootToPath)));
        return this;
    }

    /** Method called from `SqaleUpdateContext.finishExecutionOwn()` for containers. */
    default void afterModify(SqaleUpdateContext<S, Q, R> updateContext) throws SchemaException {
        // nothing by default
    }
}
