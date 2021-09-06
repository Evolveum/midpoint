/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.audit.filtering;

import static com.querydsl.core.types.dsl.Expressions.booleanTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;

import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SinglePathItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordPropertyType;

/**
 * Filter processor for audit properties stored in a single JSONB column.
 * Values for all keys are arrays, they can be multi-value, so they are always treated so.
 */
public class AuditPropertiesItemFilterProcessor
        extends SinglePathItemFilterProcessor<AuditEventRecordPropertyType, JsonbPath> {

    public <Q extends FlexibleRelationalPathBase<R>, R> AuditPropertiesItemFilterProcessor(
            SqlQueryContext<?, Q, R> context,
            Function<Q, JsonbPath> rootToPath) {
        super(context, rootToPath);
    }

    @Override
    public Predicate process(PropertyValueFilter<AuditEventRecordPropertyType> filter)
            throws RepositoryException {
        if (!(filter instanceof EqualFilter) || filter.getMatchingRule() != null) {
            throw new QueryException("Can't translate filter '" + filter + "' to operation."
                    + " Audit properties support only equals with no matching rule.");
        }

        ValueFilterValues<AuditEventRecordPropertyType, AuditEventRecordPropertyType> values =
                ValueFilterValues.from(filter);
        if (values.isEmpty()) {
            return Expressions.booleanTemplate("({0} = '{}' OR {0} is null)", path);
        }

        if (values.isMultiValue()) {
            // This works with GIN index: https://dba.stackexchange.com/a/130863/157622
            return predicateWithNotTreated(path,
                    booleanTemplate("{0} @> ANY ({1})", path,
                            values.allValues().stream()
                                    .map(v -> jsonbValue(v))
                                    .toArray(Jsonb[]::new)));
        } else {
            return predicateWithNotTreated(path,
                    booleanTemplate("{0} @> {1}", path,
                            jsonbValue(Objects.requireNonNull(values.singleValue()))));
        }
    }

    private Jsonb jsonbValue(AuditEventRecordPropertyType propertyValue) {
        // getValue returns List, so it matches the structure in JSON: {"key":["value",...], ...}
        return Jsonb.fromMap(Map.of(propertyValue.getName(), propertyValue.getValue()));
    }
}
