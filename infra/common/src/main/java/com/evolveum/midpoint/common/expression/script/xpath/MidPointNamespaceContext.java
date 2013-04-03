/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.common.expression.script.xpath;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.*;
import javax.xml.namespace.NamespaceContext;

/**
 * Used to register namespaces for prefixes for JAXP
 * 
 * @see NamespaceContext
 * 
 * @author Igor Farinic
 */

public class MidPointNamespaceContext implements NamespaceContext {

	private Map<String, String> map = new HashMap<String, String>();

	public MidPointNamespaceContext(Map<String, String> map) {
		this.map = map;
	}

	@Override
	public String getNamespaceURI(String prefix) {
		if (prefix == null)
			throw new IllegalArgumentException("Null prefix");

		String namespace = (String) map.get(prefix);
		if (null != namespace) {
			return namespace;
		}

		return XMLConstants.NULL_NS_URI;
	}

	// This method isn't necessary for XPath processing.
	@Override
	public String getPrefix(String uri) {
		throw new UnsupportedOperationException();
	}

	// This method isn't necessary for XPath processing either.
	@Override
	public Iterator<String> getPrefixes(String uri) {
		throw new UnsupportedOperationException();
	}

}
