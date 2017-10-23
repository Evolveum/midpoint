/*
 * Copyright (c) 2010-2017 Evolveum
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
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import static java.util.Collections.emptyList;

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

	private static String STRING_PATTERN_WHITESPACE = "\\s+";
	private static String STRING_PATTERN_HONORIFIC_PREFIX_ENDS_WITH_DOT = "^(\\S+\\.)$";
	private static Pattern PATTERN_NICK_NAME = Pattern.compile("^([^\"]*)\"([^\"]+)\"([^\"]*)$");

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

	public boolean contains(Object object, Object search) {
		String objectStr = stringify(object);
		if (StringUtils.isEmpty(objectStr)) {
			return false;
		}
		String searchStr = stringify(search);
		if (StringUtils.isEmpty(searchStr)) {
			return false;
		}
		return objectStr.contains(searchStr);
	}

	public boolean containsIgnoreCase(Object object, Object search) {
		String objectStr = stringify(object);
		if (StringUtils.isEmpty(objectStr)) {
			return false;
		}
		String searchStr = stringify(search);
		if (StringUtils.isEmpty(searchStr)) {
			return false;
		}
		return StringUtils.containsIgnoreCase(objectStr, searchStr);
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
	 * Normalize a PolyString value.
	 *
	 * @param orig original value to normalize
	 * @return normalized value
	 */
	public String norm(PolyString orig) {
		if (orig == null){
			return null;
		}
		if (orig.getNorm() != null) {
			return orig.getNorm();
		}
		orig.recompute(prismContext.getDefaultPolyStringNormalizer());
		return orig.getNorm();
	}

	/**
	 * Normalize a PolyStringType value.
	 *
	 * @param orig original value to normalize
	 * @return normalized value
	 */
	public String norm(PolyStringType orig) {
		if (orig == null){
			return null;
		}
		PolyString polyString = orig.toPolyString();
		return norm(polyString);
	}

	public String toAscii(Object input) {
		if (input == null) {
			return null;
		}
		String inputString = stringify(input);
		String decomposed = Normalizer.normalize(inputString, Normalizer.Form.NFKD);
		return decomposed.replaceAll("\\p{M}", "");
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

		if (whatever instanceof String) {
			return (String)whatever;
		}

		if (whatever instanceof PolyString) {
			return ((PolyString)whatever).getOrig();
		}

		if (whatever instanceof PolyStringType) {
			return ((PolyStringType)whatever).getOrig();
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

	public Collection<String> getOids(Collection<ObjectReferenceType> refs){
		if (refs == null){
			return null;
		}

		Collection<String> oids = new ArrayList<String>();
		for (ObjectReferenceType ort : refs){
			if (StringUtils.isNotBlank(ort.getOid())){
				oids.add(ort.getOid());
			} else if (ort.asReferenceValue().getObject() != null && StringUtils.isNotBlank(ort.asReferenceValue().getObject().getOid())){
				oids.add(ort.asReferenceValue().getObject().getOid());
			}
		}

		return oids;

	}

	public Collection<String> getOids(ObjectReferenceType refs){
		List<ObjectReferenceType> refList = new ArrayList<>();
		refList.add(refs);
		return getOids(refList);
	}

	public Collection<String> getOids(ObjectType refs){
		List<String> oid = new ArrayList<>();
		oid.add(refs.getOid());
		return oid;

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

	public Referencable getExtensionReferenceValue(ObjectType object, String namespace, String localPart) throws SchemaException {
		return getExtensionReferenceValue(object, new javax.xml.namespace.QName(namespace, localPart));
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

	public Referencable getExtensionReferenceValue(ObjectType object, javax.xml.namespace.QName propertyQname) throws SchemaException {
		if (object == null) {
			return null;
		}
		Collection<Referencable> values = ObjectTypeUtil.getExtensionReferenceValues(object, propertyQname);
		return toSingle(values, "a multi-valued extension property "+propertyQname);
	}

	public <T> T getPropertyValue(Containerable c, String path) throws SchemaException {
		return getPropertyValue(c, new ItemPathType(path));
	}

	public <T> Collection<T> getPropertyValues(Containerable c, String path) {
		return getPropertyValues(c, new ItemPathType(path));
	}

	public <T> T getPropertyValue(Containerable c, ItemPathType path) throws SchemaException {
		return c != null ? getPropertyValue(c.asPrismContainerValue(), path) : null;
	}

	public <T> Collection<T> getPropertyValues(Containerable c, ItemPathType path) {
		return c != null ? getPropertyValues(c.asPrismContainerValue(), path) : emptyList();
	}

	public <T> T getPropertyValue(PrismContainerValue<?> pcv, String path) throws SchemaException {
		return getPropertyValue(pcv, new ItemPathType(path));
	}

	public <T> T getPropertyValue(PrismContainerValue<?> pcv, ItemPathType path) throws SchemaException {
		Collection<T> values = getPropertyValues(pcv, path);
		return toSingle(values, "a multi-valued property "+path);
	}

	public <T> Collection<T> getPropertyValues(PrismContainerValue<?> pcv, String path) {
		return getPropertyValues(pcv, new ItemPathType(path));
	}

	public <T> Collection<T> getPropertyValues(PrismContainerValue<?> pcv, ItemPathType path) {
		if (pcv == null) {
			return emptyList();
		}
		Item<?,?> item = pcv.findItem(path.getItemPath());
		if (item == null) {
			return new ArrayList<>(0);      // TODO or emptyList?
		}
		//noinspection unchecked
		return (Collection<T>) item.getRealValues();
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
		Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
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
				return (T) property.getAnyRealValue();
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
			return determineLdapSingleAttributeValue((String)null, attributeName, values);
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

		if (StringUtils.isBlank(dn)) {
			throw new IllegalArgumentException("No dn argument specified, cannot determine which of "+values.size()+" values to use");
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

    private ParsedFullName parseFullName(String fullName) {
    	if (StringUtils.isBlank(fullName)) {
    		return null;
    	}
    	String root = fullName.trim();
    	ParsedFullName p = new ParsedFullName();

//    	LOGGER.trace("(1) root=", root);

    	Matcher m = PATTERN_NICK_NAME.matcher(root);
    	if (m.matches()) {
    		String nickName = m.group(2).trim();
    		p.setNickName(nickName);
    		root = m.group(1) + " " + m.group(3);
//    		LOGGER.trace("nick={}, root={}", nickName, root);
    	}

    	String[] words = root.split(STRING_PATTERN_WHITESPACE);
    	int i = 0;

//    	LOGGER.trace("(2) i={}, words={}", i, Arrays.toString(words));

    	StringBuilder honorificPrefixBuilder = new StringBuilder();
    	while (i < words.length && words[i].matches(STRING_PATTERN_HONORIFIC_PREFIX_ENDS_WITH_DOT)) {
    		honorificPrefixBuilder.append(words[i]);
    		honorificPrefixBuilder.append(" ");
    		i++;
    	}
    	if (honorificPrefixBuilder.length() > 0) {
    		honorificPrefixBuilder.setLength(honorificPrefixBuilder.length() - 1);
    		p.setHonorificPrefix(honorificPrefixBuilder.toString());
    	}

//    	LOGGER.trace("(3) i={}, words={}", i, Arrays.toString(words));

    	List<String> rootNameWords = new ArrayList<>();
    	while (i < words.length && !words[i].endsWith(",")) {
    		rootNameWords.add(words[i]);
    		i++;
    	}

    	if (i < words.length && words[i].endsWith(",")) {
    		String word = words[i];
    		i++;
    		if (!word.equals(",")) {
    			word = word.substring(0, word.length() - 1);
    			rootNameWords.add(word);
    		}
    	}

//    	LOGGER.trace("(4) i={}, words={}", i, Arrays.toString(words));
//    	LOGGER.trace("(4) rootNameWords={}", rootNameWords);

    	if (rootNameWords.size() > 1) {
    		p.setFamilyName(rootNameWords.get(rootNameWords.size() - 1));
    		rootNameWords.remove(rootNameWords.size() - 1);
    		p.setGivenName(rootNameWords.get(0));
    		rootNameWords.remove(0);
    		p.setAdditionalName(StringUtils.join(rootNameWords, " "));
    	} else if (rootNameWords.size() == 1) {
    		p.setFamilyName(rootNameWords.get(0));
    	}

    	StringBuilder honorificSuffixBuilder = new StringBuilder();
    	while (i < words.length) {
    		honorificSuffixBuilder.append(words[i]);
    		honorificSuffixBuilder.append(" ");
    		i++;
    	}
    	if (honorificSuffixBuilder.length() > 0) {
    		honorificSuffixBuilder.setLength(honorificSuffixBuilder.length() - 1);
    		p.setHonorificSuffix(honorificSuffixBuilder.toString());
    	}

    	LOGGER.trace("Parsed full name '{}' as {}", fullName, p);

    	return p;
    }

    public String parseGivenName(Object fullName) {
    	ParsedFullName p = parseFullName(stringify(fullName));
    	if (p == null) {
    		return null;
    	} else {
    		return p.getGivenName();
    	}
    }

    public String parseFamilyName(Object fullName) {
    	ParsedFullName p = parseFullName(stringify(fullName));
    	if (p == null) {
    		return null;
    	} else {
    		return p.getFamilyName();
    	}
    }

    public String parseAdditionalName(Object fullName) {
    	ParsedFullName p = parseFullName(stringify(fullName));
    	if (p == null) {
    		return null;
    	} else {
    		return p.getAdditionalName();
    	}
    }

    public String parseNickName(Object fullName) {
    	ParsedFullName p = parseFullName(stringify(fullName));
    	if (p == null) {
    		return null;
    	} else {
    		return p.getNickName();
    	}
    }

    public String parseHonorificPrefix(Object fullName) {
    	ParsedFullName p = parseFullName(stringify(fullName));
    	if (p == null) {
    		return null;
    	} else {
    		return p.getHonorificPrefix();
    	}
    }

    public String parseHonorificSuffix(Object fullName) {
    	ParsedFullName p = parseFullName(stringify(fullName));
    	if (p == null) {
    		return null;
    	} else {
    		return p.getHonorificSuffix();
    	}
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

    /**
     * Creates a valid LDAP distinguished name from the wide range of components. The method
     * can be invoked in many ways, e.g.:
     *
     * composeDn("cn","foo","o","bar")
	 * composeDn("cn","foo",new Rdn("o","bar"))
     * composeDn(new Rdn("cn","foo"),"ou","baz",new Rdn("o","bar"))
     * composeDn(new Rdn("cn","foo"),"ou","baz","o","bar")
	 * composeDn(new Rdn("cn","foo"),new LdapName("ou=baz,o=bar"))
     * composeDn("cn","foo",new LdapName("ou=baz,o=bar"))
     *
     * Note: the DN is not normalized. The case of the attribute names and white spaces are
     * preserved.
     */
    public static String composeDn(Object... components) throws InvalidNameException {
    	if (components == null) {
    		return null;
    	}
    	if (components.length == 0) {
    		return null;
    	}
    	if (components.length == 1 && components[0] == null) {
    		return null;
    	}
    	if (components.length == 1 && (components[0] instanceof String) && StringUtils.isBlank((String)(components[0]))) {
    		return null;
    	}
    	LinkedList<Rdn> rdns = new LinkedList<>();
    	String attrName = null;
    	for (Object component: components) {
    		if (attrName != null && !(component instanceof String || component instanceof PolyString || component instanceof PolyStringType)) {
    			throw new InvalidNameException("Invalid input to composeDn() function: expected string after '"+attrName+"' argument, but got "+component.getClass());
    		}
    		if (component instanceof Rdn) {
    			rdns.addFirst((Rdn)component);
    		} else if (component instanceof PolyString) {
    			component = ((PolyString)component).toString();
    		} else if (component instanceof PolyStringType) {
    			component = ((PolyStringType)component).toString();
    		}
    		if (component instanceof String) {
    			if (attrName == null) {
    				attrName = (String)component;
    			} else {
					rdns.addFirst(new Rdn(attrName, (String)component));
    				attrName = null;
    			}
    		}
    		if (component instanceof LdapName) {
    			rdns.addAll(0,((LdapName)component).getRdns());
    		}
    	}
    	LdapName dn = new LdapName(rdns);
    	return dn.toString();
    }

    /**
     * Creates a valid LDAP distinguished name from the wide range of components assuming that
     * the last component is a suffix. The method can be invoked in many ways, e.g.:
     *
     * composeDn("cn","foo","o=bar")
     * composeDn(new Rdn("cn","foo"),"ou=baz,o=bar")
	 * composeDn(new Rdn("cn","foo"),new LdapName("ou=baz,o=bar"))
     * composeDn("cn","foo",new LdapName("ou=baz,o=bar"))
     *
     * The last element is a complete suffix represented either as String or LdapName.
     *
     * Note: the DN is not normalized. The case of the attribute names and white spaces are
     * preserved.
     */
    public static String composeDnWithSuffix(Object... components) throws InvalidNameException {
    	if (components == null) {
    		return null;
    	}
    	if (components.length == 0) {
    		return null;
    	}
    	if (components.length == 1) {
    		if (components[0] == null) {
    			return null;
    		}
    		if ((components[0] instanceof String)) {
    			if (StringUtils.isBlank((String)(components[0]))) {
    				return null;
    			} else {
    				return (new LdapName((String)(components[0]))).toString();
    			}
    		} else if ((components[0] instanceof LdapName)) {
    			return ((LdapName)(components[0])).toString();
    		} else {
    			throw new InvalidNameException("Invalid input to composeDn() function: expected suffix (last element) to be String or LdapName, but it was "+components[0].getClass());
    		}
    	}
    	Object suffix = components[components.length - 1];
    	if (suffix instanceof String) {
    		suffix = new LdapName((String)suffix);
    	}
    	if (!(suffix instanceof LdapName)) {
    		throw new InvalidNameException("Invalid input to composeDn() function: expected suffix (last element) to be String or LdapName, but it was "+suffix.getClass());
    	}
    	components[components.length - 1] = suffix;
    	return composeDn(components);
    }

    public static String debugDump(Object o) {
    	if (o == null) {
    		return "null";
    	}
    	if (o instanceof ObjectType) {
    		return DebugUtil.debugDump(((ObjectType)o).asPrismObject(), 0);
    	}
    	return DebugUtil.debugDump(o, 0);
    }

    public static String debugDump(Object o, int indent) {
    	if (o == null) {
    		return "null";
    	}
    	if (o instanceof ObjectType) {
    		return DebugUtil.debugDump(((ObjectType)o).asPrismObject(), indent);
    	}
    	return DebugUtil.debugDump(o, indent);
    }

    public static XMLGregorianCalendar fromNow(String timeSpec) {
    	return XmlTypeConverter.fromNow(XmlTypeConverter.createDuration(timeSpec));
    }

}
