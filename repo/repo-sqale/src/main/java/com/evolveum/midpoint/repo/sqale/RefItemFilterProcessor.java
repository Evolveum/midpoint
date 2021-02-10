/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.NumberPath;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QUri;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Filter processor for reference item paths, typically mapped to three columns.
 * OID is represented by UUID column, type by ID (see {@link MObjectTypeMapping} and relation
 * by Integer (foreign key) to {@link QUri}.
 */
public class RefItemFilterProcessor extends ItemFilterProcessor<RefFilter> {

    /**
     * Returns the mapper function creating the ref-filter processor from query context.
     */
    public static ItemSqlMapper mapper(
            Function<EntityPath<?>, UuidPath> rootToOidPath,
            Function<EntityPath<?>, NumberPath<Integer>> rootToTypePath,
            Function<EntityPath<?>, NumberPath<Integer>> rootToRelationIdPath) {
        return new ItemSqlMapper(ctx -> new RefItemFilterProcessor(
                ctx, rootToOidPath, rootToTypePath, rootToRelationIdPath));
    }

    private final UuidPath oidPath;
    private final NumberPath<Integer> typePath;
    private final NumberPath<Integer> relationIdPath;

    private RefItemFilterProcessor(
            SqlQueryContext<?, ?, ?> context,
            Function<EntityPath<?>, UuidPath> rootToOidPath,
            Function<EntityPath<?>, NumberPath<Integer>> rootToTypePath,
            Function<EntityPath<?>, NumberPath<Integer>> rootToRelationIdPath) {
        super(context);
        this.oidPath = rootToOidPath.apply(context.path());
        this.typePath = rootToTypePath != null ? rootToTypePath.apply(context.path()) : null;
        this.relationIdPath = rootToRelationIdPath != null
                ? rootToRelationIdPath.apply(context.path()) : null;
    }

    @Override
    public Predicate process(RefFilter filter) {
        List<PrismReferenceValue> values = filter.getValues();
        if (values == null || values.isEmpty()) {
            return oidPath.isNull();
        }
        if (values.size() == 1) {
            return processSingleValue(filter, values.get(0));
        }

        Predicate predicate = null;
        for (PrismReferenceValue ref : values) {
            predicate = ExpressionUtils.or(predicate, processSingleValue(filter, ref));
        }
        return predicate;
    }

    private Predicate processSingleValue(RefFilter filter, PrismReferenceValue ref) {
        Predicate predicate = null;
        if (ref.getOid() != null) {
            predicate = predicateWithNotTreated(oidPath,
                    oidPath.eq(UUID.fromString(ref.getOid())));
        } else if (!filter.isOidNullAsAny()) {
            predicate = oidPath.isNull();
        }
        if (ref.getRelation() != null) {
            Integer relationId = ((SqaleRepoContext) context.sqlRepoContext())
                    .getCachedUriId(ref.getRelation());
            predicate = ExpressionUtils.and(predicate,
                    predicateWithNotTreated(relationIdPath, relationIdPath.eq(relationId)));
        } else if (!filter.isRelationNullAsAny()) {
            // TODO add support for Q_ANY and use default relation by default instead of null
            predicate = ExpressionUtils.and(predicate, relationIdPath.isNull());
        }
        if (ref.getTargetType() != null) {
            int typeCode = MObjectTypeMapping.fromTypeQName(ref.getTargetType()).code();
            predicate = ExpressionUtils.and(predicate,
                    predicateWithNotTreated(typePath, typePath.eq(typeCode)));
        } else if (!filter.isTargetTypeNullAsAny()) {
            predicate = ExpressionUtils.and(predicate, typePath.isNull());
        }
        return predicate;
    }
}
