/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import static com.querydsl.core.types.dsl.Expressions.booleanTemplate;
import static com.querydsl.core.types.dsl.Expressions.stringTemplate;

import static com.evolveum.midpoint.repo.sqale.jsonb.JsonbUtils.JSONB_POLY_NORM_KEY;
import static com.evolveum.midpoint.repo.sqale.jsonb.JsonbUtils.JSONB_POLY_ORIG_KEY;
import static com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemCardinality.ARRAY;
import static com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemCardinality.SCALAR;
import static com.evolveum.midpoint.repo.sqlbase.filtering.item.PolyStringItemFilterProcessor.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.xml.datatype.XMLGregorianCalendar;

import com.google.common.base.Strings;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.sql.SQLQuery;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.sqale.ExtUtils;
import com.evolveum.midpoint.repo.sqale.ExtensionProcessor;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItem;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.FilterOperation;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemValueFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.PolyStringItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.QuerydslUtils;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Filter processor for extension items stored in JSONB.
 * This takes care of any supported type, scalar or array, and handles any operation.
 *
 * NOTE about NOT treatment:
 * We use the same not treatment for extensions like for other columns resulting in conditions like:
 * `not (u.ext->>'1510' < ? and u.ext->>'1510' is not null)`
 * One might think that the part after AND can be replaced with u.ext ? '1510' to benefit from the GIN index.
 * But `NOT (u.ext ? '...')` is *not* fully complement to the `u.ext ? '...'` (without NOT).
 * It is only fully complement if additional `AND u.ext is not null` is added in which case the index will not be used either.
 * So instead of adding special treatment code for extensions, we just reuse existing predicateWithNotTreated methods.
 */
public class ExtensionItemFilterProcessor extends ItemValueFilterProcessor<ValueFilter<?, ?>> {

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

    private final MExtItemHolderType holderType;
    private final JsonbPath path;

    public ExtensionItemFilterProcessor(
            SqlQueryContext<?, ?, ?> context,
            Function<FlexibleRelationalPathBase<?>, JsonbPath> rootToExtensionPath,
            MExtItemHolderType holderType) {
        super(context);

        this.path = rootToExtensionPath.apply(context.path());
        this.holderType = holderType;
    }

    @Override
    public Predicate process(ValueFilter<?, ?> filter) throws RepositoryException {
        ItemDefinition<?> definition = filter.getDefinition();
        Objects.requireNonNull(definition,
                "Item '" + filter.getPath() + "' without definition used in query.");
        MExtItem extItem = new ExtensionProcessor((SqaleRepoContext) context.repositoryContext())
                .resolveExtensionItem(definition, holderType);
        assert definition != null;
        if (extItem == null) {
            throw new QueryException("Extension item " + definition.getItemName()
                    + " is not indexed, filter: " + filter);
        }

        if (definition instanceof PrismReferenceDefinition) {
            return processReference(extItem, (RefFilter) filter);
        }

        PropertyValueFilter<?> propertyValueFilter = (PropertyValueFilter<?>) filter;
        ValueFilterValues<?, ?> values = ValueFilterValues.from(propertyValueFilter);

        if (filter instanceof FuzzyStringMatchFilter<?>) {
            return processFuzzySearch(extItem, values, (FuzzyStringMatchFilter<?>) filter);
        }

        FilterOperation operation = operation(filter);

        if (values.isEmpty()) {
            if (operation.isAnyEqualOperation()) {
                return extItemIsNull(extItem);
            } else {
                throw new QueryException("Null value for other than EQUAL filter: " + filter);
            }
        }

        if (extItem.valueType.equals(STRING_TYPE)) {
            return processString(extItem, values, operation, filter);
        } else if (extItem.valueType.equals(DATETIME_TYPE)) {
            //noinspection unchecked
            PropertyValueFilter<XMLGregorianCalendar> dateTimeFilter =
                    (PropertyValueFilter<XMLGregorianCalendar>) filter;
            return processString(extItem,
                    ValueFilterValues.from(dateTimeFilter, ExtUtils::extensionDateTime),
                    operation, filter);
        } else if (ExtUtils.isEnumDefinition((PrismPropertyDefinition<?>) definition)) {
            return processEnum(extItem, values, operation, filter);
        } else if (extItem.valueType.equals(INT_TYPE) || extItem.valueType.equals(INTEGER_TYPE)
                || extItem.valueType.equals(LONG_TYPE) || extItem.valueType.equals(SHORT_TYPE)
                || extItem.valueType.equals(DOUBLE_TYPE) || extItem.valueType.equals(FLOAT_TYPE)
                || extItem.valueType.equals(DECIMAL_TYPE)) {
            return processNumeric(extItem, values, operation, filter);
        }

        // TODO for anything lower we don't support multi-value filter yet, but the solution from string can be adapted.
        if (values.isMultiValue()) {
            throw new QueryException(
                    "Multiple values in filter are not supported for extension items: " + filter);
        }

        if (extItem.valueType.equals(BOOLEAN_TYPE)) {
            return processBoolean(extItem, values, operation, filter);
        } else if (extItem.valueType.equals(POLY_STRING_TYPE)) {
            return processPolyString(extItem, values, operation, propertyValueFilter);
        }

        throw new QueryException("Unsupported filter for extension item: " + filter);
    }

