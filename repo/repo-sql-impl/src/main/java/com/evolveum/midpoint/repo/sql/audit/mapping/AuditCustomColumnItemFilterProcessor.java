/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.audit.mapping;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.FilterOperation;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemValueFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.DefaultItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordCustomColumnPropertyType;

/**
 * Filter processor for an audit custom column attribute path (Prism item).
 *
 * NOTE: This is old-repo audit class, the same is used for new repo too - this one will go away eventually.
 *
 * While it is a single-path processor, it does NOT support ordering, because "what to order by"
 * is part of the filter value ({link {@link AuditEventRecordCustomColumnPropertyType#getName()}}).
 * <p>
 * Design note: While it is technically possible to change the primary item mapping function of
 * {@link ItemSqlMapper} to be a function of both entity path <b>and value</b>, the value is simply
 * not available in order specification anyway.
 * So this is a limitation of current design of audit custom columns (not much of Query API).
 * If custom column was a proper extension column with its own item Q-name, it would be possible
 * with the same item filter processors used for non-extension columns (only the mapping
 * registration would be dynamic, which is not a big deal).
 */
public class AuditCustomColumnItemFilterProcessor extends ItemValueFilterProcessor<
        PropertyValueFilter<AuditEventRecordCustomColumnPropertyType>> {

    /**
     * One mapper is enough as it is stateless and everything happens in {@link #process}.
     */
    private static final ItemSqlMapper<?, ?> MAPPER =
            new DefaultItemSqlMapper<>(ctx -> new AuditCustomColumnItemFilterProcessor(ctx));

    /**
     * Returns the mapper creating the string filter processor from context.
     */
    @SuppressWarnings("unchecked")
    public static <Q extends FlexibleRelationalPathBase<R>, R> ItemSqlMapper<Q, R> mapper() {
        return (ItemSqlMapper<Q, R>) MAPPER;
    }

    private AuditCustomColumnItemFilterProcessor(SqlQueryContext<?, ?, ?> context) {
        super(context);
    }

    @Override
    public Predicate process(PropertyValueFilter<AuditEventRecordCustomColumnPropertyType> filter)
            throws QueryException {
        // This is a tricky situation, if multi-value, each value can have different path (derived
        // from AuditEventRecordCustomColumnPropertyType.getName()), so we can't use this directly.
        ValueFilterValues<AuditEventRecordCustomColumnPropertyType, ?> values =
                ValueFilterValues.from(filter);
        if (values.isEmpty()) {
            throw new QueryException("Custom column null value is not supported,"
                    + " column can't be determined from filter: " + filter);
        }
        if (values.isMultiValue()) {
            Predicate predicate = null;
            for (AuditEventRecordCustomColumnPropertyType propertyType : values.allValuesRaw()) {
                Predicate right = createPredicate(filter, propertyType);
                predicate = predicate != null
                        ? ExpressionUtils.or(predicate, right)
                        : right;
            }
            return predicate;
        }

        AuditEventRecordCustomColumnPropertyType value = values.singleValueRaw();
        assert value != null;
        return createPredicate(filter, value);
    }

    private Predicate createPredicate(
            PropertyValueFilter<AuditEventRecordCustomColumnPropertyType> filter,
            AuditEventRecordCustomColumnPropertyType customColumnPropertyType)
            throws QueryException {
        FilterOperation operation = operation(filter);
        Path<?> path = context.path().getPath(customColumnPropertyType.getName());
        if (customColumnPropertyType.getValue() == null) {
            if (operation.isAnyEqualOperation()) {
                return ExpressionUtils.predicate(Ops.IS_NULL, path);
            } else {
                throw new QueryException("Null value for other than EQUAL filter: " + filter);
            }
        }

        return singleValuePredicateWithNotTreated(path, operation, customColumnPropertyType.getValue());
    }
}
