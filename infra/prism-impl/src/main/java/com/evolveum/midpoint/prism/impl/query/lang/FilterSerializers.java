/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.query.lang;

import com.evolveum.midpoint.prism.impl.query.AllFilterImpl;
import com.evolveum.midpoint.prism.impl.query.AndFilterImpl;
import com.evolveum.midpoint.prism.impl.query.EqualFilterImpl;
import com.evolveum.midpoint.prism.impl.query.ExistsFilterImpl;
import com.evolveum.midpoint.prism.impl.query.FullTextFilterImpl;
import com.evolveum.midpoint.prism.impl.query.GreaterFilterImpl;
import com.evolveum.midpoint.prism.impl.query.InOidFilterImpl;
import com.evolveum.midpoint.prism.impl.query.LessFilterImpl;
import com.evolveum.midpoint.prism.impl.query.NoneFilterImpl;
import com.evolveum.midpoint.prism.impl.query.NotFilterImpl;
import com.evolveum.midpoint.prism.impl.query.OrFilterImpl;
import com.evolveum.midpoint.prism.impl.query.OrgFilterImpl;
import com.evolveum.midpoint.prism.impl.query.PropertyValueFilterImpl;
import com.evolveum.midpoint.prism.impl.query.RefFilterImpl;
import com.evolveum.midpoint.prism.impl.query.SubstringFilterImpl;
import com.evolveum.midpoint.prism.impl.query.TypeFilterImpl;
import com.evolveum.midpoint.prism.impl.query.UndefinedFilterImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.OrgFilter.Scope;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;


import static com.evolveum.midpoint.prism.query.PrismQuerySerialization.NotSupportedException;
import static com.evolveum.midpoint.prism.impl.query.lang.FilterNames.*;


import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ExpressionWrapper;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismReferenceValue;

public class FilterSerializers {

    private static final QName POLYSTRING_STRICT = PrismConstants.POLY_STRING_STRICT_MATCHING_RULE_NAME;
    private static final QName POLYSTRING_ORIG = PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME;

    private static final Map<Class<? extends ObjectFilter>, FilterSerializer<?>> SERIALIZERS = ImmutableMap
            .<Class<? extends ObjectFilter>, FilterSerializer<?>>builder()
            .put(mapping(AllFilterImpl.class, FilterSerializers::allFilter))
            .put(mapping(ExistsFilterImpl.class, FilterSerializers::existsFilter))
            .put(mapping(FullTextFilterImpl.class, FilterSerializers::fullTextFilter))
            .put(mapping(InOidFilterImpl.class, FilterSerializers::inOidFilter))
            .put(mapping(AndFilterImpl.class, FilterSerializers::andFilter))
            .put(mapping(OrFilterImpl.class, FilterSerializers::orFilter))
            .put(mapping(NoneFilterImpl.class, FilterSerializers::noneFilter))
            .put(mapping(TypeFilterImpl.class, FilterSerializers::typeFilter))
            .put(mapping(UndefinedFilterImpl.class, FilterSerializers::undefinedFilter))
            .put(mapping(GreaterFilterImpl.class, FilterSerializers::greaterFilter))
            .put(mapping(LessFilterImpl.class, FilterSerializers::lessFilter))
            .put(mapping(EqualFilterImpl.class, FilterSerializers::equalFilter))
            .put(mapping(SubstringFilterImpl.class, FilterSerializers::substringFilter))
            .put(mapping(RefFilterImpl.class, FilterSerializers::refFilter))
            .put(mapping(NotFilterImpl.class, FilterSerializers::notFilter))
            .put(mapping(OrgFilterImpl.class, FilterSerializers::orgFilter)).build();

    static void write(ObjectFilter filter, QueryWriter output) throws NotSupportedException {
        FilterSerializer<?> maybeSerializer = SERIALIZERS.get(filter.getClass());
        checkSupported(maybeSerializer != null, "Serialization of %s is not supported", filter.getClass());
        maybeSerializer.castAndWrite(filter, output);
    }

    private static <T extends ObjectFilter> Entry<Class<T>, FilterSerializer<T>> mapping(Class<T> clazz,
            FilterSerializer<T> serializer) {
        return new SimpleEntry<>(clazz, serializer);
    }

