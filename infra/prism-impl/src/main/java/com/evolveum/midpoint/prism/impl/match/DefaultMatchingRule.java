/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.match;

import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Matchable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.match.MatchingRule;

/**
 * Default matching rule used as a fall-back if no explicit matching rule is specified.
 * It is simply using java equals() method to match values.
 *
 * @author Radovan Semancik
 */
public class DefaultMatchingRule<T> implements MatchingRule<T> {

    @Override
    public QName getName() {
        return PrismConstants.DEFAULT_MATCHING_RULE_NAME;
    }

    @Override
    public boolean supports(QName xsdType) {
        // We support everything. We are the default.
        return true;
    }

    @Override
    public boolean match(T a, T b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof Matchable && b instanceof Matchable) {
            return ((Matchable)a).match((Matchable)b);
        }
        // Just use plain java equals() method
        return a.equals(b);
    }

    @Override
    public T normalize(T original) {
        return original;
    }

    @Override
    public boolean matchRegex(T a, String regex) {
        String valueToMatch;
        if (a instanceof Matchable){
            return ((Matchable<?>) a).matches(regex);
        } else if (a instanceof String){
            valueToMatch = (String) a;
        } else if (a instanceof Integer){
            valueToMatch = Integer.toString((Integer) a);
        } else {
            valueToMatch = String.valueOf(a);
        }

        return Pattern.matches(regex, valueToMatch);
    }

    @Override
    public String toString() {
        return "DefaultMatchingRule{}";
    }
}
