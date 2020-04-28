/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.match;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.match.MatchingRule;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.DOMUtil;

/**
 * Matching rule for universally unique identifier (UUID).
 *
 * Currently it is (almost) simple case ignore matching.
 *
 * @author Radovan Semancik
 *
 */
public class UuidMatchingRule implements MatchingRule<String> {

    @Override
    public QName getName() {
        return PrismConstants.UUID_MATCHING_RULE_NAME;
    }

    @Override
    public boolean supports(QName xsdType) {
        return (DOMUtil.XSD_STRING.equals(xsdType));
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.model.match.MatchingRule#match(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean match(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return StringUtils.equalsIgnoreCase(a.trim(), b.trim());
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.prism.match.MatchingRule#normalize(java.lang.Object)
     */
    @Override
    public String normalize(String original) {
        if (original == null) {
            return null;
        }
        return StringUtils.lowerCase(original).trim();
    }

    @Override
    public boolean matchRegex(String a, String regex) {
        if (a == null){
            return false;
        }

        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(a);
        return matcher.matches();
    }

}
