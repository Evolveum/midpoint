/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.match;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.match.MatchingRule;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Matching rule for LDAP distinguished name (DN).
 *
 * @author Radovan Semancik
 *
 */
public class DistinguishedNameMatchingRule implements MatchingRule<String> {

    @Override
    public QName getName() {
        return PrismConstants.DISTINGUISHED_NAME_MATCHING_RULE_NAME;
    }

    @Override
    public boolean supports(QName xsdType) {
        return (DOMUtil.XSD_STRING.equals(xsdType));
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.model.match.MatchingRule#match(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean match(String a, String b) throws SchemaException {
        if (StringUtils.isBlank(a) && StringUtils.isBlank(b)) {
            return true;
        }
        if (StringUtils.isBlank(a) || StringUtils.isBlank(b)) {
            return false;
        }
        LdapName dnA;
        try {
            dnA = new LdapName(a);
        } catch (InvalidNameException e) {
            throw new SchemaException("String '"+a+"' is not a DN: "+e.getMessage(), e);
        }
        LdapName dnB;
        try {
            dnB = new LdapName(b);
        } catch (InvalidNameException e) {
            throw new SchemaException("String '"+b+"' is not a DN: "+e.getMessage(), e);
        }
        return dnA.equals(dnB);
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.prism.match.MatchingRule#normalize(java.lang.Object)
     */
    @Override
    public String normalize(String original) throws SchemaException {
        if (StringUtils.isBlank(original)) {
            return null;
        }
        LdapName dn;
        try {
            dn = new LdapName(original);
        } catch (InvalidNameException e) {
            throw new SchemaException("String '"+original+"' is not a DN: "+e.getMessage(), e);
        }
        return StringUtils.lowerCase(dn.toString());
    }

    @Override
    public boolean matchRegex(String a, String regex) throws SchemaException {

        a = normalize(a);

        if (a == null){
            return false;
        }

        // Simple case-insensitive match
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(a);
        return matcher.matches();
    }

}
