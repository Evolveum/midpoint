/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.match;

import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 *
 */
public class PolyStringStrictMatchingRule implements MatchingRule<PolyString> {

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.prism.match.MatchingRule#getName()
     */
    @Override
    public QName getName() {
        return PrismConstants.POLY_STRING_STRICT_MATCHING_RULE_NAME;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.prism.match.MatchingRule#isSupported(java.lang.Class, javax.xml.namespace.QName)
     */
    @Override
    public boolean supports(QName xsdType) {
        return (PolyStringType.COMPLEX_TYPE.equals(xsdType));
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.prism.match.MatchingRule#match(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean match(PolyString a, PolyString b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        // Delegate to PolyString.equals(). This does it well. As we want to compare
        // all aspects of polystring here: orig, norm, translations, langs
        return a.equals(b);
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.prism.match.MatchingRule#normalize(java.lang.Object)
     */
    @Override
    public PolyString normalize(PolyString original) {
        return original;
    }

    @Override
    public boolean matchRegex(PolyString a, String regex) {
        if (a == null){
            return false;
        }

        return Pattern.matches(regex, a.getOrig());
    }

}
