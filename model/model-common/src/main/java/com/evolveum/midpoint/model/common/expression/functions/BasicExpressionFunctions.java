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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Library of standard midPoint functions. These functions are made available to all
 * midPoint expressions.
 * 
 * The functions should be written to support scripting-like comfort. It means that they all needs
 * to be null-safe, automatically convert data types as necessary and so on.
 * 
 * @author Radovan Semancik
 *
 */
public class BasicExpressionFunctions {
	
	public static final String NAME_SEPARATOR = " ";
	
	public static final Trace LOGGER = TraceManager.getTrace(BasicExpressionFunctions.class);
	
	private PrismContext prismContext;
	private Protector protector;

	public BasicExpressionFunctions(PrismContext prismContext, Protector protector) {
		super();
		this.prismContext = prismContext;
		this.protector = protector;
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
	 * Each argument is stringified, trimmed and the result is concatenated by spaces.
	 */
	public String concatName(Object... components) {
		if (components == null || components.length == 0) {
			return "";
		}
		boolean endsWithSeparator = false;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < components.length; i++) {
			Object component = components[i];
			if (component == null) {
				continue;
			}
			String stringComponent = stringify(component);
			if (stringComponent == null) {
				continue;
			}
			String trimmedStringComponent = trim(stringComponent);
			if (trimmedStringComponent.isEmpty()) {
				continue;
			}
			sb.append(trimmedStringComponent);
			if (i < (components.length - 1)) {
				sb.append(NAME_SEPARATOR);
				endsWithSeparator = true;
			} else {
				endsWithSeparator = false;
			}
		}
		if (endsWithSeparator) {
			sb.delete(sb.length() - NAME_SEPARATOR.length(), sb.length());
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
		if (orig == null){
			return null;
		}
		PolyString polyString = new PolyString(orig);
		polyString.recompute(prismContext.getDefaultPolyStringNormalizer());
		return polyString.getNorm();
	}

	/**
	 * Converts whatever it gets to a string. But it does it in a sensitive way.
	 * E.g. it tries to detect collections and returns the first element (if there is only one). 
	 * Never returns null. Returns empty string instead. 
	 */
	public String stringify(Object whatever) {
		
		if (whatever == null) {
			return "";
		}
		
		if (whatever instanceof Collection) {
			Collection collection = (Collection)whatever;
			if (collection.isEmpty()) {
				return "";
			}
			if (collection.size() > 1) {
				throw new IllegalArgumentException("Cannot stringify collection because it has "+collection.size()+" values");
			}
			whatever = collection.iterator().next();
		}
		
		Class<? extends Object> whateverClass = whatever.getClass();
		if (whateverClass.isArray()) {
			Object[] array = (Object[])whatever;
			if (array.length == 0) {
				return "";
			}
			if (array.length > 1) {
				throw new IllegalArgumentException("Cannot stringify array because it has "+array.length+" values");
			}
			whatever = array[0];
		}
		
		if (whatever == null) {
			return "";
		}
		
		if (whatever instanceof String) {
			return (String)whatever;
		}
		
		if (whatever instanceof PolyString) {
			return ((PolyString)whatever).getOrig();
		}

		if (whatever instanceof PolyStringType) {
			return ((PolyStringType)whatever).getOrig();
		}
		
		if (whatever instanceof Element) {
			Element element = (Element)whatever;
			Element origElement = DOMUtil.getChildElement(element, PolyString.F_ORIG);
			if (origElement != null) {
				// This is most likely a PolyStringType
				return origElement.getTextContent();
			} else {
				return element.getTextContent();
			}
		}
		
		if (whatever instanceof Node) {
			return ((Node)whatever).getTextContent();
		}

		return whatever.toString();
	}
	
	public boolean isEmpty(Object whatever) {
		if (whatever == null) {
			return true;
		}
		if (whatever instanceof String) {
			return ((String)whatever).isEmpty();
		}
		if (whatever instanceof Collection) {
			return ((Collection)whatever).isEmpty();
		}
		String whateverString = stringify(whatever);
		if (whateverString == null) {
			return true;
		}
		return whateverString.isEmpty();
	}
	
	public <T> Collection<T> getExtensionPropertyValues(ObjectType object, String namespace, String localPart) {
		return getExtensionPropertyValues(object, new javax.xml.namespace.QName(namespace, localPart));
	}
	
	public <T> Collection<T> getExtensionPropertyValues(ObjectType object, groovy.xml.QName propertyQname) {
		return getExtensionPropertyValues(object, propertyQname.getNamespaceURI(), propertyQname.getLocalPart());
	}
	
	public <T> Collection<T> getExtensionPropertyValues(ObjectType object, javax.xml.namespace.QName propertyQname) {
		return ObjectTypeUtil.getExtensionPropertyValuesNotNull(object, propertyQname);
	}
	

	public <T> T getExtensionPropertyValue(ObjectType object, String namespace, String localPart) throws SchemaException {
		return getExtensionPropertyValue(object, new javax.xml.namespace.QName(namespace, localPart));
	}
	
	public <T> T getExtensionPropertyValue(ObjectType object, groovy.xml.QName propertyQname) throws SchemaException {
		return getExtensionPropertyValue(object, propertyQname.getNamespaceURI(), propertyQname.getLocalPart());
	}
	
	public <T> T getExtensionPropertyValue(ObjectType object, javax.xml.namespace.QName propertyQname) throws SchemaException {
		if (object == null) {
			return null;
		}
		Collection<T> values = ObjectTypeUtil.getExtensionPropertyValues(object, propertyQname);
		return toSingle(values, "a multi-valued extension property "+propertyQname);
	}
	
	public <T> T getPropertyValue(ObjectType object, String path) throws SchemaException {
		Collection<T> values = getPropertyValues(object, path);
		return toSingle(values, "a multi-valued property "+path);
	}
	
	public <T> Collection<T> getPropertyValues(ObjectType object, String path) {
		if (object == null) {
			return null;
		}
		ScriptExpressionEvaluationContext scriptContext = ScriptExpressionEvaluationContext.getThreadLocal();
		ScriptExpression scriptExpression = scriptContext.getScriptExpression();
		ItemPath itemPath = scriptExpression.parsePath(path);
		PrismProperty property = object.asPrismObject().findProperty(itemPath);
		if (property == null) {
			return new ArrayList<T>(0);
		}
		return property.getRealValues();
	}
	
	
	public <T> Collection<T> getAttributeValues(ShadowType shadow, String attributeNamespace, String attributeLocalPart) {
		return getAttributeValues(shadow, new javax.xml.namespace.QName(attributeNamespace, attributeLocalPart));
	}
	
	public <T> Collection<T> getAttributeValues(ShadowType shadow, String attributeLocalPart) {
		return getAttributeValues(shadow, new javax.xml.namespace.QName(MidPointConstants.NS_RI, attributeLocalPart));
	}
	
	public <T> Collection<T> getAttributeValues(ShadowType shadow, groovy.xml.QName attributeQname) {
		return getAttributeValues(shadow, attributeQname.getNamespaceURI(), attributeQname.getLocalPart());
	}
	
	public <T> Collection<T> getAttributeValues(ShadowType shadow, javax.xml.namespace.QName attributeQname) {
		return ShadowUtil.getAttributeValues(shadow, attributeQname);
	}

	public <T> T getAttributeValue(ShadowType shadow, String attributeNamespace, String attributeLocalPart) throws SchemaException {
		return getAttributeValue(shadow, new javax.xml.namespace.QName(attributeNamespace, attributeLocalPart));
	}

	public <T> T getAttributeValue(ShadowType shadow, String attributeLocalPart) throws SchemaException {
		return getAttributeValue(shadow, new javax.xml.namespace.QName(MidPointConstants.NS_RI, attributeLocalPart));
	}

	public <T> T getAttributeValue(ShadowType shadow, groovy.xml.QName attributeQname) throws SchemaException {
		return getAttributeValue(shadow, attributeQname.getNamespaceURI(), attributeQname.getLocalPart());
	}
	
	public <T> T getAttributeValue(ShadowType shadow, javax.xml.namespace.QName attributeQname) throws SchemaException {
		return ShadowUtil.getAttributeValue(shadow, attributeQname);
	}
	
	public Collection<String> getAttributeStringValues(ShadowType shadow, String attributeNamespace, String attributeLocalPart) {
		return getAttributeStringValues(shadow, new javax.xml.namespace.QName(attributeNamespace, attributeLocalPart));
	}
	
	public Collection<String> getAttributeStringValues(ShadowType shadow, groovy.xml.QName attributeQname) {
		return getAttributeStringValues(shadow, attributeQname.getNamespaceURI(), attributeQname.getLocalPart());
	}
	
	public Collection<String> getAttributeStringValues(ShadowType shadow, javax.xml.namespace.QName attributeQname) {
		return ShadowUtil.getAttributeValues(shadow, attributeQname, String.class);
	}
	
	public <T> T getIdentifierValue(ShadowType shadow) throws SchemaException {
		if (shadow == null) {
			return null;
		}
		Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getIdentifiers(shadow);
		if (identifiers.size() == 0) {
			return null;
		}
		if (identifiers.size() > 1) {
			throw new SchemaException("More than one idenfier in "+shadow);
		}
		Collection<T> realValues = (Collection<T>) identifiers.iterator().next().getRealValues();
		if (realValues.size() == 0) {
			return null;
		}
		if (realValues.size() > 1) {
			throw new SchemaException("More than one idenfier value in "+shadow);
		}
		return realValues.iterator().next();
	}

	public <T> T getSecondaryIdentifierValue(ShadowType shadow) throws SchemaException {
		if (shadow == null) {
			return null;
		}
		Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getSecondaryIdentifiers(shadow);
		if (identifiers.size() == 0) {
			return null;
		}
		if (identifiers.size() > 1) {
			throw new SchemaException("More than one secondary idenfier in "+shadow);
		}
		Collection<T> realValues = (Collection<T>) identifiers.iterator().next().getRealValues();
		if (realValues.size() == 0) {
			return null;
		}
		if (realValues.size() > 1) {
			throw new SchemaException("More than one secondary idenfier value in "+shadow);
		}
		return realValues.iterator().next();
	}

	public String determineLdapSingleAttributeValue(Collection<String> dns, String attributeName, PrismProperty attribute) throws NamingException {
		return determineLdapSingleAttributeValue(dns, attributeName, attribute.getRealValues());
	}
	
	public <T> T getResourceIcfConfigurationPropertyValue(ResourceType resource, javax.xml.namespace.QName propertyQname) throws SchemaException {
		if (propertyQname == null) {
			return null;
		}
		PrismContainer<?> configurationProperties = getIcfConfigurationProperties(resource);
		if (configurationProperties == null) {
			return null;
		}
		PrismProperty<T> property = configurationProperties.findProperty(propertyQname);
		if (property == null) {
			return null;
		}
		return property.getRealValue();
	}
	
	public <T> T getResourceIcfConfigurationPropertyValue(ResourceType resource, String propertyLocalPart) throws SchemaException {
		if (propertyLocalPart == null) {
			return null;
		}
		PrismContainer<?> configurationProperties = getIcfConfigurationProperties(resource);
		if (configurationProperties == null) {
			return null;
		}
		for (PrismProperty<?> property: configurationProperties.getValue().getProperties()) {
			if (propertyLocalPart.equals(property.getElementName().getLocalPart())) {
				return (T) property.getRealValue();
			}
		}
		return null;
	}
	
	private PrismContainer<?> getIcfConfigurationProperties(ResourceType resource) {
		if (resource == null) {
			return null;
		}
		PrismContainer<?> connectorConfiguration = resource.asPrismObject().findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		if (connectorConfiguration == null) {
			return null;
		}
		return connectorConfiguration.findContainer(SchemaConstants.ICF_CONFIGURATION_PROPERTIES);
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
	
	public <T> T toSingle(Collection<T> values) throws SchemaException {
		if (values == null || values.isEmpty()) {
			return null;
		} else if (values.size() > 1) {
			throw new SchemaException("Attempt to get single value from a multi-valued property");
		} else {
			return values.iterator().next();
		}
	}
	
	private <T> T toSingle(Collection<T> values, String contextDesc) throws SchemaException {
		if (values == null || values.isEmpty()) {
			return null;
		} else if (values.size() > 1) {
			throw new SchemaException("Attempt to get single value from " + contextDesc);
		} else {
			return values.iterator().next();
		}
	}

    public static String readFile(String filename) throws IOException {
        return FileUtils.readFileToString(new File(filename));
    }
    
    public String formatDateTime(String format, XMLGregorianCalendar xmlCal) {
    	if (xmlCal == null || format == null) {
    		return null;
    	}
    	SimpleDateFormat sdf = new SimpleDateFormat(format);
    	Date date = XmlTypeConverter.toDate(xmlCal);
		return sdf.format(date);
    }
    
    public String formatDateTime(String format, Long millis) {
    	if (millis == null || format == null) {
    		return null;
    	}
    	SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(millis);
    }
    
    public XMLGregorianCalendar parseDateTime(String format, String stringDate) throws ParseException {
    	if (format == null || stringDate == null) {
    		return null;
    	}
    	String[] formats = new String[]{format};
		Date date = DateUtils.parseDate(stringDate, formats);
		if (date == null) {
			return null;
		}
		return XmlTypeConverter.createXMLGregorianCalendar(date);
    }
    
    public XMLGregorianCalendar currentDateTime() {
    	return XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
    }
    
    public String decrypt(ProtectedStringType protectedString) {
    	try {
			return protector.decryptString(protectedString);
		} catch (EncryptionException e) {
			throw new SystemException(e.getMessage(), e);
		}
    }
    
    public ProtectedStringType encrypt(String string) {
    	try {
			return protector.encryptString(string);
		} catch (EncryptionException e) {
			throw new SystemException(e.getMessage(), e);
		}
    }
	
}