    private Predicate processFuzzySearch(
            MExtItem extItem, ValueFilterValues<?, ?> values, FuzzyStringMatchFilter<?> filter)
            throws QueryException {
        if (extItem.cardinality == SCALAR) {
            return fuzzyStringPredicate(filter,
                    stringTemplate("{0}->>'{1s}'", path, extItem.id),
                    values);
        } else if (!values.isMultiValue()) {
            // e.g. for levenshtein: WHERE ... ext ? '421'
            //   AND exists (select 1 from jsonb_array_elements_text(ext->'421') as val
            //      WHERE levenshtein_less_equal(val, 'john', $1) <= $2)
            // This can't use index, but it works. Sparse keys are helped a lot by indexed ext ? key condition.
            SQLQuery<?> subselect = new SQLQuery<>().select(QuerydslUtils.EXPRESSION_ONE)
                    .from(stringTemplate("jsonb_array_elements_text({0}->'{1s}') as val", path, extItem.id))
                    .where(fuzzyStringPredicate(filter, stringTemplate("val"), values));
            return booleanTemplate("{0} ?? '{1s}'", path, extItem.id)
                    .and(subselect.exists());
        } else {
            throw new QueryException("Fuzzy match not supported for multi-value extensions"
                    + " and multiple values on the right-hand; used filter: " + filter);
        }
    }

    private Predicate processReference(MExtItem extItem, RefFilter filter) {
        List<PrismReferenceValue> values = filter.getValues();
        if (values == null || values.isEmpty()) {
            return extItemIsNull(extItem);
        }

        if (values.size() == 1) {
            return processSingleReferenceValue(extItem, filter, values.get(0));
        }

        Predicate predicate = null;
        for (PrismReferenceValue ref : values) {
            predicate = ExpressionUtils.or(predicate,
                    processSingleReferenceValue(extItem, filter, ref));
        }
        return predicate;
    }

    private Predicate processSingleReferenceValue(MExtItem extItem, RefFilter filter, PrismReferenceValue ref) {
        // We always store oid+type+relation, nothing is null or the whole item is null => missing.
        // So if we ask for OID IS NULL or for target type IS NULL we actually ask "item IS NULL".
        if (ref.getOid() == null && !filter.isOidNullAsAny()
                || ref.getTargetType() == null && !filter.isTargetTypeNullAsAny()) {
            return extItemIsNull(extItem);
        }

        Map<String, Object> json = new HashMap<>();
        if (ref.getOid() != null) {
            json.put("o", ref.getOid());
        }
        if (ref.getTargetType() != null) {
            MObjectType objectType = MObjectType.fromTypeQName(ref.getTargetType());
            json.put("t", objectType);
        }
        if (ref.getRelation() == null || !ref.getRelation().equals(PrismConstants.Q_ANY)) {
            Integer relationId = ((SqaleQueryContext<?, ?, ?>) context)
                    .searchCachedRelationId(ref.getRelation());
            json.put("r", relationId);
        } // else relation == Q_ANY, no additional predicate needed

        return predicateWithNotTreated(path,
                booleanTemplate("{0} @> {1}", path, jsonbValue(extItem, json)));
    }

