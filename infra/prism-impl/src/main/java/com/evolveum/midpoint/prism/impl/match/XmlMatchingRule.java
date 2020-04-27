/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.match;


import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.match.MatchingRule;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * String matching rule that compares strings as XML snippets.
 * The XML comparison is not schema aware. It will not handle
 * QNames in values correctly. The comparison ignores XML formatting
 * (whitespaces between elements).
 *
 * @author Radovan Semancik
 *
 */
public class XmlMatchingRule implements MatchingRule<String> {

    public static final Trace LOGGER = TraceManager.getTrace(XmlMatchingRule.class);

    @Override
    public QName getName() {
        return PrismConstants.XML_MATCHING_RULE_NAME;
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
        try {

            Document docA = DOMUtil.parseDocument(a);
            Document docB = DOMUtil.parseDocument(b);
            return DOMUtil.compareDocument(docA, docB, false, false);

        } catch (IllegalStateException | IllegalArgumentException e) {
            LOGGER.warn("Invalid XML in XML matching rule: {}", e.getMessage());
            // Invalid XML. We do not want to throw the exception from matching rule.
            // So fall back to ordinary string comparison.
            return StringUtils.equals(a, b);
        }
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.prism.match.MatchingRule#normalize(java.lang.Object)
     */
    @Override
    public String normalize(String original) {
        if (original == null) {
            return original;
        }
        try {

            Document doc = DOMUtil.parseDocument(original);
            DOMUtil.normalize(doc, false);
            String out = DOMUtil.printDom(doc, false, true).toString();
            return out.trim();

        } catch (IllegalStateException | IllegalArgumentException e) {
            LOGGER.warn("Invalid XML in XML matching rule: {}", e.getMessage());
            return original.trim();
        }
    }

    @Override
    public boolean matchRegex(String a, String regex) {
        LOGGER.warn("Regular expression matching is not supported for XML data types");
        return false;
    }

}
