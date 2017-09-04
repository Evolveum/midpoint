/*
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.prism.match;


import javax.xml.namespace.QName;

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

	public static final QName NAME = new QName(PrismConstants.NS_MATCHING_RULE, "xml");

	@Override
	public QName getName() {
		return NAME;
	}

	@Override
	public boolean isSupported(QName xsdType) {
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
