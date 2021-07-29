/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.FilterOperation;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SimpleItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SinglePathItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Similar to {@link SimpleItemFilterProcessor} but String value can be just UUID prefixes
 * and must be smartly converted based on the actual operation.
 */
public class UuidItemFilterProcessor extends SinglePathItemFilterProcessor<String, UuidPath> {

    public <Q extends FlexibleRelationalPathBase<R>, R> UuidItemFilterProcessor(
            SqlQueryContext<?, Q, R> context, Function<Q, UuidPath> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    public Predicate process(PropertyValueFilter<String> filter) throws RepositoryException {
        // This is adapted version of ItemValueFilterProcessor#createBinaryCondition.
        // Because conversion is different for various operations, we don't use ValueFilterValues.
        FilterOperation operation = operation(filter);
        if (filter.getValues() == null || filter.getValues().isEmpty()) {
            if (operation.isAnyEqualOperation()) {
                return ExpressionUtils.predicate(Ops.IS_NULL, path);
            } else {
                throw new QueryException("Null value for other than EQUAL filter: " + filter);
            }
        }

        if (filter.getValues().size() > 1) {
            if (operation.isAnyEqualOperation()) {
                List<UUID> oids = filter.getValues().stream()
                        .map(s -> UUID.fromString(s.getValue()))
                        .collect(Collectors.toList());
                return ExpressionUtils.predicate(Ops.IN,
                        operation.treatPathForIn(path), ConstantImpl.create(oids));
            } else {
                throw new QueryException("Multi-value for other than EQUAL filter: " + filter);
            }
        }

        //noinspection ConstantConditions
        String oid = filter.getSingleValue().getValue();
        // TODO treatment for prefixes based on operation
        return singleValuePredicate(path, operation, UUID.fromString(oid));
    }
}
