/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import static com.evolveum.midpoint.prism.PrismConstants.*;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

public abstract class ComparativeFilterImpl<T> extends PropertyValueFilterImpl<T> implements PropertyValueFilter<T> {

    private boolean equals;

    ComparativeFilterImpl(@NotNull ItemPath path,
            @Nullable PrismPropertyDefinition<T> definition,
            @Nullable QName matchingRule,
            @Nullable PrismPropertyValue<T> value,
            @Nullable ExpressionWrapper expression, @Nullable ItemPath rightHandSidePath,
            @Nullable ItemDefinition rightHandSideDefinition, boolean equals) {
        super(path, definition, matchingRule,
                value != null ? Collections.singletonList(value) : null,
                expression, rightHandSidePath, rightHandSideDefinition);
        this.equals = equals;
    }

    public boolean isEquals() {
        return equals;
    }

    public void setEquals(boolean equals) {
        this.equals = equals;
    }

    @Nullable
    static <T> PrismPropertyValue<T> anyValueToPropertyValue(@NotNull PrismContext prismContext, Object value) {
        List<PrismPropertyValue<T>> values = anyValueToPropertyValueList(prismContext, value);
        if (values.isEmpty()) {
            return null;
        } else if (values.size() > 1) {
            throw new UnsupportedOperationException("Comparative filter with more than one value is not supported");
        } else {
            return values.iterator().next();
        }
    }

