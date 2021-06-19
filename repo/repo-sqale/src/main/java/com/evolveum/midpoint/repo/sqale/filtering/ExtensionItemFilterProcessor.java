/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import static com.querydsl.core.types.dsl.Expressions.booleanTemplate;
import static com.querydsl.core.types.dsl.Expressions.stringTemplate;

import static com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemCardinality.SCALAR;

import java.util.function.Function;

import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItem;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SinglePathItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.JsonbPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.prism.xml.ns._public.types_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Filter processor for extension items stored in JSONB.
 * This takes care of any supported type, scalar or array, and handles any operation.
 */
public class ExtensionItemFilterProcessor
        extends SinglePathItemFilterProcessor<Object, JsonbPath> {

    // QName.toString produces different results, QNameUtil must be used here:
    public static final String STRING_TYPE = QNameUtil.qNameToUri(DOMUtil.XSD_STRING);
    public static final String INT_TYPE = QNameUtil.qNameToUri(DOMUtil.XSD_INT);
    public static final String INTEGER_TYPE = QNameUtil.qNameToUri(DOMUtil.XSD_INTEGER);
    public static final String SHORT_TYPE = QNameUtil.qNameToUri(DOMUtil.XSD_SHORT);
    public static final String LONG_TYPE = QNameUtil.qNameToUri(DOMUtil.XSD_LONG);
    public static final String DECIMAL_TYPE = QNameUtil.qNameToUri(DOMUtil.XSD_DECIMAL);
    public static final String DOUBLE_TYPE = QNameUtil.qNameToUri(DOMUtil.XSD_DOUBLE);
    public static final String FLOAT_TYPE = QNameUtil.qNameToUri(DOMUtil.XSD_FLOAT);
    public static final String BOOLEAN_TYPE = QNameUtil.qNameToUri(DOMUtil.XSD_BOOLEAN);
    public static final String DATETIME_TYPE = QNameUtil.qNameToUri(DOMUtil.XSD_DATETIME);
    public static final String POLY_STRING_TYPE = QNameUtil.qNameToUri(PolyStringType.COMPLEX_TYPE);
    public static final String REF_TYPE = QNameUtil.qNameToUri(ObjectReferenceType.COMPLEX_TYPE);

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
        assert definition != null;

        ValueFilterValues<?, ?> values = ValueFilterValues.from(filter);
        // TODO where do we want tu support eq with multiple values?
        Ops operator = operation(filter);

        if (values.isEmpty()) {
            if (operator == Ops.EQ || operator == Ops.EQ_IGNORE_CASE) {
                // ?? is "escaped" ? operator, PG JDBC driver understands it. Alternative is to use
                // function jsonb_exists but that does NOT use GIN index, only operators do!
                // We have to use parenthesis with AND shovelled into the template like this.
                return booleanTemplate("({0} ?? {1} AND {0} is not null)",
                        path, extItem.id.toString()).not();
            } else {
                throw new QueryException("Null value for other than EQUAL filter: " + filter);
            }
        }

        if (extItem.valueType.equals(STRING_TYPE)) {
            return processString(extItem, values, operator, filter);
        } else if (SqaleUtils.isEnumDefinition(definition)) {
            return processEnum(extItem, values, operator, filter);
        } else if (extItem.valueType.equals(INT_TYPE) || extItem.valueType.equals(INTEGER_TYPE)
                || extItem.valueType.equals(LONG_TYPE) || extItem.valueType.equals(SHORT_TYPE)
                || extItem.valueType.equals(DOUBLE_TYPE) || extItem.valueType.equals(FLOAT_TYPE)
                || extItem.valueType.equals(DECIMAL_TYPE)) {
            return processNumeric(extItem, values, operator, filter);
        } else if (extItem.valueType.equals(BOOLEAN_TYPE)) {
            return processBoolean(extItem, values, operator, filter);
        }

        // TODO other types
        // TODO enum using instanceof

        throw new QueryException("Unsupported filter for extension item: " + filter);
    }

    private Predicate processString(
            MExtItem extItem, ValueFilterValues<?, ?> values, Ops operator, ObjectFilter filter)
            throws QueryException {
        if (extItem.cardinality == SCALAR) {
            if (operator == Ops.EQ) {
                return predicateWithNotTreated(path, booleanTemplate("{0} @> {1}::jsonb", path,
                        String.format("{\"%d\":\"%s\"}", extItem.id, values.singleValue())));
            } else {
                // {1s} means "as string", this is replaced before JDBC driver, just as path is,
                // but for path types it's automagic, integer would turn to param and ?.
                // IMPORTANT: To get string from JSONB we want to use ->> or #>>'{}' operators,
                // that properly escape the value. Using ::TEXT cast would return the string with
                // double-quotes. For more: https://dba.stackexchange.com/a/234047/157622
                return singleValuePredicate(stringTemplate("{0}->>'{1s}'", path, extItem.id),
                        operator, values.singleValue());
            }
        } else { // multi-value
            if (operator == Ops.EQ) {
                return predicateWithNotTreated(path, booleanTemplate("{0} @> {1}::jsonb", path,
                        String.format("{\"%d\":[\"%s\"]}", extItem.id, values.singleValue())));
            } else {
                throw new QueryException("Only equals without matching rules is supported for"
                        + " multi-value string extensions; used filter: " + filter);
            }
        }
    }

    private Predicate processEnum(
            MExtItem extItem, ValueFilterValues<?, ?> values, Ops operator, ObjectFilter filter)
            throws QueryException {
        if (operator != Ops.EQ) {
            throw new QueryException(
                    "Only equals is supported for enum extensions; used filter: " + filter);
        }

        return predicateWithNotTreated(path, booleanTemplate("{0} @> {1}::jsonb", path,
                String.format(
                        extItem.cardinality == SCALAR ? "{\"%d\":\"%s\"}" : "{\"%d\":[\"%s\"]}",
                        extItem.id, values.singleValue())));
    }

    private Predicate processNumeric(
            MExtItem extItem, ValueFilterValues<?, ?> values, Ops operator, ObjectFilter filter)
            throws QueryException {
        if (extItem.cardinality == SCALAR) {
            if (operator == Ops.EQ) {
                return predicateWithNotTreated(path, booleanTemplate("{0} @> {1}::jsonb", path,
                        String.format("{\"%d\":%s}", extItem.id, values.singleValue())));
            } else {
                // {1s} means "as string", this is replaced before JDBC driver, just as path is,
                // but for path types it's automagic, integer would turn to param and ?.
                return singleValuePredicate(stringTemplate("({0}->'{1s}')::numeric", path, extItem.id),
                        operator, values.singleValue());
            }
        } else { // multi-value
            if (operator == Ops.EQ) {
                return predicateWithNotTreated(path, booleanTemplate("{0} @> {1}::jsonb", path,
                        String.format("{\"%d\":[%s]}", extItem.id, values.singleValue())));
            } else {
                throw new QueryException("Only equals is supported for"
                        + " multi-value numeric extensions; used filter: " + filter);
            }
        }
    }

    private Predicate processBoolean(
            MExtItem extItem, ValueFilterValues<?, ?> values, Ops operator, ObjectFilter filter)
            throws QueryException {
        if (operator != Ops.EQ) {
            throw new QueryException(
                    "Only equals is supported for boolean extensions; used filter: " + filter);
        }

        // array for booleans doesn't make any sense, but whatever...
        return predicateWithNotTreated(path, booleanTemplate("{0} @> {1}::jsonb", path,
                String.format(
                        extItem.cardinality == SCALAR ? "{\"%d\":%s}" : "{\"%d\":[%s]}",
                        extItem.id, values.singleValue())));
    }
}
