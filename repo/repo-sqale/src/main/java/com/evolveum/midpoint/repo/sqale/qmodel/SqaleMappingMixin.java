package com.evolveum.midpoint.repo.sqale.qmodel;

import java.util.function.BiFunction;
import javax.xml.namespace.QName;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.sqale.delta.item.EmbeddedContainerDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.delta.item.RefTableItemDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.delta.item.TableContainerDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.RefTableItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.mapping.NestedMappingResolver;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleItemSqlMapper;
import com.evolveum.midpoint.repo.sqale.mapping.TableRelationResolver;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

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
            @NotNull ItemName itemName,
            @NotNull ItemRelationResolver itemRelationResolver);

    QueryModelMapping<S, Q, R> addItemMapping(
            @NotNull QName itemName, @NotNull ItemSqlMapper itemMapper);

    /**
     * Defines nested mapping for container embedded in the same table.
     * This includes path resolver for queries and delta processor for modifications.
     */
    default <N extends Containerable> SqaleNestedMapping<N, Q, R> addNestedMapping(
            @NotNull ItemName itemName, @NotNull Class<N> nestedSchemaType) {
        SqaleNestedMapping<N, Q, R> nestedMapping =
                new SqaleNestedMapping<>(nestedSchemaType, queryType());
        addRelationResolver(itemName, new NestedMappingResolver<>(nestedMapping));
        // first function for query doesn't matter, it just can't be null
        addItemMapping(itemName, new SqaleItemSqlMapper(ctx -> null,
                ctx -> new EmbeddedContainerDeltaProcessor<>(ctx, nestedMapping)));
        return nestedMapping;
    }

    /** Defines reference mapping for both query and modifications. */
    default SqaleMappingMixin<S, Q, R> addRefMapping(
            @NotNull QName itemName, @NotNull QObjectReferenceMapping qReferenceMapping) {
        addItemMapping(itemName, new SqaleItemSqlMapper(
                ctx -> new RefTableItemFilterProcessor(ctx, qReferenceMapping),
                ctx -> new RefTableItemDeltaProcessor(ctx, qReferenceMapping)));
        // TODO add relation mapping too for reaching to the reference target
        return this;
    }

    /**
     * Defines table mapping for multi-value container owned by an object or another container.
     * This includes path resolver for queries and delta processor for modifications.
     */
    default <TQ extends QContainer<TR>, TR extends MContainer>
    SqaleMappingMixin<S, Q, R> addContainerTableMapping(
            @NotNull ItemName itemName,
            @NotNull QContainerMapping<?, TQ, TR> containerMapping,
            @NotNull BiFunction<Q, TQ, Predicate> joinPredicate) {
        addRelationResolver(itemName,
                new TableRelationResolver<>(containerMapping.queryType(), joinPredicate));

        addItemMapping(itemName, new SqaleItemSqlMapper(
                ctx -> null, // TODO query mapping later
                ctx -> new TableContainerDeltaProcessor<>(ctx, containerMapping)));
        return this;
    }
}
