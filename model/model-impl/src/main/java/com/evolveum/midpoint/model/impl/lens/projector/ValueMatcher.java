/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
public class ValueMatcher<T> {

    private static final Trace LOGGER = TraceManager.getTrace(ValueMatcher.class);

    MatchingRule<T> matchingRule;

    public ValueMatcher(MatchingRule<T> matchingRule) {
        this.matchingRule = matchingRule;
    }

    public static <T> ValueMatcher<T> createMatcher(RefinedAttributeDefinition rAttrDef, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        QName matchingRuleQName = rAttrDef.getMatchingRuleQName();
        MatchingRule<T> matchingRule;
        try {
            matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, rAttrDef.getTypeName());
        } catch (SchemaException e) {
            throw new SchemaException(e.getMessage()+", defined for attribute "+rAttrDef.getItemName(), e);
        }
        return new ValueMatcher<>(matchingRule);
    }

    public static <T> ValueMatcher<T> createDefaultMatcher(QName type, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        MatchingRule<Object> matchingRule = matchingRuleRegistry.getMatchingRule(null, type);
        return new ValueMatcher<>((MatchingRule<T>) matchingRule);
    }

    public boolean match(T realA, T realB) throws SchemaException {
        return matchingRule.match(realA, realB);
    }

    public boolean matches(T realValue, String regex) throws SchemaException{
        return matchingRule.matchRegex(realValue, regex);
    }

    public boolean hasRealValue(PrismProperty<T> property, PrismPropertyValue<T> pValue) {
        for (T existingRealValue: property.getRealValues()) {
            try {
                if (matchingRule.match(existingRealValue, pValue.getValue())) {
//                    LOGGER.trace("MATCH: {} ({}) <-> {} ({}) (rule: {})", new Object[]{
//                            existingRealValue, existingRealValue.getClass(), pValue.getValue(), pValue.getValue().getClass(), matchingRule});
                    return true;
                }
//                LOGGER.trace("NO match: {} ({}) <-> {} ({}) (rule: {})", new Object[]{
//                        existingRealValue, existingRealValue.getClass(), pValue.getValue(), pValue.getValue().getClass(), matchingRule});
            } catch (SchemaException e) {
                // At least one of the values is invalid. But we do not want to throw exception from
                // a comparison operation. That will make the system very fragile. Let's fall back to
                // ordinary equality mechanism instead.
                if (existingRealValue.equals(pValue.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Experimental // FIXME
    public PrismPropertyValue<T> findValue(PrismProperty<T> property, PrismPropertyValue<T> pValue) {
        for (PrismPropertyValue<T> existingValue: property.getValues()) {
            try {
                if (matchingRule.match(existingValue.getRealValue(), pValue.getValue())) {
                    return existingValue;
                }
            } catch (SchemaException e) {
                // At least one of the values is invalid. But we do not want to throw exception from
                // a comparison operation. That will make the system very fragile. Let's fall back to
                // ordinary equality mechanism instead.
                if (existingValue.getRealValue().equals(pValue.getValue())) {
                    return existingValue;
                }
            }
        }
        return null;
    }

    public boolean isRealValueToAdd(PropertyDelta<T> delta, PrismPropertyValue<T> pValue) {
        if (delta.getValuesToAdd() == null){
            return false;
        }
        for (PrismPropertyValue<T> existingPValue: delta.getValuesToAdd()) {
            try {
                if (matchingRule.match(existingPValue.getValue(), pValue.getValue())) {
//                    LOGGER.trace("MATCH: {} ({}) <-> {} ({}) (rule: {})", new Object[]{
//                            existingPValue.getValue(), existingPValue.getValue().getClass(), pValue.getValue(), pValue.getValue().getClass(), matchingRule});
                    return true;
                }
//                LOGGER.trace("NO match: {} ({}) <-> {} ({}) (rule: {})", new Object[]{
//                        existingPValue.getValue(), existingPValue.getValue().getClass(), pValue.getValue(), pValue.getValue().getClass(), matchingRule});
            } catch (SchemaException e) {
                // At least one of the values is invalid. But we do not want to throw exception from
                // a comparison operation. That will make the system very fragile. Let's fall back to
                // ordinary equality mechanism instead.
                if (existingPValue.getValue().equals(pValue.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "ValueMatcher(" + matchingRule + ")";
    }

}
