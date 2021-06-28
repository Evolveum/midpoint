/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.UUID;
import java.util.function.Function;

import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

public class RefItemDeltaProcessor extends ItemDeltaSingleValueProcessor<Referencable> {

    private final UuidPath oidPath;
    private final EnumPath<MObjectType> typePath;
    private final NumberPath<Integer> relationIdPath;

    /**
     * @param <Q> entity query type from which the attribute is resolved
     * @param <R> row type related to {@link Q}
     */
    public <Q extends FlexibleRelationalPathBase<R>, R> RefItemDeltaProcessor(
            SqaleUpdateContext<?, Q, R> context,
            Function<Q, UuidPath> rootToOidPath,
            Function<Q, EnumPath<MObjectType>> rootToTypePath,
            Function<Q, NumberPath<Integer>> rootToRelationIdPath) {
        this(context,
                rootToOidPath.apply(context.entityPath()),
                rootToTypePath != null ? rootToTypePath.apply(context.entityPath()) : null,
                rootToRelationIdPath != null ? rootToRelationIdPath.apply(context.entityPath()) : null);
    }

    /**
     * @param <Q> entity query type from which the attribute is resolved
     * @param <R> row type related to {@link Q}
     */
    // exposed mainly for RefTableItemFilterProcessor
    <Q extends FlexibleRelationalPathBase<R>, R> RefItemDeltaProcessor(
            SqaleUpdateContext<?, Q, R> context,
            UuidPath oidPath, EnumPath<MObjectType> typePath, NumberPath<Integer> relationIdPath) {
        super(context);
        this.oidPath = oidPath;
        this.typePath = typePath;
        this.relationIdPath = relationIdPath;
    }

    @Override
    public void setValue(Referencable value) {
        context.set(oidPath, UUID.fromString(value.getOid()));
        context.set(typePath, MObjectType.fromTypeQName(value.getType()));
        context.set(relationIdPath,
                context.repositoryContext().processCacheableRelation(value.getRelation()));
    }

    @Override
    public void delete() {
        context.set(oidPath, null);
        context.set(typePath, null);
        context.set(relationIdPath, null);
    }
}
