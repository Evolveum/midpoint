/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.CollectionUtils;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.S_ConditionEntry;
import com.evolveum.midpoint.web.component.search.Property;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author honchar
 */
public class ValueSearchFilterItem<V extends PrismValue, D extends ItemDefinition<?>, O extends ObjectType> implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String F_VALUE = "value";
    public static final String F_FILTER_TYPE_NAME = "filterTypeName";
    public static final String F_APPLY_NEGATION = "applyNegation";
    public static final String F_FILTER = "filter";
    public static final String F_MATCHING_RULE = "matchingRule";
    public static final String F_PROPERTY_NAME = "propertyName";
    public static final String F_PROPERTY_PATH = "propertyPath";

    public enum FilterName {
        EQUAL(EqualFilter.class),
        GREATER_OR_EQUAL(GreaterFilter.class),
        GREATER(GreaterFilter.class),
        LESS_OR_EQUAL(LessFilter.class),
        LESS(LessFilter.class),
        REF(RefFilter.class),
        SUBSTRING(SubstringFilter.class),
        SUBSTRING_ANCHOR_START(SubstringFilter.class),
        SUBSTRING_ANCHOR_END(SubstringFilter.class);
//        SUBSTRING_ANCHOR_START_AND_END("SUBSTRING_ANCHOR_START_AND_END"); //seems repeats usual substring

        private Class<? extends ValueFilter> filterType;

        FilterName(Class<? extends ValueFilter> filterType) {
            this.filterType = filterType;
        }

        public Class<? extends ValueFilter> getFilterType() {
            return filterType;
        }

        public static <F extends ValueFilter> FilterName findFilterName(F filter) {
            if (filter instanceof LessFilter && ((LessFilter) filter).isEquals()) {
                return FilterName.LESS_OR_EQUAL;
            } else if (filter instanceof GreaterFilter && ((GreaterFilter) filter).isEquals()) {
                return FilterName.GREATER_OR_EQUAL;
            } else if (filter instanceof SubstringFilter && ((SubstringFilter) filter).isAnchorStart() && !((SubstringFilter) filter).isAnchorEnd()) {
                return FilterName.SUBSTRING_ANCHOR_START;
            } else if (filter instanceof SubstringFilter && ((SubstringFilter) filter).isAnchorEnd() && !((SubstringFilter) filter).isAnchorStart()) {
                return FilterName.SUBSTRING_ANCHOR_END;
            } else {
                return findFilterName(filter.getClass());
            }
        }

        public static FilterName findFilterName(Class<? extends ValueFilter> filterType) {
            for (FilterName filterName : values()) {
                if (filterName.getFilterType().equals(filterType.getInterfaces()[0])) {
                    return filterName;
                }
            }
            return null;
        }
    }

    public enum MatchingRule {
        STRING_IGNORE_CASE(PrismConstants.STRING_IGNORE_CASE_MATCHING_RULE_NAME),
        POLY_STRING_STRICT(PrismConstants.POLY_STRING_STRICT_MATCHING_RULE_NAME),
        POLY_STRING_ORIG(PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME),
        POLY_STRING_NORM(PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME),
        EXCHANGE_EMAIL_ADDRESSES(PrismConstants.EXCHANGE_EMAIL_ADDRESSES_MATCHING_RULE_NAME),
        DISTINGUISHED_NAME(PrismConstants.DISTINGUISHED_NAME_MATCHING_RULE_NAME),
        XML(PrismConstants.XML_MATCHING_RULE_NAME),
        UUID(PrismConstants.UUID_MATCHING_RULE_NAME),
        DEFAULT(PrismConstants.DEFAULT_MATCHING_RULE_NAME);

        private QName matchingRuleName;

        MatchingRule(QName matchingRuleName) {
            this.matchingRuleName = matchingRuleName;
        }

        public QName getMatchingRuleName() {
            return matchingRuleName;
        }
    }

    private boolean applyNegation;
    private ValueFilter<V, D> filter;
    private FilterName filterTypeName = FilterName.EQUAL;
    private MatchingRule matchingRule = null;
    private String propertyName;
    private ItemPath propertyPath;
    private Object value;
    private ExpressionWrapper expression;
    private ItemDefinition propertyDef;

    public ValueSearchFilterItem(ValueFilter filter, boolean applyNegation) {
        this.filter = filter;
        this.applyNegation = applyNegation;
        this.propertyPath = filter.getDefinition().getItemName();
        this.propertyDef = filter.getDefinition();
        propertyName = WebComponentUtil.getItemDefinitionDisplayNameOrName(propertyDef);
        value = CollectionUtils.isNotEmpty(filter.getValues()) ? filter.getValues().get(0) : null;
        if (propertyDef instanceof PrismReferenceDefinition && value == null) {
            value = new ObjectReferenceType();
        }
        this.expression = filter.getExpression();
        parseFilterName();
    }

    public ValueSearchFilterItem(Property property, boolean applyNegation) {
        propertyName = property.getName();
        propertyPath = property.getFullPath();
        propertyDef = property.getDefinition();
        this.applyNegation = applyNegation;
        if (propertyDef instanceof PrismReferenceDefinition) {
            value = new ObjectReferenceType();
        }
        parseFilterName();
    }

    public boolean isApplyNegation() {
        return applyNegation;
    }

    public void setApplyNegation(boolean applyNegation) {
        this.applyNegation = applyNegation;
    }

    public ValueFilter getFilter() {
        return filter;
    }

    public void setFilter(ValueFilter filter) {
        this.filter = filter;
    }

    public Object getValue() {
        if (value instanceof PrismValue) {
            return ((PrismValue) value).getRealValue();
        }
        return value;
    }

    public FilterName getFilterTypeName() {
        return filterTypeName;
    }

    public void setFilterTypeName(FilterName filterTypeName) {
        this.filterTypeName = filterTypeName;
    }

    public MatchingRule getMatchingRule() {
        return matchingRule;
    }

    public void setMatchingRule(MatchingRule matchingRule) {
        this.matchingRule = matchingRule;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public ItemPath getPropertyPath() {
        return propertyPath;
    }

    public void setPropertyPath(ItemPath propertyPath) {
        this.propertyPath = propertyPath;
    }

    public void setValue(Object value) {
        if (propertyDef.getTypeClass().equals(PolyString.class)) {
            this.value = new PolyString((String) value);
            return;
        }
        this.value = value;
    }

    public ItemDefinition getPropertyDef() {
        return propertyDef;
    }

    public void setPropertyDef(ItemDefinition propertyDef) {
        this.propertyDef = propertyDef;
    }

    public ExpressionWrapper getExpression() {
        return expression;
    }

    public void setExpression(ExpressionWrapper expression) {
        this.expression = expression;
    }

    public ObjectFilter buildFilter(PrismContext prismContext, Class<O> type) {
        S_ConditionEntry conditionEntry = prismContext.queryFor(type).item(propertyPath);
        ObjectFilter builtFilter = null;
        if (FilterName.EQUAL.equals(filterTypeName)) {
            builtFilter = conditionEntry.eq(value).buildFilter();
        } else if (FilterName.GREATER.equals(filterTypeName)) {
            builtFilter = conditionEntry.gt(value).buildFilter();
        } else if (FilterName.GREATER_OR_EQUAL.equals(filterTypeName)) {
            builtFilter = conditionEntry.ge(value).buildFilter();
        } else if (FilterName.LESS.equals(filterTypeName)) {
            builtFilter = conditionEntry.lt(value).buildFilter();
        } else if (FilterName.LESS_OR_EQUAL.equals(filterTypeName)) {
            builtFilter = conditionEntry.le(value).buildFilter();
        } else if (FilterName.REF.equals(filterTypeName)) {
            if (value != null) {
                PrismReferenceValue refVal = null;
                if (value instanceof PrismReferenceValue) {
                    refVal = (PrismReferenceValue) value;
                } else if (value instanceof ObjectReferenceType) {
                    refVal = ((ObjectReferenceType) value).asReferenceValue();
                }
                if (refVal.isEmpty() && expression != null) {
                    builtFilter = conditionEntry.ref(expression).buildFilter();
                } else if (refVal.getParent() instanceof RefFilter) {
                    builtFilter = (RefFilter) refVal.getParent();
                } else {
                    builtFilter = conditionEntry.ref(refVal).buildFilter();
                }
            } else {
                builtFilter = conditionEntry.ref(Collections.emptyList()).buildFilter();
            }
        } else if (FilterName.SUBSTRING.equals(filterTypeName)) {
            builtFilter = conditionEntry.contains(value).buildFilter();
        } else if (FilterName.SUBSTRING_ANCHOR_START.equals(filterTypeName)) {
            builtFilter = conditionEntry.startsWith(value).buildFilter();
        } else if (FilterName.SUBSTRING_ANCHOR_END.equals(filterTypeName)) {
            builtFilter = conditionEntry.endsWith(value).buildFilter();
        }
        if (builtFilter instanceof ValueFilter && matchingRule != null) {
            ((ValueFilter) builtFilter).setMatchingRule(matchingRule.getMatchingRuleName());
        }
        if (builtFilter instanceof ValueFilter && expression != null) {
            ((ValueFilter) builtFilter).setExpression(expression);
        }
        if (isApplyNegation()) {
            builtFilter = prismContext.queryFactory().createNot(builtFilter);
        }
        return builtFilter != null ? builtFilter : prismContext.queryFor(type).buildFilter();
    }

    private void parseFilterName() {
        if (propertyDef instanceof PrismReferenceDefinition) {
            filterTypeName = FilterName.REF;
        } else if (filter != null) {
            filterTypeName = FilterName.findFilterName(filter);
        }
    }



    public List<FilterName> getAvailableFilterNameList() {
        if (propertyDef == null) {
            return Arrays.asList(FilterName.values());
        }
        if (propertyDef instanceof PrismReferenceDefinition) {
            return Collections.singletonList(FilterName.REF);
        } else {
            List<FilterName> filterNames = new ArrayList<>();
            for (FilterName val : FilterName.values()) {
                if (!FilterName.REF.equals(val)) {
                    filterNames.add(val);
                }
            }
            return filterNames;
        }
    }

    public List<MatchingRule> getAvailableMatchingRuleList() {
        List<MatchingRule> matchingRules = new ArrayList<>();
        if (propertyDef == null || propertyDef instanceof PrismReferenceDefinition) {
            matchingRules.addAll(Arrays.asList(MatchingRule.values()));
            return matchingRules;
        }
        if (PolyStringType.class.equals(propertyDef.getTypeClass()) || PolyString.class.equals(propertyDef.getTypeClass())) {
            matchingRules.add(MatchingRule.POLY_STRING_NORM);
            matchingRules.add(MatchingRule.POLY_STRING_ORIG);
            matchingRules.add(MatchingRule.POLY_STRING_STRICT);
        } else {
            matchingRules.add(MatchingRule.STRING_IGNORE_CASE);
            matchingRules.add(MatchingRule.EXCHANGE_EMAIL_ADDRESSES);
            matchingRules.add(MatchingRule.DISTINGUISHED_NAME);
            matchingRules.add(MatchingRule.XML);
            matchingRules.add(MatchingRule.UUID);
            matchingRules.add(MatchingRule.DEFAULT);
        }
        return matchingRules;
    }
}
