/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.UUID;
import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sqale.RootUpdateContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

public class RefItemDeltaProcessor extends ItemDeltaSingleValueProcessor<Referencable> {

    // only oidPath is strictly not-null, but then the filter better not ask for type or relation
    private final UuidPath oidPath;
    private final EnumPath<MObjectType> typePath;
    private final NumberPath<Integer> relationIdPath;

    public RefItemDeltaProcessor(
            RootUpdateContext<?, ?, ?> context,
            Function<EntityPath<?>, UuidPath> rootToOidPath,
            Function<EntityPath<?>, EnumPath<MObjectType>> rootToTypePath,
            Function<EntityPath<?>, NumberPath<Integer>> rootToRelationIdPath) {
        this(context,
                rootToOidPath.apply(context.path()),
                rootToTypePath != null ? rootToTypePath.apply(context.path()) : null,
                rootToRelationIdPath != null ? rootToRelationIdPath.apply(context.path()) : null);
    }

    // exposed mainly for RefTableItemFilterProcessor
    RefItemDeltaProcessor(RootUpdateContext<?, ?, ?> context,
            UuidPath oidPath, EnumPath<MObjectType> typePath, NumberPath<Integer> relationIdPath) {
        super(context);
        this.oidPath = oidPath;
        this.typePath = typePath;
        this.relationIdPath = relationIdPath;
    }

    @Override
    public void process(ItemDelta<?, ?> modification) throws RepositoryException {
        Referencable ref = getAnyValue(modification);

        // See implementation comments in SinglePathItemDeltaProcessor#process for logic details.
        if (modification.isDelete() || ref == null) {
            delete();
        } else {
            setValue(ref);
        }
    }

    @Override
    public void setValue(Referencable value) {
        context.set(oidPath, UUID.fromString(value.getOid()));
        context.set(typePath, MObjectType.fromTypeQName(value.getType()));
        context.set(relationIdPath, context.processCacheableRelation(value.getRelation()));
    }

    @Override
    public void delete() {
        context.set(oidPath, null);
        context.set(typePath, null);
        context.set(relationIdPath, null);
    }
}
