/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.match;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.util.DOMUtil;
import org.apache.commons.lang.StringUtils;

import javax.xml.namespace.QName;
import java.util.regex.Pattern;

/**
 * A specific matching rule for Microsoft Exchange EmailAddresses attribute consisting of SMTP:/smtp: prefix and email address.
 * It considers the case in the prefix but ignores the case in the email address.
 *
 * @author Pavol Mederly
 *
 */
public class ExchangeEmailAddressesMatchingRule implements MatchingRule<String> {

    @Override
    public QName getName() {
        return PrismConstants.EXCHANGE_EMAIL_ADDRESSES_MATCHING_RULE_NAME;
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
        a = a.trim();
        b = b.trim();
        if (a.equals(b)) {
            return true;
        }
        String aPrefix = getPrefix(a);
        String aSuffix = getSuffix(a);
        String bPrefix = getPrefix(b);
        String bSuffix = getSuffix(b);
        return StringUtils.equals(aPrefix, bPrefix) && StringUtils.equalsIgnoreCase(aSuffix, bSuffix);
    }

    private String getPrefix(String a) {
        int i = a.indexOf(':');
        if (i < 0) {
            return null;
        } else {
            return a.substring(0, i);
        }
    }

    private String getSuffix(String a) {
        int i = a.indexOf(':');
        if (i < 0) {
            return a;
        } else {
            return a.substring(i+1);
        }
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.prism.match.MatchingRule#normalize(java.lang.Object)
     */
    @Override
    public String normalize(String original) {
        String prefix = getPrefix(original);
        String suffix = StringUtils.lowerCase(getSuffix(original));
        if (prefix == null) {
            return suffix;
        } else {
            return prefix + ":" + suffix;
        }
    }

    @Override
    public boolean matchRegex(String a, String regex) {
        if (a == null) {
            return false;
        }
        return Pattern.matches(regex, a);            // we ignore case-insensitiveness of the email address
    }

}
