/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.match;

import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * @author semancik
 *
 */
public class MatchingRuleRegistryImpl implements MatchingRuleRegistry {

    @NotNull private final MatchingRule<?> defaultMatchingRule;
    @NotNull private final Map<QName, MatchingRule<?>> matchingRules = new HashMap<>();

    MatchingRuleRegistryImpl() {
        this.defaultMatchingRule = new DefaultMatchingRule<>();
    }

    // if typeQName is null, we skip the rule-type correspondence test
    @Override
    @NotNull
    public <T> MatchingRule<T> getMatchingRule(QName ruleName, QName typeQName) throws SchemaException {
        if (ruleName == null) {
            //noinspection unchecked
            return (MatchingRule<T>) defaultMatchingRule;
        }
        MatchingRule<T> matchingRule = findMatchingRule(ruleName);
        if (typeQName == null || matchingRule.supports(typeQName)) {
            return matchingRule;
        } else {
            throw new SchemaException("Matching rule " + ruleName + " does not support type " + typeQName);
        }
    }

    @NotNull
    private <T> MatchingRule<T> findMatchingRule(QName ruleName) throws SchemaException {
        //noinspection unchecked
        MatchingRule<T> rule = (MatchingRule<T>) matchingRules.get(ruleName);
        if (rule != null) {
            return rule;
        }

        // try match according to the localPart
        QName qualifiedRuleName = QNameUtil.resolveNs(ruleName, matchingRules.keySet());
        if (qualifiedRuleName != null) {
            //noinspection unchecked
            MatchingRule<T> ruleAfterQualification = (MatchingRule<T>) matchingRules.get(qualifiedRuleName);
            if (ruleAfterQualification != null) {
                return ruleAfterQualification;
            }
        }

        throw new SchemaException("Couldn't find matching rule named '" + ruleName + "'");
    }

    void registerMatchingRule(MatchingRule<?> rule) {
        matchingRules.put(rule.getName(), rule);
    }
}
