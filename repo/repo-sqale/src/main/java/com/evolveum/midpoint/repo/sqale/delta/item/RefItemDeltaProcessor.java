/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.UUID;
import java.util.function.Function;

import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;

import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class RefItemDeltaProcessor extends ItemDeltaSingleValueProcessor<ObjectReferenceType> {

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
        super(context);
        this.oidPath = rootToOidPath.apply(context.entityPath());
        this.typePath = rootToTypePath != null ? rootToTypePath.apply(context.entityPath()) : null;
        this.relationIdPath = rootToRelationIdPath != null ? rootToRelationIdPath.apply(context.entityPath()) : null;
    }

    @Override
    public void setValue(ObjectReferenceType value) {
        value = SqaleUtils.referenceWithTypeFixed(value);
        var oid = value.getOid() != null ? UUID.fromString(value.getOid()) : null;
        // OID can be null in some references - (eg. filter is specified and resolution time is run)
        context.set(oidPath, oid);
        context.set(typePath, MObjectType.fromTypeQName(value.getType()));
        context.set(relationIdPath,
                context.repositoryContext().processCacheableRelation(value.getRelation()));
    }

    @Override
    public void delete() {
        context.setNull(oidPath);
        context.setNull(typePath);
        context.setNull(relationIdPath);
    }
}
