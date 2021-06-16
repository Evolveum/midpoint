/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import java.util.function.Function;

import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItem;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SinglePathItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.JsonbPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * Filter processor for extension items stored in JSONB.
 * This takes care of any supported type, scalar or array, and handles any operation.
 */
public class ExtensionItemFilterProcessor
        extends SinglePathItemFilterProcessor<Object, JsonbPath> {

    // QName.toString produces different results, QNameUtil must be used here:
    public static final String STRING_TYPE = QNameUtil.qNameToUri(DOMUtil.XSD_STRING);

    private final MExtItemHolderType holderType;

    public <Q extends FlexibleRelationalPathBase<R>, R> ExtensionItemFilterProcessor(
            SqlQueryContext<?, Q, R> context,
            Function<Q, JsonbPath> rootToExtensionPath,
            MExtItemHolderType holderType) {
        super(context, rootToExtensionPath);

        this.holderType = holderType;
    }

    @Override
    public Predicate process(PropertyValueFilter<Object> filter) throws RepositoryException {
        PrismPropertyDefinition<?> definition = filter.getDefinition();
        MExtItem extItem = ((SqaleQueryContext<?, ?, ?>) context).repositoryContext()
                .resolveExtensionItem(definition, holderType);

        ValueFilterValues<?, ?> values = ValueFilterValues.from(filter);
        Ops operation = operation(filter);

        // If long but monotonous, it can be one method, otherwise throws must be moved inside extracted methods too.
        if (extItem.valueType.equals(STRING_TYPE)) {
            if (operation == Ops.EQ) {
                // TODO ARRAY, should be easy, just adding [ ] around second "%s"2
                return Expressions.booleanTemplate("{0} @> {1}::jsonb", path,
                        String.format("{\"%d\":\"%s\"}", extItem.id, values.singleValue()));
            }
            // TODO other ops
        }

        // TODO other types

        throw new UnsupportedOperationException("Unsupported filter for extension item: " + filter);
    }

}
