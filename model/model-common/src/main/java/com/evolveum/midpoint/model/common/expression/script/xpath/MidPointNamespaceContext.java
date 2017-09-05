/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.model.common.expression.script.xpath;

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