    private static void checkSupported(boolean check, String template, Object... args)
            throws NotSupportedException {
        if (!check) {
            throw new NotSupportedException(Strings.lenientFormat(template, args));
        }
    }

    static void orgFilter(OrgFilterImpl source, QueryWriter target) throws NotSupportedException {
        target.writeSelf();
        if (source.isRoot()) {
            target.writeFilterName(IS_ROOT);
        } else {
            checkSupported(Scope.SUBTREE.equals(source.getScope()), "Only subtree scope is supported");
            PrismReferenceValue orgRef = source.getOrgRef();
            target.writeRawValue(orgRef.getOid());
        }
    }

    static void notFilter(NotFilterImpl source, QueryWriter target) throws NotSupportedException {
        ObjectFilter nested = source.getFilter();
        if (nested instanceof EqualFilterImpl<?>) {
            valueFilter(NOT_EQUAL, (EqualFilterImpl<?>) nested, target);
        } else {
            target.writeNegatedFilter(source.getFilter());
        }
    }

    static void allFilter(AllFilterImpl source, QueryWriter target) throws NotSupportedException {
        checkSupported(false, "Filter AllFilterImpl Not Supported");
    }

    static void existsFilter(ExistsFilterImpl source, QueryWriter target) throws NotSupportedException {
        target.writePath(source.getFullPath());

        ObjectFilter nested = source.getFilter();
        if (nested != null) {
            target.writeFilterName(MATCHES);
            target.writeNestedFilter(nested);
        } else {
            target.writeFilterName(EXISTS);
        }
    }

    static void fullTextFilter(FullTextFilterImpl source, QueryWriter target)
            throws NotSupportedException {
        checkSupported(false, "Filter FullTextFilterImpl Not Supported");
        checkExpressionSupported(source.getExpression());
        target.writeSelf();
        target.writeFilterName(FULL_TEXT);
        target.writeRawValues(source.getValues());

    }

    static void inOidFilter(InOidFilterImpl source, QueryWriter target) throws NotSupportedException {
        checkExpressionSupported(source.getExpression());
        target.writeSelf();
        target.writeFilterName(source.isConsiderOwner() ? OWNED_BY_OID : IN_OID);
        target.writeRawValues(source.getOids());
    }

    static void andFilter(AndFilterImpl source, QueryWriter target) throws NotSupportedException {
        Iterator<ObjectFilter> conditions = source.getConditions().iterator();
        while (conditions.hasNext()) {
            ObjectFilter condition = conditions.next();
            if (condition instanceof OrFilter) {
                target.writeNestedFilter(condition);
            } else {
                target.writeFilter(condition);
            }
            if (conditions.hasNext()) {
                target.writeFilterName(AND);
            }
        }
    }

    static void orFilter(OrFilterImpl source, QueryWriter target) throws NotSupportedException {
        Iterator<ObjectFilter> conditions = source.getConditions().iterator();
        while (conditions.hasNext()) {
            ObjectFilter condition = conditions.next();
            if (condition instanceof AndFilter) {
                target.writeNestedFilter(condition);
            } else {
                target.writeFilter(condition);
            }
            if (conditions.hasNext()) {
                target.writeFilterName(OR);
            }
        }
    }

    static void noneFilter(NoneFilterImpl source, QueryWriter target) throws NotSupportedException {
        checkSupported(false, "Filter NoneFilterImpl Not Supported");
    }

    static void typeFilter(TypeFilterImpl source, QueryWriter target) throws NotSupportedException {
        checkSupported(false, "Filter TypeFilterImpl Not Supported");
    }

    static void undefinedFilter(UndefinedFilterImpl source, QueryWriter target)
            throws NotSupportedException {
        checkSupported(false, "Filter UndefinedFilterImpl Not Supported");
    }

    static void greaterFilter(GreaterFilterImpl<?> source, QueryWriter target)
            throws NotSupportedException {
        valueFilter(source.isEquals() ? GREATER_OR_EQUAL : GREATER, source, target);
    }

