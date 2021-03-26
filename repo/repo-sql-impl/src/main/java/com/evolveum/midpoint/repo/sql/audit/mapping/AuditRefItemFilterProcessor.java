/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.audit.mapping;

import java.util.function.Function;

import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemSqlMapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Filter processor for a reference attribute paths of audit-related types.
 * In audit the references have OID and (mostly, but optionally) type, but never relation.
 */
public class AuditRefItemFilterProcessor extends ItemFilterProcessor<RefFilter> {

    /**
     * Returns the mapper function creating the ref-filter processor from context.
     */
    public static ItemSqlMapper mapper(Function<EntityPath<?>, StringPath> rootToOidPath) {
        return new ItemSqlMapper(ctx -> new AuditRefItemFilterProcessor(ctx, rootToOidPath, null, null));
    }

    public static ItemSqlMapper mapper(
            Function<EntityPath<?>, StringPath> rootToOidPath,
            Function<EntityPath<?>, StringPath> rootToNamePath,
            Function<EntityPath<?>, NumberPath<Integer>> rootToTypePath) {
        return new ItemSqlMapper(ctx ->
                new AuditRefItemFilterProcessor(ctx, rootToOidPath, rootToNamePath, rootToTypePath));
    }

    private final StringPath oidPath;
    private final StringPath namePath;
    private final NumberPath<Integer> typePath;

    private AuditRefItemFilterProcessor(
            SqlQueryContext<?, ?, ?> context,
            Function<EntityPath<?>, StringPath> rootToOidPath,
            Function<EntityPath<?>, StringPath> rootToNamePath,
            Function<EntityPath<?>, NumberPath<Integer>> rootToTypePath) {
        super(context);
        this.oidPath = rootToOidPath.apply(context.path());
        this.namePath = rootToNamePath != null ? rootToNamePath.apply(context.path()) : null;
        this.typePath = rootToTypePath != null ? rootToTypePath.apply(context.path()) : null;
    }

    @Override
    public Predicate process(RefFilter filter) {
        PrismReferenceValue singleValue = filter.getSingleValue();
        Referencable ref = singleValue != null ? singleValue.getRealValue() : null;

        if (ref != null) {
            if (ref.getOid() != null) {
                return predicateWithNotTreated(oidPath, oidPath.eq(ref.getOid()));
            } else if (namePath != null
                    && ref.getTargetName() != null && ref.getTargetName().getOrig() != null) {
                Predicate left = predicateWithNotTreated(namePath,
                        namePath.eq(ref.getTargetName().getOrig()));
                Predicate right = null;
                if (ref.getType() != null && typePath != null) {
                    right = creteTypePredicate(ref);
                }
                return ExpressionUtils.and(left, right);
            } else if (ref.getType() != null && typePath != null) {
                return creteTypePredicate(ref);
            }
        }
        return oidPath.isNull();
    }

    private Predicate creteTypePredicate(Referencable ref) {
        Class<? extends ObjectType> type = context.qNameToSchemaClass(ref.getType());
        return predicateWithNotTreated(typePath,
                typePath.eq(RObjectType.getByJaxbType(type).ordinal()));
    }
}
