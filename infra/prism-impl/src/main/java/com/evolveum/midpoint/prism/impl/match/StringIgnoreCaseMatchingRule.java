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
 * String matching rule that ignores the case.
 *
 * @author Radovan Semancik
 *
 */
public class StringIgnoreCaseMatchingRule implements MatchingRule<String> {

    @Override
    public QName getName() {
        return PrismConstants.STRING_IGNORE_CASE_MATCHING_RULE_NAME;
    }

    @Override
    public boolean supports(QName xsdType) {
        return (DOMUtil.XSD_STRING.equals(xsdType));
    }

    @Override
    public boolean match(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return StringUtils.equalsIgnoreCase(a, b);
    }

    @Override
    public String normalize(String original) {
        return StringUtils.lowerCase(original);
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

    @Override
    public String toString() {
        return "StringIgnoreCaseMatchingRule{}";
    }
}