    @Override
    public boolean equals(Object o, boolean exact) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o, exact)) return false;
        ComparativeFilterImpl<?> that = (ComparativeFilterImpl<?>) o;
        return equals == that.equals;
    }

    // Just to make checkstyle happy
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public boolean match(PrismContainerValue object, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        Collection<PrismValue> objectItemValues = getObjectItemValues(object);
        Collection<? extends PrismValue> filterItemValues = emptyIfNull(getValues());
        if (filterItemValues.isEmpty()) {
            throw new SchemaException("Couldn't evaluate the comparison: the value is missing");
        } else if (filterItemValues.size() > 1) {
            throw new SchemaException("Couldn't evaluate the comparison: there is more than one value: " + filterItemValues);
        }
        PrismValue filterValue = filterItemValues.iterator().next();

        MatchingRule<?> matchingRule = getMatchingRuleFromRegistry(matchingRuleRegistry);
        checkPrismPropertyValue(filterValue);
        for (Object objectItemValue : objectItemValues) {
            checkPrismPropertyValue(objectItemValue);
            if (matches((PrismPropertyValue<?>) objectItemValue, (PrismPropertyValue<?>) filterValue, matchingRule)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(PrismPropertyValue<?> objectItemValue, PrismPropertyValue<?> filterValue, MatchingRule<?> matchingRule) throws SchemaException {
        assert filterValue != null;
        assert objectItemValue != null;

        Object filterRealValue = filterValue.getRealValue();
        Object objectRealValue = objectItemValue.getRealValue();
        assert filterRealValue != null;
        assert objectRealValue != null;

        if (filterRealValue instanceof Number && objectRealValue instanceof Number) {
            return matchNumbers((Number) objectRealValue, (Number) filterRealValue);
        } else if (isStringLike(filterRealValue) && isStringLike(objectRealValue)) {
            return matchStringLike(objectRealValue, filterRealValue, matchingRule);
        } else if (filterRealValue instanceof XMLGregorianCalendar && objectRealValue instanceof XMLGregorianCalendar) {
            return matchForDateTime((XMLGregorianCalendar) objectRealValue, (XMLGregorianCalendar) filterRealValue);
        } else {
            throw new SchemaException("Couldn't compare incompatible/unsupported types: filter: " + filterRealValue.getClass() +
                    ", object: " + objectRealValue.getClass());
        }
    }

    private boolean isStringLike(Object value) {
        return value instanceof String || value instanceof PolyString || value instanceof PolyStringType;
    }

    private boolean matchNumbers(Number object, Number filter) {
        return matchForBigDecimal(toBigDecimal(object), toBigDecimal(filter));
    }

    private boolean matchStringLike(Object object, Object filter, MatchingRule<?> matchingRule) throws SchemaException {
        if (object instanceof String) {
            return matchForString((String) object, toString(filter), matchingRule);
        } else if (object instanceof PolyString) {
            return matchForPolyString((PolyString) object, toPolyString(filter), matchingRule);
        } else if (object instanceof PolyStringType) {
            return matchForPolyString(PolyString.toPolyString((PolyStringType) object), toPolyString(filter), matchingRule);
        } else {
            throw new AssertionError(object);
        }
    }

    private String toString(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof PolyString) {
            return ((PolyString) value).getOrig();
        } else if (value instanceof PolyStringType) {
            return ((PolyStringType) value).getOrig();
        } else {
            return String.valueOf(value);
        }
    }

    private PolyString toPolyString(Object value) {
        if (value instanceof PolyString) {
            return (PolyString) value;
        } else if (value instanceof PolyStringType) {
            return PolyString.toPolyString((PolyStringType) value);
        } else {
            PolyString polyString = PolyString.fromOrig(toString(value));
            PrismContext prismContext = getPrismContext();
            if (prismContext != null) {
                polyString.recompute(prismContext.getDefaultPolyStringNormalizer());
            }
            return polyString;
        }
    }

    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        } else if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        } else if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
            return BigDecimal.valueOf(number.longValue());
        } else if (number instanceof Float || number instanceof Double) {
            return BigDecimal.valueOf(number.doubleValue());
        } else {
            throw new UnsupportedOperationException("Comparing numbers of type " + number.getClass() + " is not supported.");
        }
    }

    // TODO Rewrite by adding comparison method(s) directly to the matching rule
    private boolean matchForPolyString(PolyString object, PolyString filter, MatchingRule<?> matchingRule) throws SchemaException {
        QName ruleName = matchingRule != null ? matchingRule.getName() : null;
        if (ruleName == null || DEFAULT_MATCHING_RULE_NAME.equals(ruleName) ||
                POLY_STRING_STRICT_MATCHING_RULE_NAME.equals(ruleName)) {
            return matchForString(object.getOrig(), filter.getOrig(), null) &&
                    matchForString(object.getNorm(), filter.getNorm(), null);
        } else if (POLY_STRING_ORIG_MATCHING_RULE_NAME.equals(ruleName)) {
            return matchForString(object.getOrig(), filter.getOrig(), null);
        } else if (POLY_STRING_NORM_MATCHING_RULE_NAME.equals(ruleName)) {
            return matchForString(object.getNorm(), filter.getNorm(), null);
        } else {
            throw new SchemaException("Unsupported matching rule for comparing polystrings: " + ruleName);
        }
    }

    // TODO Rewrite by adding comparison method(s) directly to the matching rule
    private boolean matchForString(String object, String filter, MatchingRule<?> matchingRule) throws SchemaException {
        QName ruleName = matchingRule != null ? matchingRule.getName() : null;
        if (ruleName == null || DEFAULT_MATCHING_RULE_NAME.equals(ruleName)) {
            return matchForString(object, filter);
        } else if (STRING_IGNORE_CASE_MATCHING_RULE_NAME.equals(ruleName)) {
            return matchForString(object.toLowerCase(), filter.toLowerCase());
        } else {
            throw new SchemaException("Unsupported matching rule for comparing strings: " + ruleName);
        }
    }

    private boolean matchForString(String object, String filter) {
        return processComparisonResult(object.compareTo(filter));
    }

    private boolean matchForBigDecimal(BigDecimal object, BigDecimal filter) {
        return processComparisonResult(object.compareTo(filter));
    }

    private boolean matchForDateTime(XMLGregorianCalendar object, XMLGregorianCalendar filter) {
        int comparison = object.compare(filter);
        if (comparison == DatatypeConstants.LESSER) {
            return processComparisonResult(-1);
        } else if (comparison == DatatypeConstants.EQUAL) {
            return processComparisonResult(0);
        } else if (comparison == DatatypeConstants.GREATER) {
            return processComparisonResult(1);
        } else if (comparison == DatatypeConstants.INDETERMINATE) {
            return false; // This is questionable. But 'false' is formally correct answer here.
        } else {
            throw new IllegalStateException("Unexpected XMLGregorianCalendar comparison result for " + object + " vs " +
                    filter + ": " + comparison);
        }
    }

    abstract boolean processComparisonResult(int result);

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), equals);
    }

    @Override
    abstract public PropertyValueFilterImpl clone();
}
