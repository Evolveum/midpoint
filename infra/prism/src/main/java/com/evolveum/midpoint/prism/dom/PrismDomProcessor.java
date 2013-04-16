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
package com.evolveum.midpoint.prism.dom;

import java.io.File;
import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.ReflectionUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 * 
 */
public class PrismDomProcessor {

	private static final String JAVA_PROPERTY_OID = "oid";
	private static final String JAVA_PROPERTY_TYPE = "type";
	private static final String JAVA_PROPERTY_DESCRIPTION = "description";
	private static final String JAVA_PROPERTY_FILTER = "filter";
	private static final String JAVA_JAXB_PROPERTY_ANY = "any";

	private SchemaRegistry schemaRegistry;
	private PrismContext prismContext;

	public PrismDomProcessor(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	private PrismJaxbProcessor getJaxbProcessor() {
		return getPrismContext().getPrismJaxbProcessor();
	}

	public <T extends Objectable> PrismObject<T> parseObject(File file, Class<T> type) throws SchemaException {
		PrismObject<T> object = parseObject(file);
		if (!object.canRepresent(type)) {
			throw new SchemaException("Requested object of type " + type + ", but parsed type "
					+ object.getCompileTimeClass());
		}
		return object;
	}

	public <T extends Objectable> PrismObject<T> parseObject(File file) throws SchemaException {
		Document parsedDocument = DOMUtil.parseFile(file);
		Element element = DOMUtil.getFirstChildElement(parsedDocument);
		if (element == null) {
			return null;
		}
		try {
			return parseObject(element);
		} catch (SchemaException e) {
			throw new SchemaException(e.getMessage() + " while parsing file " + file, e);
		}
	}

	/**
	 * This is really stupid implementation. Parsing long files with DOM does
	 * not make much sense. But it may be OK for tests and such "lightweight"
	 * things.
	 */
	public List<PrismObject<? extends Objectable>> parseObjects(File file) throws SchemaException {
		Document parsedDocument = DOMUtil.parseFile(file);
		Element listElement = DOMUtil.getFirstChildElement(parsedDocument);
		if (listElement == null) {
			return null;
		}
		NodeList childNodes = listElement.getChildNodes();
		int count = 0;
		List<PrismObject<? extends Objectable>> list = new ArrayList<PrismObject<? extends Objectable>>();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node instanceof Element) {
				count++;
				try {
					PrismObject<Objectable> object = parseObject((Element) node);
					list.add(object);
				} catch (SchemaException e) {
					throw new SchemaException(
							e.getMessage() + " while parsing object #" + count + " from file " + file, e);
				}
			}
		}
		return list;
	}

	public <T extends Objectable> PrismObject<T> parseObject(String objectString, Class<T> type) throws SchemaException {
		PrismObject<T> object = parseObject(objectString);
		if (!object.canRepresent(type)) {
			throw new SchemaException("Requested object of type " + type + ", but parsed type "
					+ object.getCompileTimeClass());
		}
		return object;
	}

	public <T extends Objectable> PrismObject<T> parseObject(String objectString) throws SchemaException {
		Document parsedDocument = DOMUtil.parseDocument(objectString);
		Element element = DOMUtil.getFirstChildElement(parsedDocument);
		if (element == null) {
			return null;
		}
		return parseObject(element);
	}

	public <T extends Objectable> PrismObject<T> parseObject(Node domNode) throws SchemaException {
		if (domNode instanceof Element) {
			return parseObject((Element) domNode);
		}
		return parseObject(DOMUtil.getFirstChildElement(domNode));
	}

	public <T extends Objectable> PrismObject<T> parseObject(Element objectElement) throws SchemaException {
		QName elementName = DOMUtil.getQName(objectElement);
		PrismSchema schema = schemaRegistry.findSchemaByNamespace(elementName.getNamespaceURI());
		if (schema == null) {
			throw new SchemaException("No schema for namespace " + elementName.getNamespaceURI());
		}
		PrismObjectDefinition<T> objectDefinition = null;
		// Try to use explicit xsi:type definition first. This overrides element
		// name.
		QName xsiType = DOMUtil.resolveXsiType(objectElement);
		if (xsiType != null) {
			objectDefinition = schema.findObjectDefinitionByType(xsiType);
			if (objectDefinition == null) {
				throw new SchemaException("No definition found for type " + xsiType + " (as defined by xsi:type)");
			}
		} else {
			objectDefinition = schema.findObjectDefinitionByElementName(elementName);
			if (objectDefinition == null) {
				throw new SchemaException("No object definition for element " + elementName + " in schema " + schema);
			}
		}
		return parseObject(objectElement, objectDefinition);
	}

	private <T extends Objectable> PrismObject<T> parseObject(Element objectElement,
			PrismObjectDefinition<T> objectDefinition) throws SchemaException {
		PrismObject<T> object = (PrismObject<T>) parsePrismContainer(objectElement, objectDefinition);
		String oid = getOid(objectElement);
		object.setOid(oid);
		String version = objectElement.getAttribute(PrismConstants.ATTRIBUTE_VERSION_LOCAL_NAME);
		object.setVersion(version);
		return object;
	}

	public <T extends Containerable> PrismContainer<T> parsePrismContainer(Element domElement) throws SchemaException {
		// locate appropriate definition based on the element name
		QName elementName = DOMUtil.getQName(domElement);
		PrismSchema schema = schemaRegistry.findSchemaByNamespace(elementName.getNamespaceURI());
		if (schema == null) {
			throw new SchemaException("No schema definition for namespace '" + elementName.getNamespaceURI() + "'");
		}
		PrismContainerDefinition<T> propertyContainerDefinition = schema.findItemDefinition(elementName,
				PrismContainerDefinition.class);
		if (propertyContainerDefinition == null) {
			throw new SchemaException("No definition for element " + elementName);
		}
		return parsePrismContainer(domElement, propertyContainerDefinition);
	}

	public <T extends Containerable> PrismContainer<T> parsePrismContainer(Element domElement,
			PrismContainerDefinition<T> propertyContainerDefinition) throws SchemaException {
		List<Object> valueElements = new ArrayList<Object>(1);
		valueElements.add(domElement);
		return parsePrismContainer(valueElements, DOMUtil.getQName(domElement), propertyContainerDefinition);
	}

	private <T extends Containerable> PrismContainer<T> parsePrismContainer(List<? extends Object> valueElements,
			QName itemName, PrismContainerDefinition<T> containerDefinition) throws SchemaException {
		PrismContainer<T> container = containerDefinition.instantiate(itemName);
		for (Object value : valueElements) {
			if (value instanceof Element) {
				Element element = (Element) value;
				Long id = getContainerId(element);
				PrismContainerValue<T> pval = new PrismContainerValue<T>(null, null, container, id);
				List<Element> childElements = DOMUtil.listChildElements(element);
				Collection<? extends Item> newContainerItems = parsePrismContainerItems(childElements,
						containerDefinition);
				addValuesToContainerValue(pval, newContainerItems);
				container.add(pval);
			} else if (value instanceof JAXBElement) {
				parsePrismContainerFromValueObject(((JAXBElement) value).getValue(), container.getDefinition(),
						container);
			} else {
				parsePrismContainerFromValueObject(value, container.getDefinition(), container);
			}
		}
		return container;
	}

	/**
	 * Adds all the value to the container value, merging with existing values
	 * if necessary.
	 */
	private <T extends Containerable> void addValuesToContainerValue(PrismContainerValue<T> containerValue,
			Collection<? extends Item> newContainerItems) throws SchemaException {
		for (Item<?> newItem : newContainerItems) {
			Item existingItem = containerValue.findItem(newItem.getName());
			if (existingItem == null) {
				containerValue.add(newItem);
			} else {
				for (PrismValue newPVal : newItem.getValues()) {
					existingItem.add(newPVal.clone());
				}
			}
		}
	}

	private <T extends Containerable> PrismContainerValue<T> parsePrismContainerFromValueObject(Object value,
			PrismContainerDefinition def, PrismContainer<T> parent) throws SchemaException {
		if (value instanceof Containerable) {
			PrismContainerValue<T> containerValue = ((Containerable) value).asPrismContainerValue();
			// This may be a JAXB parsed or constructed bean without a context.
			// Make sure the context is set.
			containerValue.revive(parent.getPrismContext());
			parent.add(containerValue.clone());
			containerValue.applyDefinition(def);
			return containerValue;
		}
		throw new SchemaException("Cannot process value of class " + value.getClass() + " as a container " + def);

	}

	private String getOid(Element element) {
		String oid = element.getAttribute(PrismConstants.ATTRIBUTE_OID_LOCAL_NAME);
		if (StringUtils.isBlank(oid)) {
			return null;
		}
		return oid;
	}

	private Long getContainerId(Element element) {
		String id = element.getAttribute(PrismConstants.ATTRIBUTE_ID_LOCAL_NAME);
		if (StringUtils.isNotBlank(id)) {
			return Long.valueOf(id);
		}
		id = element.getAttributeNS(element.getNamespaceURI(), PrismConstants.ATTRIBUTE_ID_LOCAL_NAME);
		if (StringUtils.isNotBlank(id)) {
			return Long.valueOf(id);
		}
		id = element
				.getAttributeNS(DOMUtil.XML_ID_ATTRIBUTE.getNamespaceURI(), DOMUtil.XML_ID_ATTRIBUTE.getLocalPart());
		if (StringUtils.isNotBlank(id)) {
			return Long.valueOf(id);
		}
		return null;
	}

	public <T extends Containerable> Collection<? extends Item> parsePrismContainerItems(List<Element> childElements,
			PrismContainerDefinition<T> containerDefinition) throws SchemaException {
		return parsePrismContainerItems(childElements, containerDefinition, null);
	}

	/**
	 * Parses items of PRISM CONTAINER from a list of elements.
	 * <p/>
	 * The elements must describe properties or property container as defined by
	 * this PropertyContainerDefinition. Serializes all the elements from the
	 * provided list.
	 * <p/>
	 * Internal parametric method. This does the real work.
	 * <p/>
	 * min/max constraints are not checked now TODO: maybe we need to check them
	 */
	protected <T extends Containerable> Collection<? extends Item<?>> parsePrismContainerItems(List<Element> elements,
			PrismContainerDefinition<T> containerDefinition, Collection<? extends ItemDefinition> selection)
			throws SchemaException {

		// TODO: more robustness in handling schema violations (min/max
		// constraints, etc.)

		Collection<Item<?>> props = new HashSet<Item<?>>();

		// Iterate over all the XML elements there. Each element is
		// an item value.
		for (int i = 0; i < elements.size(); i++) {
			Element propElement = elements.get(i);
			QName elementQName = DOMUtil.getQName(propElement);
			// Collect all elements with the same QName
			List<Element> valueElements = new ArrayList<Element>();
			valueElements.add(propElement);
			while (i + 1 < elements.size() && elementQName.equals(DOMUtil.getQName(elements.get(i + 1)))) {
				i++;
				valueElements.add(elements.get(i));
			}

			// If there was a selection filter specified, filter out the
			// properties that are not in the filter.

			// Quite an ugly code. TODO: clean it up
			if (selection != null) {
				boolean selected = false;
				for (ItemDefinition selProdDef : selection) {
					if (selProdDef.getNameOrDefaultName().equals(elementQName)) {
						selected = true;
					}
				}
				if (!selected) {
					continue;
				}
			}

			// Find item definition from the schema
			ItemDefinition def = locateItemDefinition(containerDefinition, elementQName, valueElements);

			if (def == null) {
				if (containerDefinition.isRuntimeSchema()) {
					// No definition for item, but the schema is runtime. the
					// definition may come later.
					// Null is OK here.
				} else {
					throw new SchemaException("Item " + elementQName + " has no definition", elementQName);
				}
			}

			Item<?> item = parseItem(valueElements, elementQName, def);
			props.add(item);
		}
		return props;
	}

	/**
	 * Try to locate xsi:type definition in the elements and return appropriate
	 * ItemDefinition.
	 */
	private ItemDefinition resolveDynamicItemDefinition(ItemDefinition parentDefinition, List<Element> valueElements,
			PrismContext prismContext) throws SchemaException {
		QName typeName = null;
		QName elementName = null;
		// Set it to multi-value to be on the safe side
		int maxOccurs = -1;
		for (Object element : valueElements) {
			if (elementName == null) {
				elementName = JAXBUtil.getElementQName(element);
			}
			// TODO: try JAXB types
			if (element instanceof Element) {
				Element domElement = (Element) element;
				if (DOMUtil.hasXsiType(domElement)) {
					typeName = DOMUtil.resolveXsiType(domElement);
					if (typeName != null) {
						String maxOccursString = domElement.getAttributeNS(
								PrismConstants.A_MAX_OCCURS.getNamespaceURI(),
								PrismConstants.A_MAX_OCCURS.getLocalPart());
						if (!StringUtils.isBlank(maxOccursString)) {
							maxOccurs = parseMultiplicity(maxOccursString, elementName);
						}
						break;
					}
				}
			}
		}
		// FIXME: now the definition assumes property, may also be property
		// container?
		if (typeName == null) {
			return null;
		}
		PrismPropertyDefinition propDef = new PrismPropertyDefinition(elementName, elementName, typeName, prismContext);
		propDef.setMaxOccurs(maxOccurs);
		propDef.setDynamic(true);
		return propDef;
	}

	private ItemDefinition resolveDynamicItemDefinition(ItemDefinition parentDefinition, QName elementName,
			List<Element> valueElements, PrismContext prismContext) throws SchemaException {
		QName typeName = null;
		// QName elementName = null;
		// Set it to multi-value to be on the safe side
		int maxOccurs = -1;
		for (Object element : valueElements) {
			// if (elementName == null) {
			// elementName = JAXBUtil.getElementQName(element);
			// }
			// TODO: try JAXB types
			if (element instanceof Element) {
				Element domElement = (Element) element;
				if (DOMUtil.hasXsiType(domElement)) {
					typeName = DOMUtil.resolveXsiType(domElement);
					if (typeName != null) {
						String maxOccursString = domElement.getAttributeNS(
								PrismConstants.A_MAX_OCCURS.getNamespaceURI(),
								PrismConstants.A_MAX_OCCURS.getLocalPart());
						if (!StringUtils.isBlank(maxOccursString)) {
							maxOccurs = parseMultiplicity(maxOccursString, elementName);
						}
						break;
					}
				}
			}
		}
		// FIXME: now the definition assumes property, may also be property
		// container?
		if (typeName == null) {
			return null;
		}
		PrismPropertyDefinition propDef = new PrismPropertyDefinition(elementName, elementName, typeName, prismContext);
		propDef.setMaxOccurs(maxOccurs);
		propDef.setDynamic(true);
		return propDef;
	}

	private int parseMultiplicity(String maxOccursString, QName elementName) throws SchemaException {
		if (PrismConstants.MULTIPLICITY_UNBONUNDED.equals(maxOccursString)) {
			return -1;
		}
		if (maxOccursString.startsWith("-")) {
			return -1;
		}
		if (StringUtils.isNumeric(maxOccursString)) {
			return Integer.valueOf(maxOccursString);
		} else {
			throw new SchemaException("Expecetd numeric value for " + PrismConstants.A_MAX_OCCURS.getLocalPart()
					+ " attribute on " + elementName + " but got " + maxOccursString);
		}
	}

	private <T> PrismProperty<T> parsePrismPropertyRaw(List<? extends Object> valueElements, QName itemName)
			throws SchemaException {
		Object firstElement = valueElements.get(0);
		QName propertyName = JAXBUtil.getElementQName(firstElement);
		PrismProperty<T> property = new PrismProperty<T>(itemName);
		for (Object valueElement : valueElements) {
			PrismPropertyValue<T> pval = new PrismPropertyValue<T>(null);
			pval.setRawElement(valueElement);
			property.add(pval);
		}
		return property;
	}

	public <T> PrismProperty<T> parsePrismProperty(List<? extends Object> valueElements, QName propName,
			PrismPropertyDefinition propertyDefinition) throws SchemaException {
		if (valueElements == null || valueElements.isEmpty()) {
			return null;
		}
		PrismProperty<T> prop = propertyDefinition.instantiate(propName);

		if (!propertyDefinition.isMultiValue() && valueElements.size() > 1) {
			throw new SchemaException("Attempt to store multiple values in single-valued property " + propName);
		}

		for (Object valueElement : valueElements) {
			T realValue = parsePrismPropertyRealValue(valueElement, propertyDefinition);
			if (realValue != null) {
				prop.add(new PrismPropertyValue<T>(realValue));
			}
		}
		return prop;
	}

	public <T> T parsePrismPropertyRealValue(Object valueElement, PrismPropertyDefinition propertyDefinition)
			throws SchemaException {
		QName typeName = propertyDefinition.getTypeName();
		PrismJaxbProcessor jaxbProcessor = getJaxbProcessor();
		Object realValue = null;
		if (valueElement instanceof JAXBElement<?>) {
			Object jaxbElementValue = ((JAXBElement<?>)valueElement).getValue();
			if (jaxbElementValue instanceof Element) {
				valueElement = jaxbElementValue;
			}
		}
		if (valueElement instanceof Element) {
			// Need to convert the DOM representation to something more
			// java-like
			Element element = (Element) valueElement;
			if (DOMUtil.isNil(element)) {
				return null;
			}
			if (propertyDefinition.getTypeName().equals(DOMUtil.XSD_ANY)) {
				// No conversion. Element is the value. Almost.
				// Extract the namespace declarations from explicit elements
				// used to fortify them against
				// stupid XML normalization. If the declarations were not
				// fortified this has no effect.
				PrismUtil.unfortifyNamespaceDeclarations(element);
				realValue = element;
			} else if (XmlTypeConverter.canConvert(typeName)) {
				realValue = XmlTypeConverter.toJavaValue(element, typeName);
			} else if (jaxbProcessor.canConvert(typeName)) {
				try {
					realValue = jaxbProcessor.toJavaValue(element, typeName);
				} catch (JAXBException e) {
					throw new SchemaException("Attempt to unmarshal value of property " + propertyDefinition.getName()
							+ " failed: " + e.getMessage(), e);
				} catch (IllegalArgumentException e) {
					throw new SchemaException(e.getMessage() + " in property " + propertyDefinition.getName(), e);
				}
			} else {
				// fallback to storing DOM in value
				// We need to fix the declarations. The element may be used as
				// stand-alone
				// DOM element value, so it needs to be "torn out" of the
				// original document
				DOMUtil.fixNamespaceDeclarations(element);
				realValue = element;
			}
		} else if (valueElement instanceof JAXBElement) {
			JAXBElement jaxbElement = (JAXBElement) valueElement;
			realValue = jaxbElement.getValue();
		} else {
			// The values is already in java form, just store it directly
			realValue = valueElement;
		}
		postProcessPropertyRealValue((T) realValue);
		return (T) realValue;
	}

	private <T> void postProcessPropertyRealValue(T realValue) {
		if (realValue == null) {
			return;
		}
		if (realValue instanceof PolyString) {
			PolyString polyString = (PolyString) realValue;
			if (!polyString.isComputed()) {
				polyString.recompute(getPrismContext().getDefaultPolyStringNormalizer());
			}
		}
	}

	public <T> PrismProperty<T> parsePropertyFromValueElement(Element valueElement,
			PrismPropertyDefinition propertyDefinition) throws SchemaException {
		PrismProperty<T> prop = propertyDefinition.instantiate();
		if (propertyDefinition.isSingleValue()) {
			T realValue = (T) XmlTypeConverter.convertValueElementAsScalar(valueElement,
					propertyDefinition.getTypeName());
			postProcessPropertyRealValue(realValue);
			prop.addValue(new PrismPropertyValue<T>(realValue));
		} else {
			List<T> list = (List) XmlTypeConverter.convertValueElementAsList(valueElement,
					propertyDefinition.getTypeName());
			for (T realValue : list) {
				postProcessPropertyRealValue(realValue);
				prop.add(new PrismPropertyValue<T>(realValue));
			}
		}
		return prop;
	}

	/**
	 * Used e.g. to parse values from XML representation of deltas.
	 * valueElements may contain DOM and JAXB values
	 * 
	 * @throws SchemaException
	 */
	public <T extends Containerable, V extends PrismValue> Collection<? extends Item<V>> parseContainerItems(
			PrismContainerDefinition<T> containingPcd, List<Object> valueElements) throws SchemaException {
		Collection<Item<V>> items = new ArrayList<Item<V>>(valueElements.size());
		for (int i = 0; i < valueElements.size(); i++) {
			Object valueElement = valueElements.get(i);
			QName elementQName = JAXBUtil.getElementQName(valueElement);
			if (elementQName == null) {
				throw new SchemaException("Cannot determine name of element " + valueElement + " in " + containingPcd);
			}
			List<Object> itemValueElements = new ArrayList<Object>();
			itemValueElements.add(valueElement);
			while (i + 1 < valueElements.size()
					&& elementQName.equals(JAXBUtil.getElementQName(valueElements.get(i + 1)))) {
				i++;
				itemValueElements.add(valueElements.get(i));
			}
			ItemDefinition itemDefinition = locateItemDefinition(containingPcd, elementQName, itemValueElements);
			if (itemDefinition == null) {
				if (containingPcd.isRuntimeSchema()) {
					// Definition may come later. Null is OK.
				} else {
					throw new SchemaException("Definition of item " + elementQName + " cannot be found in "
							+ containingPcd);
				}
			}
			Item<V> item = parseItem(itemValueElements, elementQName, itemDefinition);
			items.add(item);
		}
		return items;
	}

	/**
	 * Used e.g. to parse values from XML representation of deltas.
	 * valueElements may contain DOM and JAXB values
	 * 
	 * @throws SchemaException
	 */
	public <T extends Containerable> Collection<? extends Item<?>> parseContainerItems(
			PrismContainerDefinition<T> containingPcd, List<? extends Object> valueElements, QName propertyName,
			boolean reference) throws SchemaException {
		Validate.notNull(propertyName, "Property name to parse must not be null");
		Collection<Item<?>> items = new ArrayList<Item<?>>(valueElements.size());
		ItemDefinition itemDefinition = locateItemDefinition(containingPcd, propertyName, valueElements);
		if (itemDefinition == null) {
			if (containingPcd.isRuntimeSchema()) {
				// Definition may come later. Null is OK.
			} else {
				throw new SchemaException("Definition of item " + propertyName + " cannot be found in " + containingPcd);
			}
		}
		Item<?> item = parseItem(valueElements, propertyName, itemDefinition, reference);
		items.add(item);

		return items;
	}

	private <T extends Containerable> ItemDefinition locateItemDefinition(
			PrismContainerDefinition<T> containerDefinition, QName elementQName, List<? extends Object> valueElements)
			throws SchemaException {
		ItemDefinition def = containerDefinition.findItemDefinition(elementQName);
		if (def != null) {
			return def;
		}

		if (!valueElements.isEmpty() && valueElements.get(0) instanceof Element) {
			// Try to locate xsi:type definition in the element
			def = resolveDynamicItemDefinition(containerDefinition, elementQName, (List<Element>) valueElements,
					prismContext);
		}
		if (def != null) {
			return def;
		}

		if (containerDefinition.isRuntimeSchema()) {
			// Try to locate global definition in any of the schemas
			def = resolveGlobalItemDefinition(containerDefinition, elementQName, valueElements);
		}
		return def;
	}

	private <T extends Containerable> ItemDefinition resolveGlobalItemDefinition(
			PrismContainerDefinition<T> containerDefinition, List<? extends Object> valueElements)
			throws SchemaException {
		Object firstElement = valueElements.get(0);
		QName elementQName = JAXBUtil.getElementQName(firstElement);
		return getPrismContext().getSchemaRegistry().resolveGlobalItemDefinition(elementQName);
	}

	private <T extends Containerable> ItemDefinition resolveGlobalItemDefinition(
			PrismContainerDefinition<T> containerDefinition, QName elementQName, List<? extends Object> valueElements)
			throws SchemaException {
		// Object firstElement = valueElements.get(0);
		// QName elementQName = JAXBUtil.getElementQName(firstElement);
		return getPrismContext().getSchemaRegistry().resolveGlobalItemDefinition(elementQName);
	}

	public PrismReference parsePrismReference(List<? extends Object> valueElements, QName propName,
			PrismReferenceDefinition referenceDefinition) throws SchemaException {
		if (valueElements == null || valueElements.isEmpty()) {
			return null;
		}
		PrismReference ref = referenceDefinition.instantiate();

		if (!referenceDefinition.isMultiValue() && valueElements.size() > 1) {
			throw new SchemaException("Attempt to store multiple values in single-valued property " + propName);
		}

		for (Object valueElement : valueElements) {
			if (propName.equals(referenceDefinition.getName())) {
				// This is "real" reference (oid type and nothing more)
				if (valueElement instanceof Element) {
					ref.add(parseReferenceValue((Element) valueElement));
				} else if (valueElement instanceof JAXBElement) {
					ref.add(parseReferenceValueFromObject(((JAXBElement<?>) valueElement).getValue()));
				} else {
					ref.add(parseReferenceValueFromObject(valueElement));
				}
			} else {
				// This is a composite object (complete object stored inside
				// reference)
				ref.add(parseReferenceAsCompositeObject(valueElement, referenceDefinition));
			}
		}
		return ref;
	}

	private PrismReferenceValue parseReferenceAsCompositeObject(Object valueElement,
			PrismReferenceDefinition referenceDefinition) throws SchemaException {
		QName targetTypeName = referenceDefinition.getTargetTypeName();
		PrismObjectDefinition<Objectable> schemaObjectDefinition = null;
		if (targetTypeName != null) {
			schemaObjectDefinition = getPrismContext().getSchemaRegistry().findObjectDefinitionByType(targetTypeName);
		}

		PrismObject<Objectable> compositeObject = null;
		try {
			if (valueElement instanceof Element) {
				Element valueDomElement = (Element) valueElement;
				if (schemaObjectDefinition == null) {
					compositeObject = parseObject(valueDomElement);
				} else {
					compositeObject = parseObject(valueDomElement, schemaObjectDefinition);
				}
			} else if (valueElement instanceof JAXBElement) {
				// This must be complete JAXB object
				JAXBElement<Objectable> jaxbElement = (JAXBElement<Objectable>) valueElement;
				Objectable objectable = jaxbElement.getValue();
				compositeObject = objectable.asPrismObject();
				if (schemaObjectDefinition == null) {
					getPrismContext().adopt(objectable);
				} else {
					compositeObject.revive(getPrismContext());
					compositeObject.applyDefinition(schemaObjectDefinition);
				}
			}
		} catch (SchemaException e) {
			throw new SchemaException(e.getMessage() + " while parsing composite object in reference element "
					+ referenceDefinition.getCompositeObjectElementName(), e);
		}

		PrismReferenceValue refVal = new PrismReferenceValue();
		refVal.setObject(compositeObject);
		return refVal;
	}

	public PrismReferenceValue parseReferenceValue(Element element) {
		String oid = getOid(element);
		QName type = DOMUtil.getQNameAttribute(element, PrismConstants.ATTRIBUTE_REF_TYPE_LOCAL_NAME);
		if (type != null) {
			DOMUtil.validateNonEmptyQName(type, " in reference type in " + DOMUtil.getQName(element));
		}
		PrismReferenceValue refVal = new PrismReferenceValue(oid);
		refVal.setTargetType(type);

		QName relationAttribute = DOMUtil.getQNameAttribute(element, PrismConstants.ATTRIBUTE_RELATION_LOCAL_NAME);
		if (relationAttribute != null) {
			DOMUtil.validateNonEmptyQName(relationAttribute, " in reference type in " + DOMUtil.getQName(element));
		}
		refVal.setRelation(relationAttribute);

		Element descriptionElement = DOMUtil.getChildElement(element, PrismConstants.ELEMENT_DESCRIPTION_LOCAL_NAME);
		if (descriptionElement != null) {
			refVal.setDescription(descriptionElement.getTextContent());
		}
		Element filterElement = DOMUtil.getChildElement(element, PrismConstants.ELEMENT_FILTER_LOCAL_NAME);
		if (filterElement != null) {
			refVal.setFilter(DOMUtil.getFirstChildElement(filterElement));
		}

		return refVal;
	}

	private PrismReference parseReference(List<? extends Object> elements, ItemDefinition definition) throws SchemaException{
		if (!(definition instanceof PrismReferenceDefinition)) {
			throw new SchemaException("Attempt to parse prism reference with bad definition " + definition
					+ ". It is required prism reference definition.");
		}

		if (elements == null || elements.isEmpty()) {
			return null;
		}

		PrismReferenceDefinition referenceDefinition = (PrismReferenceDefinition) definition;
		PrismReference ref = referenceDefinition.instantiate();
		PrismReferenceValue refVal = new PrismReferenceValue();
		for (Object obj : elements) {
			if (obj instanceof Element) {
				Element subelement = (Element) obj;
				if (QNameUtil.compareQName(new QName(PrismConstants.NS_QUERY, "path"), subelement)){
					continue;
				}
				if (QNameUtil.compareQName(PrismConstants.Q_OID, subelement)) {
					String oid = null;
					if (DOMUtil.hasChildElements(subelement)) {
						Element value = DOMUtil.getChildElement(subelement, PrismConstants.Q_VALUE);
						if (value != null) {
							oid = value.getTextContent();
						}
					} else {
						oid = subelement.getTextContent();
					}
					refVal.setOid(oid);
				} else if (QNameUtil.compareQName(PrismConstants.Q_TYPE, subelement)) {
					QName type = DOMUtil.resolveQName(subelement);
					if (type != null) {
						DOMUtil.validateNonEmptyQName(type, " in reference type in " + DOMUtil.getQName(subelement));
					}
					refVal.setTargetType(type);
				} else if (QNameUtil.compareQName(PrismConstants.Q_RELATION, subelement)) {
					QName relation = DOMUtil.resolveQName(subelement);
					if (relation != null) {
						DOMUtil.validateNonEmptyQName(relation, " in reference type in " + DOMUtil.getQName(subelement));
					}
					refVal.setRelation(relation);
				}
				ref.add(refVal);
			}
			// TODO: filter, description..and also support for expression for
			// type and reference
		}

		return ref;
	}

	/**
	 * The input is an object that represents the reference, most likely a JAXB
	 * object. Pull out the information by reflection. Black magic warning: JAXB
	 * compiler may create the reference in (at least) two different ways
	 * depending on how much schema is available during compilation. This
	 * manisfests itself mostly for the "filter" property. So we try both ways
	 * before raising an error.
	 */
	private PrismReferenceValue parseReferenceValueFromObject(Object referenceObject) throws SchemaException {
		String oid = ReflectionUtil.getJavaProperty(referenceObject, JAVA_PROPERTY_OID, String.class);
		QName type = ReflectionUtil.getJavaProperty(referenceObject, JAVA_PROPERTY_TYPE, QName.class);
		PrismReferenceValue refVal = new PrismReferenceValue(oid);
		refVal.setTargetType(type);
		String description = ReflectionUtil.getJavaProperty(referenceObject, JAVA_PROPERTY_DESCRIPTION, String.class);
		refVal.setDescription(description);
		Object filterType = ReflectionUtil.getJavaProperty(referenceObject, JAVA_PROPERTY_FILTER, Object.class);
		if (filterType != null) {
			if (ReflectionUtil.hasJavaProperty(filterType, JAVA_JAXB_PROPERTY_ANY)) {
				List filterElementList = ReflectionUtil.getJavaProperty(filterType, JAVA_JAXB_PROPERTY_ANY, List.class);
				if (filterElementList != null) {
					Object firstElement = filterElementList.get(0);
					if (firstElement instanceof Element) {
						refVal.setFilter((Element) firstElement);
					} else {
						throw new SchemaException("Unknown type of filter element " + firstElement.getClass());
					}
				}
			} else if (ReflectionUtil.hasJavaProperty(filterType, JAVA_PROPERTY_FILTER)) {
				Element filterElement = ReflectionUtil.getJavaProperty(filterType, JAVA_PROPERTY_FILTER, Element.class);
				refVal.setFilter(filterElement);
			} else {
				throw new SchemaException("JAXB bean of type " + referenceObject.getClass().getName()
						+ " representing prism reference" + " does not contain '" + JAVA_JAXB_PROPERTY_ANY
						+ "' property nor '" + JAVA_PROPERTY_FILTER + "' property");
			}
		}
		return refVal;
	}

	/**
	 * This gets definition of an unspecified type. It has to find the right
	 * method to call. Value elements have the same element name. They may be
	 * elements of a property or a container.
	 */
	public <V extends PrismValue> Item<V> parseItem(List<? extends Object> valueElements, QName itemName, ItemDefinition def)
			throws SchemaException {
		if (def == null) {
			// Assume property in a container with runtime definition
			return (Item<V>) parsePrismPropertyRaw(valueElements, itemName);
		}
		if (def instanceof PrismContainerDefinition) {
			return (Item<V>) parsePrismContainer(valueElements, itemName, (PrismContainerDefinition<?>) def);
		} else if (def instanceof PrismPropertyDefinition) {
			return (Item<V>) parsePrismProperty(valueElements, itemName, (PrismPropertyDefinition) def);
		}
		if (def instanceof PrismReferenceDefinition) {
			return (Item<V>) parsePrismReference(valueElements, itemName, (PrismReferenceDefinition) def);
		} else {
			throw new IllegalArgumentException("Attempt to parse unknown definition type " + def.getClass().getName());
		}
	}

	public Item<?> parseItem(List<? extends Object> valueElements, QName itemName, ItemDefinition def, boolean reference)
			throws SchemaException {
		if (def == null) {
			// Assume property in a container with runtime definition
			return parsePrismPropertyRaw(valueElements, itemName);
		}
		if (reference) {
			return parseReference(valueElements, def);
		} else{
			return parseItem(valueElements, itemName, def);
		}
	}

	// Property:

	// public Property parseFromValueElement(Element valueElement, PropertyPath
	// parentPath) throws SchemaException {
	// Property prop = this.instantiate(parentPath);
	// if (isSingleValue()) {
	// prop.add(new
	// PropertyValue(XsdTypeConverter.convertValueElementAsScalar(valueElement,
	// getTypeName())));
	// } else {
	// List list = XsdTypeConverter.convertValueElementAsList(valueElement,
	// getTypeName());
	// for (Object object : list) {
	// prop.add(new PropertyValue(object));
	// }
	// }
	// return prop;
	// }

	/**
	 * Parse the provided JAXB/DOM element and add it as a new value of the
	 * specified item.
	 */
	public <T extends Containerable> boolean addItemValue(Item item, Object element, PrismContainer<T> container)
			throws SchemaException {
		ItemDefinition itemDefinition = item.getDefinition();
		List<Object> itemValueElements = new ArrayList<Object>();
		itemValueElements.add(element);
		if (itemDefinition == null) {
			itemDefinition = locateItemDefinition(container.getDefinition(), item.getName(), itemValueElements);
		}
		if (itemDefinition == null) {
			if (container.getDefinition() != null && container.getDefinition().isRuntimeSchema()) {
				// Null is OK, definition may come later
			} else {
				throw new SchemaException("Definition of item " + item + " in " + container + " cannot be determined");
			}
		}
		// Kind of hack now. Just to reuse existing code.
		Item<?> fauxItem = parseItem(itemValueElements, item.getName(), itemDefinition);
		PrismValue itemValue = fauxItem.getValues().get(0);
		return item.add(itemValue);
	}

	/**
	 * Parse the provided JAXB/DOM element and delete it from the specified
	 * item.
	 */
	public <T extends Containerable> boolean deleteItemValue(Item item, Object element, PrismContainer<T> container)
			throws SchemaException {
		ItemDefinition itemDefinition = item.getDefinition();
		List<Object> itemValueElements = new ArrayList<Object>();
		itemValueElements.add(element);
		if (itemDefinition == null) {
			itemDefinition = locateItemDefinition(container.getDefinition(), item.getName(), itemValueElements);
		}
		if (itemDefinition == null) {
			if (container.getDefinition() != null && container.getDefinition().isRuntimeSchema()) {
				// Null is OK, definition may come later
			} else {
				throw new SchemaException("Definition of item " + item + " in " + container + " cannot be determined");
			}
		}
		// Kind of hack now. Just to reuse existing code.
		Item<?> fauxItem = parseItem(itemValueElements, item.getName(), itemDefinition);
		PrismValue itemValue = fauxItem.getValues().get(0);
		return item.remove(itemValue);
	}

	// ================================================
	// = SERIALIZATION
	// ================================================

	/**
	 * Returns "dead" DOM representation of the PrismObject. The representation
	 * is a copy of the object and does not change with the object. But the
	 * representation is fully DOM-compliant.
	 */
	public Element serializeToDom(PrismObject<?> object) throws SchemaException {
		return serializeToDom(object, false);
	}

	public Element serializeToDom(PrismObject<?> object, boolean serializeCompositeObjects) throws SchemaException {
		DomSerializer domSerializer = new DomSerializer(getPrismContext());
		domSerializer.setSerializeCompositeObjects(serializeCompositeObjects);
		return domSerializer.serialize(object);
	}

	public <T extends Containerable> Element serializeToDom(PrismContainerValue<T> object, Element parentElement)
			throws SchemaException {

		DomSerializer domSerializer = new DomSerializer(getPrismContext());
		return domSerializer.serializeContainerValue(object, parentElement);
	}

	public <T extends Objectable> String serializeObjectToString(PrismObject<T> object) throws SchemaException {
		return serializeObjectToString(object, false);
	}

	public <T extends Objectable> String serializeObjectToString(PrismObject<T> object,
			boolean serializeCompositeObjects) throws SchemaException {
		Element element = serializeToDom(object, serializeCompositeObjects);
		return DOMUtil.serializeDOMToString(element);
	}

	public <T extends Containerable> String serializeObjectToString(PrismContainerValue<T> object, Element parentElement)
			throws SchemaException {
		Element element = serializeToDom(object, parentElement);
		return DOMUtil.serializeDOMToString(element);
	}

	public Element serializeValueToDom(PrismValue pval, QName elementName) throws SchemaException {
		return serializeValueToDom(pval, elementName, DOMUtil.getDocument());
	}

	public Element serializeValueToDom(PrismValue pval, QName elementName, Document document) throws SchemaException {
		// This is kind of a hack. The serializeValueToDom method will place the
		// element that we want below the
		// "parent" element that we need to provide. So we create fake element
		// and then extract the real element
		// from it.
		Element fakeElement = document.createElementNS(elementName.getNamespaceURI(), elementName.getLocalPart());
		serializeValueToDom(pval, fakeElement);
		return DOMUtil.getFirstChildElement(fakeElement);
	}

	public void serializeValueToDom(PrismValue pval, Element parentElement) throws SchemaException {
		DomSerializer domSerializer = new DomSerializer(getPrismContext());
		domSerializer.serialize(pval, parentElement);
	}

	public List<Element> serializeItemToDom(Item<?> item) throws SchemaException {
		return serializeItemToDom(item, DOMUtil.getDocument());
	}

	public List<Element> serializeItemToDom(Item<?> item, Document document) throws SchemaException {
		QName elementName = item.getName();
		// This is kind of a hack. The serializeValueToDom method will place the
		// element that we want below the
		// "parent" element that we need to provide. So we create fake element
		// and then extract the real element
		// from it.
		Element fakeElement = document.createElementNS(elementName.getNamespaceURI(), elementName.getLocalPart());
		serializeItemToDom(item, fakeElement);
		return DOMUtil.listChildElements(fakeElement);
	}

	public void serializeItemToDom(Item<?> item, Element parentElement) throws SchemaException {
		DomSerializer domSerializer = new DomSerializer(getPrismContext());
		domSerializer.serialize(item, parentElement);
	}

	/**
	 * Determines proper name for the element with specified local name.
	 */
	public String determineElementNamespace(Itemable parent, String elementDescriptionLocalName) {
		ItemDefinition definition = parent.getDefinition();
		if (definition == null) {
			return parent.getName().getNamespaceURI();
		}
		// This is very simplistic now, it assumes that all elements are in the
		// "tns" namespace.
		// TODO: Improve it later.
		return definition.getTypeName().getNamespaceURI();
	}
}
