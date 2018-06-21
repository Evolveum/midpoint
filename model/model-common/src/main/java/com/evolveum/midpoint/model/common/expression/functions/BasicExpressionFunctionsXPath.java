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
package com.evolveum.midpoint.model.common.expression.functions;

import java.util.Collection;

import javax.naming.NamingException;

import org.w3c.dom.Element;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Library of standard midPoint functions. These functions are made available to all
 * midPoint expressions.
 *
 * @author Radovan Semancik
 *
 */
public class BasicExpressionFunctionsXPath {

	public static final Trace LOGGER = TraceManager.getTrace(BasicExpressionFunctionsXPath.class);

	private BasicExpressionFunctions functions;

	public BasicExpressionFunctionsXPath(BasicExpressionFunctions functions) {
		super();
		this.functions = functions;
	}

	/**
	 * Convert string to lower case.
	 */
	public static String lc(String orig) {
		return BasicExpressionFunctions.lc(orig);
	}

	/**
	 * Convert string to upper case.
	 */
	public static String uc(String orig) {
		return BasicExpressionFunctions.uc(orig);
	}

	/**
	 * Remove whitespaces at the beginning and at the end of the string.
	 */
	public static String trim(String orig) {
		return BasicExpressionFunctions.trim(orig);
	}

	/**
	 * Concatenates the arguments to create a name.
	 * Each argument is trimmed and the result is concatenated by spaces.
	 */
	public String concatName(String... components) {
		return functions.concatName(components);
	}

	/**
	 * Normalize a string value. It follows the default normalization algorithm
	 * used for PolyString values.
	 *
	 * @param orig original value to normalize
	 * @return normalized value
	 */
	public String norm(String orig) {
		return functions.norm(orig);
	}

	public String stringify(Object whatever) {
		return functions.stringify(whatever);
	}

	public String determineLdapSingleAttributeValue(Element dn, String attributeName, Element valueElement) throws NamingException {
		// Trivial case: the value is a single element therefore it has a single value.
		return valueElement.getTextContent();
	}

	public String determineLdapSingleAttributeValue(Collection<String> dns, String attributeName, Element valueElement) throws NamingException {
		// Trivial case: the value is a single element therefore it has a single value.
		return valueElement.getTextContent();
	}

	public String determineLdapSingleAttributeValue(Element dnElement, String attributeName, Collection<String> values) throws NamingException {
		if (values == null || values.isEmpty()) {
			// Shortcut. This is maybe the most common case. We want to return quickly and we also need to avoid more checks later.
			return null;
		}
		if (dnElement == null) {
			throw new IllegalArgumentException("No dn argument specified");
		}
		return functions.determineLdapSingleAttributeValue(dnElement.getTextContent(), attributeName, values);
	}

	public String determineLdapSingleAttributeValue(Collection<String> dns, String attributeName, Collection<String> values) throws NamingException {
		return functions.determineLdapSingleAttributeValue(dns, attributeName, values);
	}

}
