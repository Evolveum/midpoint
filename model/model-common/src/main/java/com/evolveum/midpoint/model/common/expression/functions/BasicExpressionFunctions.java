/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.functions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jetbrains.annotations.NotNull;
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
import static java.util.Collections.emptySet;

/**
 * Library of standard midPoint functions. These functions are made available to all
 * midPoint expressions.
 * <p>
 * The functions should be written to support scripting-like comfort. It means that they all needs
 * to be null-safe, automatically convert data types as necessary and so on.
 *
 * @author Radovan Semancik
 */
public class BasicExpressionFunctions {

    public static final String NAME_SEPARATOR = " ";
    private static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;

    public static final Trace LOGGER = TraceManager.getTrace(BasicExpressionFunctions.class);

    /**
     * Special value that is too far in the past. It can be returned from time-based expressions to
     * make sure that the expression is always executed.
     */
    public static final XMLGregorianCalendar LONG_AGO = XmlTypeConverter.createXMLGregorianCalendar(1, 1, 1, 0, 0, 0);

    private static final String STRING_PATTERN_WHITESPACE = "\\s+";
    private static final String STRING_PATTERN_HONORIFIC_PREFIX_ENDS_WITH_DOT = "^(\\S+\\.)$";
    private static final Pattern PATTERN_NICK_NAME = Pattern.compile("^([^\"]*)\"([^\"]+)\"([^\"]*)$");

    private final PrismContext prismContext;
    private final Protector protector;
    private final Clock clock;
    private final SecureRandom secureRandom;