    static void lessFilter(LessFilterImpl<?> source, QueryWriter target) throws NotSupportedException {
        valueFilter(source.isEquals() ? LESS_OR_EQUAL : LESS, source, target);
    }

    static void equalFilter(EqualFilterImpl<?> source, QueryWriter target) throws NotSupportedException {
        if (isNotExistsFilter(source)) {
            target = target.negated();
            target.writePath(source.getFullPath());
            target.writeFilterName(EXISTS);
        } else if (isPolystringMatchesFilter(source)) {
            polystringMatchesFilter(source, target);
        } else {
            valueFilter(EQUAL, source, target);
        }
    }

    private static void polystringMatchesFilter(EqualFilterImpl<?> source, QueryWriter target) {
        var poly = (PolyString) source.getValues().get(0).getValue();
        QName matchingRule = source.getMatchingRule();
        target.writePath(source.getFullPath());
        target.writeFilterName(MATCHES);
        target.startNestedFilter();
        if (POLYSTRING_ORIG.equals(matchingRule)) {
            writeProperty(target, "orig", poly.getOrig(), false, false);
        } else if (POLYSTRING_STRICT.equals(matchingRule)) {
            writeProperty(target, "orig", poly.getOrig(), false, false);
            writeProperty(target, "norm", poly.getNorm(), false, true);
        } else { // also POLYSTRING_NORM
            writeProperty(target, "norm", poly.getNorm(), false, false);
        }
        target.endNestedFilter();
    }

    private static boolean isPolystringMatchesFilter(EqualFilterImpl<?> source) {
        return source.getValues().size() == 1 && source.getValues().get(0).getRealValue() instanceof PolyString;
    }

    private static boolean isNotExistsFilter(EqualFilterImpl<?> source) {
        return source.getRightHandSidePath() == null && (source.getValues() == null || source.getValues().isEmpty());
    }

    static void substringFilter(SubstringFilterImpl<?> source, QueryWriter target)
            throws NotSupportedException {
        final QName name;
        if (source.isAnchorStart() && source.isAnchorEnd()) {
            name = EQUAL;
        } else if (source.isAnchorStart()) {
            name = STARTS_WITH;
        } else if (source.isAnchorEnd()) {
            name = ENDS_WITH;
        } else {
            name = CONTAINS;
        }
        valueFilter(name, source, target);
    }

    static void refFilter(RefFilterImpl source, QueryWriter target) throws NotSupportedException {
        checkSupported(source.getValues().size() == 1, "Only one reference is supported");
        checkExpressionSupported(source.getExpression());
        target.writePath(source.getFullPath());
        target.writeFilterName(MATCHES);
        target.startNestedFilter();
        for (PrismReferenceValue value : source.getValues()) {
            var oidEmitted = writeProperty(target, "oid", value.getOid(), source.isOidNullAsAny(), false);
            var targetEmitted = writeProperty(target, "type", value.getTargetType(), source.isTargetTypeNullAsAny(),
                    oidEmitted);
            writeProperty(target, "relation", value.getRelation(), true, targetEmitted);
        }
        target.endNestedFilter();
    }

    private static void checkExpressionSupported(@Nullable ExpressionWrapper expression) throws NotSupportedException {
        checkSupported(expression == null, "Expression serialization not supported yet");
    }

    private static boolean writeProperty(QueryWriter target, String path, Object value, boolean skipNull,
            boolean emitAnd) {
        if (skipNull && value == null) {
            return false;
        }
        if (emitAnd) {
            target.writeFilterName(AND);
        }
        target.writePath(path);
        target.writeFilterName(EQUAL);
        target.writeRawValue(value);
        return true;
    }

    private static void valueFilter(QName name, PropertyValueFilterImpl<?> source, QueryWriter target) throws NotSupportedException {
        checkExpressionSupported(source.getExpression());
        target.writePath(source.getFullPath());
        target.writeFilterName(name);
        target.writeMatchingRule(source.getMatchingRule());

        @Nullable
        ItemPath right = source.getRightHandSidePath();
        if(right != null) {
            target.writePath(right);
        } else {
            target.writeValues(source.getValues());
        }
    }

}
