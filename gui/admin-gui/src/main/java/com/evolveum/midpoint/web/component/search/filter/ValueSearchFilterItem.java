/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.filter;

import java.io.Serializable;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.collections.CollectionUtils;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.prism.query.builder.S_ConditionEntry;
import com.evolveum.midpoint.web.component.search.Property;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author honchar
 */
public class ValueSearchFilterItem<V extends PrismValue, D extends ItemDefinition, O extends ObjectType> implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String F_VALUE = "value";
    public static final String F_FILTER_NAME = "filterName";
    public static final String F_APPLY_NEGATION = "applyNegation";
    public static final String F_FILTER = "filter";
    public static final String F_MATCHING_RULE = "matchingRule";
    public static final String F_PROPERTY_NAME = "propertyName";
    public static final String F_PROPERTY_PATH = "propertyPath";

    public enum FilterName {
        EQUAL("EQUAL"),
        GREATER_OR_EQUAL("GREATER-OR-EQUAL"),
        GREATER("GREATER"),
        LESS_OR_EQUAL("LESS-OR-EQUAL"),
        LESS("LESS"),
        REF("REF"),
        SUBSTRING("SUBSTRING"),
        SUBSTRING_ANCHOR_START("SUBSTRING_ANCHOR_START"),
        SUBSTRING_ANCHOR_END("SUBSTRING_ANCHOR_END");
//        SUBSTRING_ANCHOR_START_AND_END("SUBSTRING_ANCHOR_START_AND_END"); //seems repeats usual substring

        private String filterName;

        FilterName(String filterName) {
            this.filterName = filterName;
        }

        public String getFilterName() {
            return filterName;
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
    private ObjectFilter filter;
    private FilterName filterName = FilterName.EQUAL;
    private MatchingRule matchingRule = null;
    private String propertyName;
    private QName propertyPath;
    private Object value;
    ItemDefinition propertyDef;

    public ValueSearchFilterItem(ObjectFilter filter, boolean applyNegation) {
        this.filter = filter;
        this.applyNegation = applyNegation;
        if (filter instanceof ValueFilter) {
            propertyName = ((ValueFilter) filter).getElementName().toString();
            propertyPath = ((ValueFilter) filter).getElementName();
            propertyDef = ((ValueFilter) filter).getDefinition();
            value = CollectionUtils.isNotEmpty(((ValueFilter) filter).getValues()) ?
                    ((ValueFilter) filter).getValues().get(0) : null;
        }
    }

    public ValueSearchFilterItem(Property property, boolean applyNegation) {
        propertyName = property.getDefinition().getItemName().toString();
        propertyPath = property.getDefinition().getItemName();
        propertyDef = property.getDefinition();
        this.applyNegation = applyNegation;
    }

    public boolean isApplyNegation() {
        return applyNegation;
    }

    public void setApplyNegation(boolean applyNegation) {
        this.applyNegation = applyNegation;
    }

    public ObjectFilter getFilter() {
        return filter;
    }

    public void setFilter(ObjectFilter filter) {
        this.filter = filter;
    }

    //todo which filter types do we want to support here
    public Object getValue() {
        if (value instanceof PrismValue) {
            return ((PrismValue) value).getRealValue();
        }
        return null;
    }

    public FilterName getFilterName() {
        return filterName;
    }

    public void setFilterName(FilterName filterName) {
        this.filterName = filterName;
    }

    public MatchingRule getMatchingRule() {
        return matchingRule;
    }

    public void setMatchingRule(MatchingRule matchingRule) {
        this.matchingRule = matchingRule;
        if (filter instanceof ValueFilter){
            ((ValueFilter) filter).setMatchingRule(matchingRule.getMatchingRuleName());
        }
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public QName getPropertyPath() {
        return propertyPath;
    }

    public void setPropertyPath(QName propertyPath) {
        this.propertyPath = propertyPath;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public ItemDefinition getPropertyDef() {
        return propertyDef;
    }

    public void setPropertyDef(ItemDefinition propertyDef) {
        this.propertyDef = propertyDef;
    }

    public QName getElementName(){
        if (filter instanceof ValueFilter) {
            return ((ValueFilter) filter).getElementName();
        }
        return null;
    }

    public ObjectFilter buildFilter(PrismContext prismContext, Class<O> type){
        S_ConditionEntry conditionEntry = prismContext.queryFor(type).item(propertyPath);
        ObjectFilter builtFilter = null;
        if (FilterName.EQUAL.equals(filterName)) {
            builtFilter = conditionEntry.eq(value).buildFilter();
        } else if (FilterName.GREATER.equals(filterName)) {
            builtFilter = conditionEntry.gt(value).buildFilter();
        } else if (FilterName.GREATER_OR_EQUAL.equals(filterName)) {
            builtFilter = conditionEntry.ge(value).buildFilter();
        } else if (FilterName.LESS.equals(filterName)) {
            builtFilter = conditionEntry.lt(value).buildFilter();
        } else if (FilterName.LESS_OR_EQUAL.equals(filterName)) {
            builtFilter = conditionEntry.le(value).buildFilter();
        } else if (FilterName.REF.equals(filterName) && value != null) {
            ObjectReferenceType refVal = (ObjectReferenceType) value;
            //todo do we need to separately create refType and refRelation ?
//            if (StringUtils.isNotEmpty(refVal.getOid())){
//
//            }
            builtFilter = conditionEntry.ref(refVal.asReferenceValue()).buildFilter();
        } else if (FilterName.SUBSTRING.equals(filterName)) {
            builtFilter = conditionEntry.contains(value).buildFilter();
        } else if (FilterName.SUBSTRING_ANCHOR_START.equals(filterName)) {
            builtFilter = conditionEntry.startsWith(value).buildFilter();
        } else if (FilterName.SUBSTRING_ANCHOR_END.equals(filterName)) {
            builtFilter = conditionEntry.endsWith(value).buildFilter();
        }
        return builtFilter != null ? builtFilter : prismContext.queryFor(type).buildFilter();
    }
}
