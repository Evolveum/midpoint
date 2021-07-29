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
 *
 * [WARNING]
 * This currently assumes OID column only and does not treat predicate for nullable columns.
 */
public class UuidItemFilterProcessor extends SinglePathItemFilterProcessor<String, UuidPath> {

    private static final String OID_MIN = "00000000-0000-0000-0000-000000000000";
    private static final String OID_MAX = "ffffffff-ffff-ffff-ffff-ffffffffffff";

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
        if (oid.length() < OID_MIN.length()) {
            // operator is enough, ignore case doesn't play any role for UUID type
            return processIncompleteOid(oid, operation.operator, filter);
        } else {
            // singleValuePredicate() treatment is not necessary, let's just create the predicate
            return ExpressionUtils.predicate(operation.operator,
                    path, ConstantImpl.create(UUID.fromString(oid)));
        }
    }

    // we don't need to "treat" predicate, OID can't be null
    private Predicate processIncompleteOid(
            String oid, Ops operator, PropertyValueFilter<String> filter) throws QueryException {
        if (operator == Ops.GT || operator == Ops.GOE) {
            return path.goe(finishWithZeros(oid));
        }
        if (operator == Ops.LT || operator == Ops.LOE) {
            return path.lt(finishWithZeros(oid));
        }
        if (operator == Ops.STARTS_WITH || operator == Ops.STARTS_WITH_IC) {
            return path.goe(finishWithZeros(oid)).and(path.loe(finishWithEfs(oid)));
        }
        throw new QueryException("Unsupported operator " + operator + " for incomplete OID '"
                + oid + "' in filter: " + filter);
    }

    private UUID finishWithZeros(String oid) {
        return UUID.fromString(oid + OID_MIN.substring(oid.length()));
    }

    private UUID finishWithEfs(String oid) {
        return UUID.fromString(oid + OID_MAX.substring(oid.length()));
    }
}