    private Predicate processString(
            MExtItem extItem, ValueFilterValues<?, ?> values, FilterOperation operation, ObjectFilter filter)
            throws QueryException {
        if (operation.isEqualOperation()) {
            return equalPredicate(extItem, values);
        }

        // other non-EQ operations
        if (extItem.cardinality == SCALAR) {
            // {1s} means "as string", this is replaced before JDBC driver, just as path is,
            // but for path types it's automagic, integer would turn to param and ?.
            // IMPORTANT: To get string from JSONB we want to use ->> or #>>'{}' operators,
            // that properly escape the value. Using ::TEXT cast would return the string with
            // double-quotes. For more: https://dba.stackexchange.com/a/234047/157622
            return singleValuePredicateWithNotTreated(stringTemplate("{0}->>'{1s}'", path, extItem.id),
                    operation, values.singleValue());
        } else if (!values.isMultiValue()) {
            // e.g. for substring: WHERE ... ext ? '421'
            //   AND exists (select 1 from jsonb_array_elements_text(ext->'421') as val where val like '%2%')
            // This can't use index, but it works. Sparse keys are helped a lot by indexed ext ? key condition.
            SQLQuery<?> subselect = new SQLQuery<>().select(QuerydslUtils.EXPRESSION_ONE)
                    .from(stringTemplate("jsonb_array_elements_text({0}->'{1s}') as val", path, extItem.id))
                    .where(singleValuePredicate(stringTemplate("val"), operation, values.singleValue()));
            return booleanTemplate("{0} ?? '{1s}'", path, extItem.id)
                    .and(subselect.exists());
        } else {
            throw new QueryException("Non-equal operation not supported for multi-value extensions"
                    + " and multiple values on the right-hand; used filter: " + filter);
        }
    }

    private Predicate processEnum(
            MExtItem extItem, ValueFilterValues<?, ?> values, FilterOperation operation, ObjectFilter filter)
            throws QueryException {
        if (!operation.isEqualOperation()) {
            throw new QueryException(
                    "Only equals is supported for enum extensions; used filter: " + filter);
        }

        return equalPredicate(extItem, values);
    }

    private Predicate processNumeric(
            MExtItem extItem, ValueFilterValues<?, ?> values, FilterOperation operation, ObjectFilter filter)
            throws QueryException {
        if (operation.isEqualOperation()) {
            return equalPredicate(extItem, values);
        }

        // other non-EQ operations
        if (extItem.cardinality == SCALAR) {
            // {1s} means "as string", this is replaced before JDBC driver, just as path is,
            // but for path types it's automagic, integer would turn to param and ?.
            return singleValuePredicateWithNotTreated(
                    stringTemplate("({0}->'{1s}')::numeric", path, extItem.id),
                    operation,
                    values.singleValue());
        } else if (!values.isMultiValue()) {
            // e.g. for substring: WHERE ... ext ? '421'
            //   AND exists (select 1 from jsonb_array_elements(ext->'421') as val where val::numeric > 40)
            // This can't use index, but it works. Sparse keys are helped a lot by indexed ext ? key condition.
            SQLQuery<?> subselect = new SQLQuery<>().select(QuerydslUtils.EXPRESSION_ONE)
                    .from(stringTemplate("jsonb_array_elements({0}->'{1s}') as val", path, extItem.id))
                    .where(singleValuePredicate(stringTemplate("val::numeric"), operation, values.singleValue()));
            return booleanTemplate("{0} ?? '{1s}'", path, extItem.id)
                    .and(subselect.exists());
        } else {
            throw new QueryException("Non-equal operation not supported for multi-value extensions"
                    + " and multiple values on the right-hand; used filter: " + filter);
        }
    }

    private Predicate equalPredicate(MExtItem extItem, ValueFilterValues<?, ?> values) throws QueryException {
        if (values.isMultiValue()) {
            return predicateWithNotTreated(path,
                    booleanTemplate("{0} @> ANY ({1})", path,
                            values.allValues().stream()
                                    .map(v -> jsonbValue(extItem, v))
                                    .toArray(Jsonb[]::new)));
        } else {
            return predicateWithNotTreated(path,
                    booleanTemplate("{0} @> {1}", path, jsonbValue(extItem, values.singleValue())));
        }
    }

