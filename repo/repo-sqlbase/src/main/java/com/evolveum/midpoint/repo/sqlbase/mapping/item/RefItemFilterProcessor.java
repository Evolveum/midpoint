/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping.item;

import java.util.function.Function;

import com.querydsl.core.types.*;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.sqlbase.SqlPathContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Filter processor for a reference attribute path.
 */
public class RefItemFilterProcessor extends ItemFilterProcessor<RefFilter> {

    /**
     * Returns the mapper function creating the string filter processor from context.
     */
    public static ItemSqlMapper mapper(Function<EntityPath<?>, Path<?>> rootToQueryItem) {
        return new ItemSqlMapper(ctx -> new RefItemFilterProcessor(ctx, rootToQueryItem, null, null));
    }

    public static ItemSqlMapper mapper(
            Function<EntityPath<?>, Path<?>> rootOidToQueryItem,
            Function<EntityPath<?>, Path<?>> rootTypeToQueryItem,
            Function<Class<ObjectType>, Integer> typeConversionFunction) {
        return new ItemSqlMapper(ctx -> new RefItemFilterProcessor(
                ctx, rootOidToQueryItem, rootTypeToQueryItem, typeConversionFunction));
    }

    private final Path<?> oidPath;
    private final Path<?> typePath;
    private final Function<Class<ObjectType>, Integer> typeConversionFunction;

    private RefItemFilterProcessor(
            SqlPathContext<?, ?, ?> context,
            Function<EntityPath<?>, Path<?>> rootOidToQueryItem,
            Function<EntityPath<?>, Path<?>> rootTypeToQueryItem,
            Function<Class<ObjectType>, Integer> typeConversionFunction) {
        super(context);
        this.oidPath = rootOidToQueryItem.apply(context.path());
        this.typePath = rootTypeToQueryItem != null ? rootTypeToQueryItem.apply(context.path()) : null;
        this.typeConversionFunction = typeConversionFunction;
    }

    @Override
    public Predicate process(RefFilter filter) {
        PrismReferenceValue singleValue = filter.getSingleValue();
        Referencable ref = singleValue != null ? singleValue.getRealValue() : null;
        if (ref != null) {
            if (ref.getOid() != null) {
                return ExpressionUtils.predicate(Ops.EQ, oidPath, ConstantImpl.create(ref.getOid()));
            } else if (ref.getType() != null && typePath != null && typeConversionFunction != null) {
                Class<ObjectType> type = context.prismContext().getSchemaRegistry().getCompileTimeClass(ref.getType());
                return ExpressionUtils.predicate(Ops.EQ, typePath, ConstantImpl.create(typeConversionFunction.apply(type)));
            }
        }
        return ExpressionUtils.predicate(Ops.IS_NULL, oidPath);
    }
}
