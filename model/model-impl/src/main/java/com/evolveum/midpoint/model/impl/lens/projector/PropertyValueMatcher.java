/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.util.EqualsChecker;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

/**
 * Helps with the consolidation of attribute and auxiliary object class values.
 *
 * @author semancik
 */
public class PropertyValueMatcher<T> implements EqualsChecker<PrismPropertyValue<T>> {

    private static final Trace LOGGER = TraceManager.getTrace(PropertyValueMatcher.class);

    private final MatchingRule<T> matchingRule;

    private PropertyValueMatcher(MatchingRule<T> matchingRule) {
        this.matchingRule = matchingRule;
    }

    static @NotNull <T> PropertyValueMatcher<T> createMatcher(ShadowSimpleAttributeDefinition<?> rAttrDef, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        QName matchingRuleQName = rAttrDef.getMatchingRuleQName();
        MatchingRule<T> matchingRule;
        try {
            matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, rAttrDef.getTypeName());
        } catch (SchemaException e) {
            throw new SchemaException(e.getMessage()+", defined for attribute "+rAttrDef.getItemName(), e);
        }
        return new PropertyValueMatcher<>(matchingRule);
    }

    @SuppressWarnings("SameParameterValue")
    static <T> PropertyValueMatcher<T> createDefaultMatcher(QName typeName, MatchingRuleRegistry matchingRuleRegistry) {
        return new PropertyValueMatcher<>( matchingRuleRegistry.getMatchingRuleSafe(null, typeName));
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

    @Override
    public boolean test(PrismPropertyValue<T> v1, PrismPropertyValue<T> v2) {
        try {
            return match(v1.getRealValue(), v2.getRealValue());
        } catch (SchemaException e) {
            // We do not want to throw exception from a comparison operation. That would make the system very fragile.
            LOGGER.warn("Couldn't match values: {} and {}: {}", v1, v2, e.getMessage(), e);
            return v1.equals(v2);
        }
    }
}