    private Predicate processBoolean(
            MExtItem extItem, ValueFilterValues<?, ?> values, FilterOperation operation, ObjectFilter filter)
            throws QueryException {
        if (!operation.isEqualOperation()) {
            throw new QueryException(
                    "Only equals is supported for boolean extensions; used filter: " + filter);
        }

        // We don't really expect array for booleans here.
        return equalPredicate(extItem, values);
    }

    /**
     * Filter should be PropertyValueFilter<PolyString>, but pure Strings are handled fine
     * for orig/norm cases (but not for default/strict).
     */
    private Predicate processPolyString(MExtItem extItem, ValueFilterValues<?, ?> values,
            FilterOperation operation, PropertyValueFilter<?> filter)
            throws QueryException {
        String matchingRule = filter.getMatchingRule() != null
                ? filter.getMatchingRule().getLocalPart() : null;

        if (extItem.cardinality == ARRAY && !operation.isEqualOperation()) {
            return processComplexCases(filter, extItem, operation, matchingRule);
        }

        if (Strings.isNullOrEmpty(matchingRule) || DEFAULT.equals(matchingRule)
                || STRICT.equals(matchingRule) || STRICT_IGNORE_CASE.equals(matchingRule)) {
            // The value here should be poly-string, otherwise it never matches both orig and norm.
            return processPolyStringBoth(extItem, values, operation);
        } else if (ORIG.equals(matchingRule) || ORIG_IGNORE_CASE.equals(matchingRule)) {
            return processPolyStringComponent(extItem,
                    ValueFilterValues.from(filter, PolyStringItemFilterProcessor::extractOrig),
                    JSONB_POLY_ORIG_KEY, operation);
        } else if (NORM.equals(matchingRule) || NORM_IGNORE_CASE.equals(matchingRule)) {
            return processPolyStringComponent(
                    extItem, ValueFilterValues.from(filter, PolyStringItemFilterProcessor::extractNorm),
                    JSONB_POLY_NORM_KEY, operation);
        } else {
            throw new QueryException("Unknown matching rule '" + matchingRule + "'.");
        }
    }

    @SuppressWarnings("DuplicatedCode") // see JsonbPolysPathItemFilterProcessor
    private BooleanExpression processComplexCases(
            PropertyValueFilter<?> filter, MExtItem extItem, FilterOperation operation, String matchingRule)
            throws QueryException {
        ValueFilterValues<?, ?> values = ValueFilterValues.from(filter);
        // e.g. for substring: WHERE ... exists (select 1
        //     from jsonb_to_recordset(ext->'1') as (o text, n text) where n like '%substring%')
        // Optional AND o like '%substring%' is also possible for strict/default matching rule.
        // This can't use index, but it works.
        SQLQuery<?> subselect = new SQLQuery<>().select(QuerydslUtils.EXPRESSION_ONE)
                .from(stringTemplate("jsonb_to_recordset({0}->'{1s}') as (" + JSONB_POLY_ORIG_KEY
                        + " text, " + JSONB_POLY_NORM_KEY + " text)", path, extItem.id));

        if (Strings.isNullOrEmpty(matchingRule) || DEFAULT.equals(matchingRule)
                || STRICT.equals(matchingRule) || STRICT_IGNORE_CASE.equals(matchingRule)) {
            // The value here should be poly-string, otherwise it never matches both orig and norm.
            PolyString polyString = values.singleValuePolyString();
            subselect.where(singleValuePredicate(stringTemplate(JSONB_POLY_ORIG_KEY), operation, polyString.getOrig()))
                    .where(singleValuePredicate(stringTemplate(JSONB_POLY_NORM_KEY), operation, polyString.getNorm()));
        } else if (ORIG.equals(matchingRule) || ORIG_IGNORE_CASE.equals(matchingRule)) {
            subselect.where(singleValuePredicate(stringTemplate(JSONB_POLY_ORIG_KEY), operation,
                    PolyStringItemFilterProcessor.extractOrig(values.singleValue())));
        } else if (NORM.equals(matchingRule) || NORM_IGNORE_CASE.equals(matchingRule)) {
            subselect.where(singleValuePredicate(stringTemplate(JSONB_POLY_NORM_KEY), operation,
                    PolyStringItemFilterProcessor.extractNorm(values.singleValue())));
        } else {
            throw new QueryException("Unknown matching rule '" + matchingRule + "'. Filter: " + filter);
        }

        return subselect.exists();
    }

