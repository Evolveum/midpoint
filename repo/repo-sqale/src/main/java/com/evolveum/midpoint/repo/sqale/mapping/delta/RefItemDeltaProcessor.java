/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping.delta;

import java.util.UUID;
import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sqale.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class RefItemDeltaProcessor extends ItemDeltaProcessor<ObjectReferenceType> {

    // only oidPath is strictly not-null, but then the filter better not ask for type or relation
    private final UuidPath oidPath;
    private final EnumPath<MObjectType> typePath;
    private final NumberPath<Integer> relationIdPath;

    public RefItemDeltaProcessor(
            SqaleUpdateContext<?, ?, ?> context,
            Function<EntityPath<?>, UuidPath> rootToOidPath,
            Function<EntityPath<?>, EnumPath<MObjectType>> rootToTypePath,
            Function<EntityPath<?>, NumberPath<Integer>> rootToRelationIdPath) {
        this(context,
                rootToOidPath.apply(context.path()),
                rootToTypePath != null ? rootToTypePath.apply(context.path()) : null,
                rootToRelationIdPath != null ? rootToRelationIdPath.apply(context.path()) : null);
    }

    // exposed mainly for RefTableItemFilterProcessor
    RefItemDeltaProcessor(SqaleUpdateContext<?, ?, ?> context,
            UuidPath oidPath, EnumPath<MObjectType> typePath, NumberPath<Integer> relationIdPath) {
        super(context);
        this.oidPath = oidPath;
        this.typePath = typePath;
        this.relationIdPath = relationIdPath;
    }

    @Override
    public void process(ItemDelta<?, ?> modification) throws RepositoryException {
        ObjectReferenceType ref = getAnyValue(modification);

        // See implementation comments in SinglePathItemDeltaProcessor#process for logic details.
        if (modification.isDelete() || ref == null) {
            context.set(oidPath, null);
            context.set(typePath, null);
            context.set(relationIdPath, null);
        } else {
            context.set(oidPath, UUID.fromString(ref.getOid()));
            context.set(typePath, MObjectType.fromTypeQName(ref.getType()));
            context.set(relationIdPath, context.processCacheableRelation(ref.getRelation()));
        }
    }
}