    public BasicExpressionFunctions(PrismContext prismContext, Protector protector, Clock clock) {
        super();
        this.prismContext = prismContext;
        this.protector = protector;
        this.clock = clock;
        this.secureRandom = new SecureRandom();
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
     * Checks whether the given normalized string begins with the specified value
     * by first normalizing it.
     *
     * @param string the string
     * @param value  the value
     * @return true/false
     */
    public boolean startsWith(String string, String value) {
        if (string == null || value == null) {
            return false;
        }
        PolyString polyString = new PolyString(string);
        return startsWith(polyString, value);
    }

    /**
     * Checks whether the given normalized string begins with the specified value.
     *
     * @param polyString the string
     * @param value  the value
     * @return true/false
     */
    public boolean startsWith(PolyString polyString, String value) {
        if (polyString == null || value == null) {
            return false;
        }
        polyString.recompute(prismContext.getDefaultPolyStringNormalizer());
        return polyString.getNorm().startsWith(value);
    }


    /**
     * Checks whether the given normalized string ends with the specified value
     * by first normalizing it.
     *
     * @param string the string
     * @param value  the value
     * @return true/false
     */
    public boolean endsWith(String string, String value) {
        if (string == null || value == null) {
            return false;
        }
        PolyString polyString = new PolyString(string);
        return endsWith(polyString, value);
    }

    /**
     * Checks whether the given normalized string ends with the specified value
     *
     * @param polyString the string
     * @param value  the value
     * @return true/false
     */
    public boolean endsWith(PolyString polyString, String value) {
        if (polyString == null || value == null) {
            return false;
        }
        polyString.recompute(prismContext.getDefaultPolyStringNormalizer());
        return polyString.getNorm().endsWith(value);
    }

    /**
     * Normalize a string value. It follows the default normalization algorithm
     * used for PolyString values.
     *
     * @param orig original value to normalize
     * @return normalized value
     */
    public String norm(String orig) {
        if (orig == null) {
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
        if (orig == null) {
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
        if (orig == null) {
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
            return (String) whatever;
        }

        if (whatever instanceof PolyString) {
            return ((PolyString) whatever).getOrig();
        }

        if (whatever instanceof PolyStringType) {
            return ((PolyStringType) whatever).getOrig();
        }

        if (whatever instanceof Collection) {
            Collection collection = (Collection) whatever;
            if (collection.isEmpty()) {
                return "";
            }
            if (collection.size() > 1) {
                throw new IllegalArgumentException("Cannot stringify collection because it has " + collection.size() + " values");
            }
            whatever = collection.iterator().next();
        }

        Class<? extends Object> whateverClass = whatever.getClass();
        if (whateverClass.isArray()) {
            Object[] array = (Object[]) whatever;
            if (array.length == 0) {
                return "";
            }
            if (array.length > 1) {
                throw new IllegalArgumentException("Cannot stringify array because it has " + array.length + " values");
            }
            whatever = array[0];
        }

        if (whatever == null) {
            return "";
        }

        if (whatever instanceof String) {
            return (String) whatever;
        }

        if (whatever instanceof PolyString) {
            return ((PolyString) whatever).getOrig();
        }

        if (whatever instanceof PolyStringType) {
            return ((PolyStringType) whatever).getOrig();
        }

        if (whatever instanceof Element) {
            Element element = (Element) whatever;
            Element origElement = DOMUtil.getChildElement(element, PolyString.F_ORIG);
            if (origElement != null) {
                // This is most likely a PolyStringType
                return origElement.getTextContent();
            } else {
                return element.getTextContent();
            }
        }

        if (whatever instanceof Node) {
            return ((Node) whatever).getTextContent();
        }

        return whatever.toString();
    }

    public Collection<String> getOids(Collection<ObjectReferenceType> refs) {
        if (refs == null) {
            return null;
        }

        Collection<String> oids = new ArrayList<>();
        for (ObjectReferenceType ort : refs) {
            if (StringUtils.isNotBlank(ort.getOid())) {
                oids.add(ort.getOid());
            } else if (ort.asReferenceValue().getObject() != null && StringUtils.isNotBlank(ort.asReferenceValue().getObject().getOid())) {
                oids.add(ort.asReferenceValue().getObject().getOid());
            }
        }

        return oids;

    }

    public Collection<String> getOids(ObjectReferenceType refs) {
        List<ObjectReferenceType> refList = new ArrayList<>();
        refList.add(refs);
        return getOids(refList);
    }

    public Collection<String> getOids(ObjectType refs) {
        List<String> oid = new ArrayList<>();
        oid.add(refs.getOid());
        return oid;

    }

    public boolean isEmpty(Object whatever) {
        if (whatever == null) {
            return true;
        }
        if (whatever instanceof String) {
            return ((String) whatever).isEmpty();
        }
        if (whatever instanceof Collection) {
            return ((Collection) whatever).isEmpty();
        }
        String whateverString = stringify(whatever);
        if (whateverString == null) {
            return true;
        }
        return whateverString.isEmpty();
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public <T> Collection<T> getExtensionPropertyValues(Containerable containerable, String namespace, String localPart) {
        checkColon(localPart);
        return getExtensionPropertyValues(containerable, new javax.xml.namespace.QName(namespace, localPart));
    }

    public <T> Collection<T> getExtensionPropertyValues(Containerable object, groovy.xml.QName propertyQname) {
        return getExtensionPropertyValues(object, propertyQname.getNamespaceURI(), propertyQname.getLocalPart());
    }

    public <T> Collection<T> getExtensionPropertyValues(Containerable object, javax.xml.namespace.QName propertyQname) {
        return ObjectTypeUtil.getExtensionPropertyValuesNotNull(object, propertyQname);
    }

    public <T> T getExtensionPropertyValue(Containerable containerable, String localPart) throws SchemaException {
        checkColon(localPart);
        return getExtensionPropertyValue(containerable, new javax.xml.namespace.QName(null, localPart));
    }

    public <T> T getExtensionPropertyValue(Containerable containerable, String namespace, String localPart) throws SchemaException {
        checkColon(localPart);
        return getExtensionPropertyValue(containerable, new javax.xml.namespace.QName(namespace, localPart));
    }

    public Referencable getExtensionReferenceValue(ObjectType object, String namespace, String localPart) throws SchemaException {
        checkColon(localPart);
        return getExtensionReferenceValue(object, new javax.xml.namespace.QName(namespace, localPart));
    }

    private void checkColon(String localPart) {
        if (localPart != null && localPart.contains(":")) {
            LOGGER.warn("Colon in QName local part: '{}' -- are you sure?", localPart);
        }
    }

    public <T> T getExtensionPropertyValue(Containerable containerable, groovy.xml.QName propertyQname) throws SchemaException {
        return getExtensionPropertyValue(containerable, propertyQname.getNamespaceURI(), propertyQname.getLocalPart());
    }

    public <T> T getExtensionPropertyValue(Containerable containerable, javax.xml.namespace.QName propertyQname) throws SchemaException {
        if (containerable == null) {
            return null;
        }
        Collection<T> values = ObjectTypeUtil.getExtensionPropertyValues(containerable, propertyQname);
        return toSingle(values, "a multi-valued extension property " + propertyQname);
    }

    public Referencable getExtensionReferenceValue(ObjectType object, javax.xml.namespace.QName propertyQname) throws SchemaException {
        if (object == null) {
            return null;
        }
        Collection<Referencable> values = ObjectTypeUtil.getExtensionReferenceValues(object, propertyQname);
        return toSingle(values, "a multi-valued extension property " + propertyQname);
    }

    public <T> T getPropertyValue(Containerable c, String path) throws SchemaException {
        return getPropertyValue(c, prismContext.itemPathParser().asItemPathType(path));
    }

    public <T> Collection<T> getPropertyValues(Containerable c, String path) {
        return getPropertyValues(c, prismContext.itemPathParser().asItemPathType(path));
    }

    public <T> T getPropertyValue(Containerable c, ItemPathType path) throws SchemaException {
        return c != null ? getPropertyValue(c.asPrismContainerValue(), path) : null;
    }

    public <T> Collection<T> getPropertyValues(Containerable c, ItemPathType path) {
        return c != null ? getPropertyValues(c.asPrismContainerValue(), path) : emptyList();
    }

    public <T> T getPropertyValue(PrismContainerValue<?> pcv, String path) throws SchemaException {
        return getPropertyValue(pcv, prismContext.itemPathParser().asItemPathType(path));
    }

    public <T> T getPropertyValue(PrismContainerValue<?> pcv, ItemPathType path) throws SchemaException {
        Collection<T> values = getPropertyValues(pcv, path);
        return toSingle(values, "a multi-valued property " + path);
    }

    public <T> Collection<T> getPropertyValues(PrismContainerValue<?> pcv, String path) {
        return getPropertyValues(pcv, prismContext.itemPathParser().asItemPathType(path));
    }

    public <T> Collection<T> getPropertyValues(PrismContainerValue<?> pcv, ItemPathType path) {
        if (pcv == null) {
            return emptyList();
        }
        Item<?, ?> item = pcv.findItem(path.getItemPath());
        if (item == null) {
            return new ArrayList<>(0);      // TODO or emptyList?
        }
        //noinspection unchecked
        return (Collection<T>) item.getRealValues();
    }


    public <T> Collection<T> getAttributeValues(ShadowType shadow, String attributeNamespace, String attributeLocalPart) {
        checkColon(attributeLocalPart);
        return getAttributeValues(shadow, new javax.xml.namespace.QName(attributeNamespace, attributeLocalPart));
    }

    public <T> Collection<T> getAttributeValues(ShadowType shadow, String attributeLocalPart) {
        checkColon(attributeLocalPart);
        return getAttributeValues(shadow, new javax.xml.namespace.QName(MidPointConstants.NS_RI, attributeLocalPart));
    }

    public <T> Collection<T> getAttributeValues(ShadowType shadow, groovy.xml.QName attributeQname) {
        return getAttributeValues(shadow, attributeQname.getNamespaceURI(), attributeQname.getLocalPart());
    }

    public <T> Collection<T> getAttributeValues(ShadowType shadow, javax.xml.namespace.QName attributeQname) {
        return ShadowUtil.getAttributeValues(shadow, attributeQname);
    }

    public <T> T getAttributeValue(ShadowType shadow, String attributeNamespace, String attributeLocalPart) throws SchemaException {
        checkColon(attributeLocalPart);
        return getAttributeValue(shadow, new javax.xml.namespace.QName(attributeNamespace, attributeLocalPart));
    }

    public <T> T getAttributeValue(ShadowType shadow, String attributeLocalPart) throws SchemaException {
        checkColon(attributeLocalPart);
        return getAttributeValue(shadow, new javax.xml.namespace.QName(MidPointConstants.NS_RI, attributeLocalPart));
    }

    public <T> T getAttributeValue(ShadowType shadow, groovy.xml.QName attributeQname) throws SchemaException {
        return getAttributeValue(shadow, attributeQname.getNamespaceURI(), attributeQname.getLocalPart());
    }

    public <T> T getAttributeValue(ShadowType shadow, javax.xml.namespace.QName attributeQname) throws SchemaException {
        return ShadowUtil.getAttributeValue(shadow, attributeQname);
    }

    public Collection<String> getAttributeStringValues(ShadowType shadow, String attributeNamespace, String attributeLocalPart) {
        checkColon(attributeLocalPart);
        return getAttributeStringValues(shadow, new javax.xml.namespace.QName(attributeNamespace, attributeLocalPart));
    }

    public Collection<String> getAttributeStringValues(ShadowType shadow, groovy.xml.QName attributeQname) {
        return getAttributeStringValues(shadow, attributeQname.getNamespaceURI(), attributeQname.getLocalPart());
    }

    public Collection<String> getAttributeStringValues(ShadowType shadow, javax.xml.namespace.QName attributeQname) {
        return ShadowUtil.getAttributeValues(shadow, attributeQname, String.class);
    }

    /**
     * Generic method to extract all metadata values pointed-to by given item path (specified as segments).
     * Note: does not support multivalued containers withing the path (e.g. collecting transformation/source/name,
     * where transformation/source is a multivalued container).
     */
    @Experimental
    @NotNull
    public Collection<?> getMetadataValues(PrismValue value, Object... pathSegments) {
        if (value == null) {
            return emptySet();
        } else {
            ItemPath itemPath = ItemPath.create(pathSegments);
            return value.getValueMetadataAsContainer().valuesStream()
                    .map(md -> md.findItem(itemPath))
                    .filter(Objects::nonNull)
                    .flatMap(item -> item.getRealValues().stream())
                    .collect(Collectors.toList());
        }
    }

    @Experimental
    @NotNull
    public Collection<?> getMetadataValues(PrismValue value, String path) {
        return getMetadataValues(value, (Object[]) path.split("/")); // temporary TODO rework this!
    }

    /**
     * Simplified version of getMetadataValue aimed at fetching single-segment extension paths.
     */
    @Experimental
    @NotNull
    public Collection<?> getMetadataExtensionValues(PrismValue value, String itemLocalPart) {
        checkColon(itemLocalPart);
        return getMetadataValues(value, ValueMetadataType.F_EXTENSION, itemLocalPart);
    }

    public void setExtensionRealValues(PrismContainerValue<?> containerValue, Map<String, Object> map) throws SchemaException {
        PrismContainer<Containerable> ext = containerValue.findOrCreateContainer(ObjectType.F_EXTENSION);
        Map<QName, Object> qnameKeyedMap = new HashMap<>();
        map.forEach((uri, value) -> qnameKeyedMap.put(QNameUtil.uriToQName(uri, true), value));
        List<Item<?, ?>> items = ObjectTypeUtil.mapToExtensionItems(qnameKeyedMap, ext.getDefinition(), prismContext);
        for (Item<?, ?> item : items) {
            ext.getValue().addReplaceExisting(item);
        }
    }

    public <T> T getIdentifierValue(ShadowType shadow) throws SchemaException {
        if (shadow == null) {
            return null;
        }
        ResourceAttribute<?> primaryIdentifier =
                MiscUtil.extractSingleton(
                        ShadowUtil.getPrimaryIdentifiers(shadow),
                        () -> new SchemaException("More than one identifier in " + shadow));
        if (primaryIdentifier == null) {
            return null;
        }
        //noinspection unchecked
        return (T) MiscUtil.extractSingleton(
                primaryIdentifier.getRealValues(),
                () -> new SchemaException("More than one identifier value in " + shadow));
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
            throw new SchemaException("More than one secondary idenfier in " + shadow);
        }
        Collection<T> realValues = (Collection<T>) identifiers.iterator().next().getRealValues();
        if (realValues.size() == 0) {
            return null;
        }
        if (realValues.size() > 1) {
            throw new SchemaException("More than one secondary idenfier value in " + shadow);
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
        PrismProperty<T> property = configurationProperties.findProperty(ItemName.fromQName(propertyQname));
        if (property == null) {
            return null;
        }
        return property.getRealValue();
    }

    public <T> T getResourceIcfConfigurationPropertyValue(ResourceType resource, String propertyLocalPart) throws SchemaException {
        checkColon(propertyLocalPart);
        if (propertyLocalPart == null) {
            return null;
        }
        PrismContainer<?> configurationProperties = getIcfConfigurationProperties(resource);
        if (configurationProperties == null) {
            return null;
        }
        for (PrismProperty<?> property : configurationProperties.getValue().getProperties()) {
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
            return determineLdapSingleAttributeValue((String) null, attributeName, values);
        }
        if (dns.size() > 1) {
            throw new IllegalArgumentException("Nore than one value (" + dns.size() + " for dn argument specified");
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
            stringValues = (Collection) values;
        } else if (firstElement instanceof Element) {
            stringValues = new ArrayList<>(values.size());
            for (Object value : values) {
                Element element = (Element) value;
                stringValues.add(element.getTextContent());
            }
        } else {
            throw new IllegalArgumentException("Unexpected value type " + firstElement.getClass());
        }

        if (stringValues.size() == 1) {
            return stringValues.iterator().next();
        }

        if (StringUtils.isBlank(dn)) {
            throw new IllegalArgumentException("No dn argument specified, cannot determine which of " + values.size() + " values to use");
        }

        LdapName parsedDn = new LdapName(dn);
        for (int i = 0; i < parsedDn.size(); i++) {
            Rdn rdn = parsedDn.getRdn(i);
            Attributes rdnAttributes = rdn.toAttributes();
            NamingEnumeration<String> rdnIDs = rdnAttributes.getIDs();
            while (rdnIDs.hasMore()) {
                String rdnID = rdnIDs.next();
                Attribute attribute = rdnAttributes.get(rdnID);
                if (attributeName.equals(attribute.getID())) {
                    for (int j = 0; j < attribute.size(); j++) {
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
        return clock.currentTimeXMLGregorianCalendar();
    }

    public XMLGregorianCalendar fromNow(String timeSpec) {
        return XmlTypeConverter.fromNow(timeSpec);
    }

    public XMLGregorianCalendar addDuration(XMLGregorianCalendar now, Duration duration) {
        if (now == null) {
            return null;
        }
        if (duration == null) {
            return now;
        }
        return XmlTypeConverter.addDuration(now, duration);
    }

    public XMLGregorianCalendar addDuration(XMLGregorianCalendar now, String duration) {
        if (now == null) {
            return null;
        }
        if (duration == null) {
            return now;
        }
        return XmlTypeConverter.addDuration(now, duration);
    }

    public XMLGregorianCalendar addMillis(XMLGregorianCalendar now, long duration) {
        if (now == null) {
            return null;
        }
        if (duration == 0) {
            return now;
        }
        return XmlTypeConverter.addMillis(now, duration);
    }

    public XMLGregorianCalendar roundDownToMidnight(XMLGregorianCalendar in) {
        XMLGregorianCalendar out = XmlTypeConverter.createXMLGregorianCalendar(in);
        out.setTime(0, 0, 0, 0);
        return out;
    }

    public XMLGregorianCalendar roundUpToEndOfDay(XMLGregorianCalendar in) {
        XMLGregorianCalendar out = XmlTypeConverter.createXMLGregorianCalendar(in);
        out.setTime(23, 59, 59, 999);
        return out;
    }

    public XMLGregorianCalendar longAgo() {
        return LONG_AGO;
    }

    private ParsedFullName parseFullName(String fullName) {
        if (StringUtils.isBlank(fullName)) {
            return null;
        }
        String root = fullName.trim();
        ParsedFullName p = new ParsedFullName();

//        LOGGER.trace("(1) root=", root);

        Matcher m = PATTERN_NICK_NAME.matcher(root);
        if (m.matches()) {
            String nickName = m.group(2).trim();
            p.setNickName(nickName);
            root = m.group(1) + " " + m.group(3);
//            LOGGER.trace("nick={}, root={}", nickName, root);
        }

        String[] words = root.split(STRING_PATTERN_WHITESPACE);
        int i = 0;

//        LOGGER.trace("(2) i={}, words={}", i, Arrays.toString(words));

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

//        LOGGER.trace("(3) i={}, words={}", i, Arrays.toString(words));

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

//        LOGGER.trace("(4) i={}, words={}", i, Arrays.toString(words));
//        LOGGER.trace("(4) rootNameWords={}", rootNameWords);

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
     * <p>
     * composeDn("cn","foo","o","bar")
     * composeDn("cn","foo",new Rdn("o","bar"))
     * composeDn(new Rdn("cn","foo"),"ou","baz",new Rdn("o","bar"))
     * composeDn(new Rdn("cn","foo"),"ou","baz","o","bar")
     * composeDn(new Rdn("cn","foo"),new LdapName("ou=baz,o=bar"))
     * composeDn("cn","foo",new LdapName("ou=baz,o=bar"))
     * <p>
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
        if (components.length == 1 && (components[0] instanceof String) && StringUtils.isBlank((String) (components[0]))) {
            return null;
        }
        LinkedList<Rdn> rdns = new LinkedList<>();
        String attrName = null;
        for (Object component : components) {
            if (attrName != null && !(component instanceof String || component instanceof PolyString || component instanceof PolyStringType)) {
                throw new InvalidNameException("Invalid input to composeDn() function: expected string after '" + attrName + "' argument, but got " + MiscUtil.getClass(component));
            }
            if (component instanceof Rdn) {
                rdns.addFirst((Rdn) component);
            } else if (component instanceof PolyString) {
                component = component.toString();
            } else if (component instanceof PolyStringType) {
                component = component.toString();
            }
            if (component instanceof String) {
                if (attrName == null) {
                    attrName = (String) component;
                } else {
                    rdns.addFirst(new Rdn(attrName, (String) component));
                    attrName = null;
                }
            }
            if (component instanceof LdapName) {
                rdns.addAll(0, ((LdapName) component).getRdns());
            }
        }
        LdapName dn = new LdapName(rdns);
        return dn.toString();
    }

    /**
     * Creates a valid LDAP distinguished name from the wide range of components assuming that
     * the last component is a suffix. The method can be invoked in many ways, e.g.:
     * <p>
     * composeDn("cn","foo","o=bar")
     * composeDn(new Rdn("cn","foo"),"ou=baz,o=bar")
     * composeDn(new Rdn("cn","foo"),new LdapName("ou=baz,o=bar"))
     * composeDn("cn","foo",new LdapName("ou=baz,o=bar"))
     * <p>
     * The last element is a complete suffix represented either as String or LdapName.
     * <p>
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
                if (StringUtils.isBlank((String) (components[0]))) {
                    return null;
                } else {
                    return (new LdapName((String) (components[0]))).toString();
                }
            } else if ((components[0] instanceof LdapName)) {
                return ((LdapName) (components[0])).toString();
            } else {
                throw new InvalidNameException("Invalid input to composeDn() function: expected suffix (last element) to be String or LdapName, but it was " + components[0].getClass());
            }
        }
        Object suffix = components[components.length - 1];
        if (suffix instanceof String) {
            suffix = new LdapName((String) suffix);
        }
        if (!(suffix instanceof LdapName)) {
            throw new InvalidNameException("Invalid input to composeDn() function: expected suffix (last element) to be String or LdapName, but it was " + suffix.getClass());
        }
        components[components.length - 1] = suffix;
        return composeDn(components);
    }

    /**
     * Hashes cleartext password in an (unofficial) LDAP password format. Supported algorithms: SSHA, SHA and MD5.
     */
    public String hashLdapPassword(ProtectedStringType protectedString, String alg) throws NoSuchAlgorithmException, EncryptionException {
        if (protectedString == null) {
            return null;
        }
        String clearString = protector.decryptString(protectedString);
        if (clearString == null) {
            return null;
        }
        return hashLdapPassword(clearString.getBytes(UTF8_CHARSET), alg);
    }

    /**
     * Hashes cleartext password in an (unofficial) LDAP password format. Supported algorithms: SSHA, SHA and MD5.
     */
    public String hashLdapPassword(String clearString, String alg) throws NoSuchAlgorithmException {
        if (clearString == null) {
            return null;
        }
        return hashLdapPassword(clearString.getBytes(UTF8_CHARSET), alg);
    }

    /**
     * Hashes cleartext password in an (unofficial) LDAP password format. Supported algorithms: SSHA, SHA and MD5.
     */
    public String hashLdapPassword(byte[] clearBytes, String alg) throws NoSuchAlgorithmException {
        if (clearBytes == null) {
            return null;
        }

        MessageDigest md = null;

        try {
            if (alg.equalsIgnoreCase("SSHA") || alg.equalsIgnoreCase("SHA")) {
                    md = MessageDigest.getInstance("SHA-1");
            } else if ( alg.equalsIgnoreCase("SMD5") || alg.equalsIgnoreCase("MD5") ) {
                md = MessageDigest.getInstance("MD5");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException("Could not find MessageDigest algorithm: "+alg, e);
        }

        if (md == null) {
            throw new NoSuchAlgorithmException("Unsupported MessageDigest algorithm: " + alg);
        }

        byte[] salt = {};
        if (alg.equalsIgnoreCase("SSHA") || alg.equalsIgnoreCase("SMD5")) {
            salt = new byte[8];
            secureRandom.nextBytes(salt);
        }

        md.reset();
        md.update(clearBytes);
        md.update(salt);
        byte[] hash = md.digest();

        byte[] hashAndSalt = new byte[hash.length + salt.length];
        System.arraycopy(hash, 0, hashAndSalt, 0, hash.length);
        System.arraycopy(salt, 0, hashAndSalt, hash.length, salt.length);

        StringBuilder resSb = new StringBuilder(alg.length() + hashAndSalt.length);
        resSb.append('{');
        resSb.append(alg);
        resSb.append('}');
        resSb.append(Base64.getEncoder().encodeToString(hashAndSalt));

        return resSb.toString();
    }

    public static String debugDump(Object o) {
        if (o == null) {
            return "null";
        }
        if (o instanceof ObjectType) {
            return DebugUtil.debugDump(((ObjectType) o).asPrismObject(), 0);
        }
        return DebugUtil.debugDump(o, 0);
    }

    public static String debugDump(Object o, int indent) {
        if (o == null) {
            return "null";
        }
        if (o instanceof ObjectType) {
            return DebugUtil.debugDump(((ObjectType) o).asPrismObject(), indent);
        }
        return DebugUtil.debugDump(o, indent);
    }

    /**
     * Sets "worker threads" task extension property.
     *
     * Do not use for activity-based tasks.
     */
    @Deprecated
    public void setTaskWorkerThreadsLegacy(@NotNull TaskType task, Integer value) throws SchemaException {
        Objects.requireNonNull(task, "task is not specified");
        ObjectTypeUtil.setExtensionPropertyRealValues(prismContext, task.asPrismContainerValue(),
                SchemaConstants.MODEL_EXTENSION_WORKER_THREADS, value);
    }

    /**
     * Sets "worker threads" distribution parameter for the root task activity.
     */
    public void setTaskWorkerThreads(@NotNull TaskType task, Integer value) throws SchemaException {
        Objects.requireNonNull(task, "task is not specified");
        ActivityDefinitionType activity = task.getActivity();
        Objects.requireNonNull(activity, "task has no activity");

        if (activity.getDistribution() == null) {
            if (value == null) {
                return; // we don't want to create empty containers if not needed
            } else {
                activity.setDistribution(new ActivityDistributionDefinitionType(PrismContext.get()));
            }
        }
        activity.getDistribution().setWorkerThreads(value);
        // Maybe we could delete empty distribution container if value is null - but most probably we shouldn't.
    }
}
