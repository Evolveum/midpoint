/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import java.util.function.Function;

import javax.xml.namespace.QName;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EnumPath;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.FilterOperation;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SimpleItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SinglePathItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Similar to {@link SimpleItemFilterProcessor} but String value can be just UUID prefixes
 * and must be smartly converted based on the actual operation.
 *
 * [WARNING]
 * Prefix support assumes OID column only and does not treat predicate for nullable columns.
 */
public class TypeQNameItemFilterProcessor extends SinglePathItemFilterProcessor<QName, EnumPath<MObjectType>> {

    public <Q extends FlexibleRelationalPathBase<R>, R> TypeQNameItemFilterProcessor(
            SqlQueryContext<?, Q, R> context, Function<Q, EnumPath<MObjectType>> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    public Predicate process(PropertyValueFilter<QName> filter) throws RepositoryException {
        FilterOperation operation = operation(filter);
        if (!operation.isEqualOperation()) {
            throw new QueryException("Only equal filter supported");
        }
        if (filter.hasNoValue()) {
            if (operation.isAnyEqualOperation()) {
                return ExpressionUtils.predicate(Ops.IS_NULL, path);
            } else {
                throw new QueryException("Null value for other than EQUAL filter: " + filter);
            }
        }

        Predicate predicate = null;
        for (var value : filter.getValues()) {
            var local = path.eq(MObjectType.fromTypeQName(value.getValue()));
            if (predicate == null) {
                predicate = local;
            } else {
                predicate = ExpressionUtils.and(predicate, local);
            }
        }
        return predicate;
    }

}