    private Predicate processPolyStringBoth(
            MExtItem extItem, ValueFilterValues<?, ?> values, FilterOperation operation) {
        PolyString poly = (PolyString) values.singleValueRaw(); // must be Poly here
        assert poly != null; // empty values treated in main process()

        if (operation.isEqualOperation()) {
            return predicateWithNotTreated(path,
                    booleanTemplate("{0} @> {1}", path,
                            jsonbValue(extItem, Map.of(
                                    JSONB_POLY_ORIG_KEY, poly.getOrig(),
                                    JSONB_POLY_NORM_KEY, poly.getNorm()))));
        } else if (extItem.cardinality == SCALAR) {
            return ExpressionUtils.and(
                    singleValuePredicateWithNotTreated(
                            stringTemplate("{0}->'{1s}'->>'{2s}'",
                                    path, extItem.id, JSONB_POLY_ORIG_KEY),
                            operation, poly.getOrig()),
                    singleValuePredicateWithNotTreated(
                            stringTemplate("{0}->'{1s}'->>'{2s}'",
                                    path, extItem.id, JSONB_POLY_NORM_KEY),
                            operation, poly.getNorm()));
        } else {
            throw new AssertionError("Multi-value non-equal filter should not get here.");
        }
    }

    private Predicate processPolyStringComponent(MExtItem extItem,
            ValueFilterValues<?, ?> values, String subKey, FilterOperation operation)
            throws QueryException {
        // Here the values are converted to Strings already
        if (operation.isEqualOperation()) {
            return predicateWithNotTreated(path, booleanTemplate("{0} @> {1}", path,
                    jsonbValue(extItem, Map.of(subKey, Objects.requireNonNull(values.singleValue())))));
        } else if (extItem.cardinality == SCALAR) {
            return singleValuePredicateWithNotTreated(
                    stringTemplate("{0}->'{1s}'->>'{2s}'", path, extItem.id, subKey),
                    operation, values.singleValue());
        } else {
            throw new AssertionError("Multi-value non-equal filter should not get here.");
        }
    }

    private static final String STRING_IGNORE_CASE =
            PrismConstants.STRING_IGNORE_CASE_MATCHING_RULE_NAME.getLocalPart();

    @Override
    protected boolean isIgnoreCaseFilter(ValueFilter<?, ?> filter) {
        String matchingRule = filter.getMatchingRule() != null
                ? filter.getMatchingRule().getLocalPart() : null;

        // The first equals substitutes default super.isIgnoreCaseFilter(), the rest is for polys.
        return STRING_IGNORE_CASE.equals(matchingRule)
                || STRICT_IGNORE_CASE.equals(matchingRule)
                || ORIG_IGNORE_CASE.equals(matchingRule)
                || NORM_IGNORE_CASE.equals(matchingRule);
    }

    private BooleanExpression extItemIsNull(MExtItem extItem) {
        // ?? is "escaped" ? operator, PG JDBC driver understands it. Alternative is to use
        // function jsonb_exists but that does NOT use GIN index, only operators do!
        // We have to use parenthesis with AND shovelled into the template like this to apply NOT to it all.
        return booleanTemplate("({0} ?? '{1s}' AND {0} is not null)",
                path, extItem.id).not();
    }

    /**
     * Creates JSONB value for `@>` (contains) operation.
     * Only one value should be provided, it is wrapped in collection for non-SCALAR items
     * only to match the structure of stored multi-value extension property.
     */
    private Jsonb jsonbValue(MExtItem extItem, Object value) {
        return Jsonb.fromMap(Map.of(extItem.id.toString(),
                extItem.cardinality == SCALAR ? value : List.of(value)));
    }
}
