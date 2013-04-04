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
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
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
public class BasicExpressionFunctions {
	
	public static final String NAME_SEPARATOR = " ";
	
	public static final Trace LOGGER = TraceManager.getTrace(BasicExpressionFunctions.class);
	
	private PrismContext prismContext;

	public BasicExpressionFunctions(PrismContext prismContext) {
		super();
		this.prismContext = prismContext;
	}
	
	/**
	 * Convert string to lower case.
	 */
	public static String lc(String orig) {
		return StringUtils.lowerCase(orig);
	}

	/**
	 * Convert string to upper case.
	 */
	public static String uc(String orig) {
		return StringUtils.upperCase(orig);
	}
	
	/**
	 * Remove whitespaces at the beginning and at the end of the string.
	 */
	public static String trim(String orig) {
		return StringUtils.trim(orig);
	}

	/**
	 * Concatenates the arguments to create a name.
	 * Each argument is trimmed and the result is concatenated by spaces.
	 */
	public String concatName(String... components) {
		if (components == null || components.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < components.length; i++) {
			String component = components[i];
			if (component == null) {
				continue;
			}
			sb.append(trim(component));
			if (i < (components.length - 1)) {
				sb.append(NAME_SEPARATOR);
			}
		}
		return sb.toString();
	}

	/**
	 * Normalize a string value. It follows the default normalization algorithm
	 * used for PolyString values.
	 * 
	 * @param orig original value to normalize
	 * @return normalized value
	 */
	public String norm(String orig) {
		PolyString polyString = new PolyString(orig);
		polyString.recompute(prismContext.getDefaultPolyStringNormalizer());
		return polyString.getNorm();
	}
	
	public Collection<Object> getAttributeValues(ShadowType shadow, String attributeNamespace, String attributeLocalPart) {
		return getAttributeValues(shadow, new javax.xml.namespace.QName(attributeNamespace, attributeLocalPart));
	}
	
	public Collection<Object> getAttributeValues(ShadowType shadow, groovy.xml.QName attributeQname) {
		return getAttributeValues(shadow, attributeQname.getNamespaceURI(), attributeQname.getLocalPart());
	}
	
	public Collection<Object> getAttributeValues(ShadowType shadow, javax.xml.namespace.QName attributeQname) {
		return ResourceObjectShadowUtil.getAttributeValues(shadow, attributeQname, Object.class);
	}

	public Collection<String> getAttributeStringValues(ShadowType shadow, String attributeNamespace, String attributeLocalPart) {
		return getAttributeStringValues(shadow, new javax.xml.namespace.QName(attributeNamespace, attributeLocalPart));
	}
	
	public Collection<String> getAttributeStringValues(ShadowType shadow, groovy.xml.QName attributeQname) {
		return getAttributeStringValues(shadow, attributeQname.getNamespaceURI(), attributeQname.getLocalPart());
	}
	
	public Collection<String> getAttributeStringValues(ShadowType shadow, javax.xml.namespace.QName attributeQname) {
		return ResourceObjectShadowUtil.getAttributeValues(shadow, attributeQname, String.class);
	}
			
	public String determineLdapSingleAttributeValue(Collection<String> dns, String attributeName, PrismProperty attribute) throws NamingException {
		return determineLdapSingleAttributeValue(dns, attributeName, attribute.getRealValues());
	}
	
	public String determineLdapSingleAttributeValue(Collection<String> dns, String attributeName, Collection<String> values) throws NamingException {
		if (values == null || values.isEmpty()) {
			// Shortcut. This is maybe the most common case. We want to return quickly and we also need to avoid more checks later.
			return null;
		}
		if (dns == null || dns.isEmpty()) {
			throw new IllegalArgumentException("No dn argument specified");
		}
		if (dns.size() > 1) {
			throw new IllegalArgumentException("Nore than one value ("+dns.size()+" for dn argument specified");
		}
		return determineLdapSingleAttributeValue(dns.iterator().next(), attributeName, values);
	}
		
	// We cannot have Collection<String> here. The generic type information will disappear at runtime and the scripts can pass
	// anything that they find suitable. E.g. XPath is passing elements
	public String determineLdapSingleAttributeValue(String dn, String attributeName, Collection<?> values) throws NamingException {
		if (values == null || values.isEmpty()) {
			return null;
		}
		
		Collection<String> stringValues = null;
		// Determine item type, try to convert to strings
		Object firstElement = values.iterator().next();
		if (firstElement instanceof String) {
			stringValues = (Collection)values;
		} else if (firstElement instanceof Element) {
			stringValues = new ArrayList<String>(values.size());
			for (Object value: values) {
				Element element = (Element)value;
				stringValues.add(element.getTextContent());
			}
		} else {
			throw new IllegalArgumentException("Unexpected value type "+firstElement.getClass());
		}
		
		if (stringValues.size() == 1) {
			return stringValues.iterator().next();
		}
		
		LdapName parsedDn =  new LdapName(dn);
		for (int i=0; i < parsedDn.size(); i++) {
			Rdn rdn = parsedDn.getRdn(i);
			Attributes rdnAttributes = rdn.toAttributes();
			NamingEnumeration<String> rdnIDs = rdnAttributes.getIDs();
			while (rdnIDs.hasMore()) {
				String rdnID = rdnIDs.next();
				Attribute attribute = rdnAttributes.get(rdnID);
				if (attributeName.equals(attribute.getID())) {
					for (int j=0; j < attribute.size(); j++) {
						Object value = attribute.get(j);
						if (stringValues.contains(value)) {
							return (String) value;
						}
					}
				}
			}
		}
		
		// Fallback. No values in DN. Just return the first alphabetically-wise value.
		return Collections.min(stringValues);
	}
	
}
