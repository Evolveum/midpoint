package com.evolveum.midpoint.repo.sqale.mapping;

import java.util.Objects;
import java.util.function.BiFunction;
import javax.xml.namespace.QName;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.sqale.delta.item.ContainerTableDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.delta.item.EmbeddedContainerDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.delta.item.RefTableItemDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.RefTableItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
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
            @NotNull ItemRelationResolver<Q, R> itemRelationResolver);

    QueryModelMapping<S, Q, R> addItemMapping(
            @NotNull QName itemName, @NotNull ItemSqlMapper<Q, R> itemMapper);

    /**
     * Defines nested mapping for container embedded in the same table.
     * This includes path resolver for queries and delta processor for modifications.
     *
     * @param <N> schema type of the nested container
     */
    default <N extends Containerable> SqaleNestedMapping<N, Q, R> addNestedMapping(
            @NotNull ItemName itemName, @NotNull Class<N> nestedSchemaType) {
        SqaleNestedMapping<N, Q, R> nestedMapping =
                new SqaleNestedMapping<>(nestedSchemaType, queryType());
        addRelationResolver(itemName, new NestedMappingResolver<>(nestedMapping));
        addItemMapping(itemName, new SqaleItemSqlMapper<>(
                ctx -> new EmbeddedContainerDeltaProcessor<>(ctx, nestedMapping)));
        return nestedMapping;
    }

    /** Defines reference mapping for both query and modifications. */
    default SqaleMappingMixin<S, Q, R> addRefMapping(
            @NotNull QName itemName, @NotNull QReferenceMapping<?, ?, Q, R> referenceMapping) {
        Objects.requireNonNull(referenceMapping, "referenceMapping");
        addItemMapping(itemName, new SqaleItemSqlMapper<>(
                ctx -> new RefTableItemFilterProcessor<>(ctx, referenceMapping),
                ctx -> new RefTableItemDeltaProcessor<>(ctx, referenceMapping)));

        // TODO add relation mapping too for reaching to the reference target
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
        // TODO join predicate can be constructed by Q or the mapping,
        //  perhaps with QOwnedBy<OQ, OR> instead of just <OR>, but that may mean QContainer<R, OQ, OR>...?
        //  of course the join would be implemented in QOwnedBy
        //  BTW: adding OQ on refs is messy, we already have AOR and we would need AOQ for QAssignmentReferenceMapping too.
        addRelationResolver(itemName,
                new ContainerTableRelationResolver<>(containerMapping, joinPredicate));

        addItemMapping(itemName, new SqaleItemSqlMapper<>(
                ctx -> new ContainerTableDeltaProcessor<>(ctx, containerMapping)));
        return this;
    }
}
