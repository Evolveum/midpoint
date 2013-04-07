/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression.functions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

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
