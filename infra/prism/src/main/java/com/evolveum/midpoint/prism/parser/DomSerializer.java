/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.prism.parser;

import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContainerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class DomSerializer {
	
	private Document doc;
	private Element topElement;
	private boolean serializeCompositeObjects = false;
	private boolean fortifyNamespaces = false;
	private DOMParser parser;
	private SchemaRegistry schemaRegistry;
	
	DomSerializer(DOMParser parser, SchemaRegistry schemaRegistry) {
		super();
		this.parser = parser;
		this.schemaRegistry = schemaRegistry;
	}
	
	public boolean isSerializeCompositeObjects() {
		return serializeCompositeObjects;
	}

	public void setSerializeCompositeObjects(boolean serializeCompositeObjects) {
		this.serializeCompositeObjects = serializeCompositeObjects;
	}

	private DynamicNamespacePrefixMapper getNamespacePrefixMapper() {
		return schemaRegistry.getNamespacePrefixMapper();
	}

	public Element serialize(PrismObject<?> object) throws SchemaException {
		initialize();		
		Element topElement = createElement(object.getElementName());
		serialize(object, topElement);
		return topElement;
	}
		
	private void serialize(PrismObject<?> object, QName elementName, Element parentElement) throws SchemaException {
		Element topElement = createElement(elementName);
		parentElement.appendChild(topElement);
		serialize(object, topElement);
	}
	
	private void serialize(PrismObject<?> object, Element topElement) throws SchemaException {
		serializeItems(object.getValue().getItems(), object.getDefinition(), topElement);
		if (object.getOid() != null) {
			topElement.setAttribute(PrismConstants.ATTRIBUTE_OID_LOCAL_NAME, object.getOid());
		}
		if (object.getVersion() != null) {
			topElement.setAttribute(PrismConstants.ATTRIBUTE_VERSION_LOCAL_NAME, object.getVersion());
		}
        QName elementQName = new QName(topElement.getNamespaceURI(), topElement.getLocalName());
		if (object.getDefinition() != null &&
//				!prismContext.getSchemaRegistry().hasImplicitTypeDefinition(object.getName(), object.getDefinition().getTypeName())) {
                !schemaRegistry.hasImplicitTypeDefinition(elementQName, object.getDefinition().getTypeName())) {
			DOMUtil.setXsiType(topElement, object.getDefinition().getTypeName());
		}
	}
	
	private void initialize() {
		doc = DOMUtil.getDocument();
		topElement = null;
	}

    public Element serializeContainerValue(PrismContainerValue<?> value, Element parentElement) throws SchemaException {
        initialize();
        doc = parentElement.getOwnerDocument();
        
        serialize(value, parentElement);
        return parentElement;
    }

	private void serialize(PrismContainerValue<?> value, Element parentElement) throws SchemaException {
		PrismContainerable<?> parent = value.getParent();
		QName elementQName = parent.getElementName();
		Element element = createElement(elementQName);
		parentElement.appendChild(element);
		serializeItems(value.getItems(), parent.getDefinition(), element);
		if (value.getId() != null) {
			element.setAttribute(PrismConstants.ATTRIBUTE_ID_LOCAL_NAME, value.getId().toString());
		}
	}
	
	private void serialize(PrismPropertyValue<?> value, Element parentElement) throws SchemaException {
		Itemable parent = value.getParent();
		if (parent == null) {
			throw new IllegalArgumentException("PValue "+value+" has no parent therefore it cannot be serialized to DOM");
		}
		QName elementName = parent.getElementName();
		if (value.getRawElement() != null) {
			// This element was not yet touched by the schema, but we still can serialize it
			serializeRawElement(value.getRawElement(), parentElement);
			return;
		}
		Class<? extends Object> type = value.getValue().getClass();
		if (value.getValue() instanceof Element) {
			// No conversion needed, just adopt the element
			Element originalValueElement = (Element)value.getValue();
			// Make sure that all applicable namespace declarations are placed on this element
			DOMUtil.fixNamespaceDeclarations(originalValueElement);
			Element adoptedElement = (Element) parentElement.getOwnerDocument().importNode(originalValueElement, true);
			parentElement.appendChild(adoptedElement);
			if (fortifyNamespaces) {
				// HACK HACK HACK. Make sure that the declarations survive stupid XML normalization by placing them
				// in explicit elements.
				PrismUtil.fortifyNamespaceDeclarations(adoptedElement);
			}
		} else if (XmlTypeConverter.canConvert(type)) {
			// Primitive value
			Element element = createElement(elementName);
			parentElement.appendChild(element);
			XmlTypeConverter.toXsdElement(value.getValue(), element, false);
			ItemDefinition definition = parent.getDefinition();
			if (definition != null && definition.isDynamic()) {
				DOMUtil.setXsiType(element, definition.getTypeName());
				// We cannot do this as simple types cannot have attributes
//				element.setAttributeNS(PrismConstants.A_MAX_OCCURS.getNamespaceURI(), 
//						PrismConstants.A_MAX_OCCURS.getLocalPart(), multiplicityToString(definition.getMaxOccurs()));
			}

		} else {
			// JAXB value
			PrismJaxbProcessor jaxbProcessor = null;
			if (jaxbProcessor.canConvert(type)) {
				try {
					jaxbProcessor.marshalObjectToDom(value.getValue(), elementName, parentElement);
				} catch (JAXBException e) {
					throw new SchemaException("Cannot process value of property "+elementName+": "+value+": "+e.getMessage(),e);
				}
			} else {
				throw new SchemaException("Cannot process value of property "+elementName+": "+value+": No processor available");
			}
		}
	}
	
	private String multiplicityToString(int maxOccurs) {
		if (maxOccurs<0) {
			return PrismConstants.MULTIPLICITY_UNBONUNDED;
		}
		return String.valueOf(maxOccurs);
	}

	private void serializeRawElement(Object rawElement, Element parentElement) throws SchemaException {
		if (rawElement instanceof Element) {
			Document ownerDocument = parentElement.getOwnerDocument();
			Element adoptedElement = (Element) ownerDocument.adoptNode((Element)rawElement);
			parentElement.appendChild(adoptedElement);
		} else if (rawElement instanceof JAXBElement){
			PrismJaxbProcessor jaxbProcessor = null;
			try {
				jaxbProcessor.marshalElementToDom((JAXBElement)rawElement, parentElement);
			} catch (JAXBException e) {
				throw new SchemaException("Error processing element "+rawElement+": "+e.getMessage(),e);
			}
		} else {
			throw new IllegalArgumentException("Cannot process raw element "+rawElement+" of type "+rawElement.getClass());
		}
	}

	private void serializeObject(PrismReferenceValue value, Element parentElement) throws SchemaException {
		Itemable parent = value.getParent();
		PrismReferenceDefinition definition = (PrismReferenceDefinition) parent.getDefinition();
		if (definition == null) {
			throw new SchemaException("Cannot serialize object in reference "+parent+" as the reference has no definition");
		}
		QName compositeObjectElementName = definition.getCompositeObjectElementName();
		if (compositeObjectElementName == null) {
			throw new SchemaException("Cannot serialize composite object in "+DOMUtil.getQName(parentElement)+" because the composite element" +
					"name for reference "+definition+" is not defined");
		}
		serialize(value.getObject(), compositeObjectElementName, parentElement);
	}

	private void serializeItems(List<Item<?>> items, PrismContainerDefinition definition, Element parentElement) throws SchemaException {
		if (definition != null && !definition.isDynamic()) {
			ComplexTypeDefinition complexTypeDefinition = definition.getComplexTypeDefinition();
			if (complexTypeDefinition != null) {
				serializeItemsUsingDefinition(items, complexTypeDefinition, parentElement);
				return;
			}
		}
		// We have no choice here. Just follow the "natural" order of items
		for (Item<?> item: items) {
			serialize(item, parentElement);
		}

	}
	
	private void serializeItemsUsingDefinition(List<Item<?>> items, ComplexTypeDefinition definition, Element parentElement) throws SchemaException {
		for (ItemDefinition itemDef: definition.getDefinitions()) {
			Item<?> item = findItem(items, itemDef);
			if (item != null) {
				serialize(item, parentElement);
			}
		}
	}

	private Item<?> findItem(List<Item<?>> items, ItemDefinition itemDef) {
		QName itemName = itemDef.getName();
		for (Item<?> item: items) {
			if (itemName.equals(item.getElementName())) {
				return item;
			}
		}
		return null;
	}

	<V extends PrismValue> void serialize(Item<V> item, Element parentElement) throws SchemaException {
		// special handling for reference values (account vs accountRef).
		if (item instanceof PrismReference) {
			serialize((PrismReference)item, parentElement);
		} else {
			for (V pval: item.getValues()) {
				if (!item.equals(pval.getParent())) {
					throw new IllegalArgumentException("The parent for value "+pval+" of item "+item+" is incorrect");
				}
				serialize(pval, parentElement);
			}
		}
	}
	
	private void serialize(PrismReference item, Element parentElement) throws SchemaException {
		// Composite objects first, references second
		for (PrismReferenceValue pval: item.getValues()) {
			if (pval.getObject() == null) {
				continue;
			}
			if (!item.equals(pval.getParent())) {
				throw new IllegalArgumentException("The parent for value "+pval+" of item "+item+" is incorrect");
			}
			serializeReferenceValue(pval, parentElement);
		}
		for (PrismReferenceValue pval: item.getValues()) {
			if (pval.getObject() != null) {
				continue;
			}
			if (!item.equals(pval.getParent())) {
				throw new IllegalArgumentException("The parent for value "+pval+" of item "+item+" is incorrect");
			}
			serializeReferenceValue(pval, parentElement);
		}
	}
	
	private void serializeReferenceValue(PrismReferenceValue value, Element parentElement) throws SchemaException {
		boolean isComposite = false;
		if (value.getParent() != null && value.getParent().getDefinition() != null) {
			PrismReferenceDefinition definition = (PrismReferenceDefinition) value.getParent().getDefinition();
			isComposite = definition.isComposite();
		}
		if ((serializeCompositeObjects || isComposite) && value.getObject() != null) {
			serializeObject(value, parentElement);
		} else {
			serializeRef(value, parentElement);
		}
	}
	
	private void serializeRef(PrismReferenceValue value, Element parentElement) throws SchemaException {
		Itemable parent = value.getParent();
		Element element = createElement(parent.getElementName());
		parentElement.appendChild(element);
		element.setAttribute(PrismConstants.ATTRIBUTE_OID_LOCAL_NAME, value.getOid());
		if (value.getRelation() != null) {
			QName relation = value.getRelation();
			relation = getNamespacePrefixMapper().setQNamePrefixExplicit(relation);
			try {
				DOMUtil.setQNameAttribute(element, PrismConstants.ATTRIBUTE_RELATION_LOCAL_NAME, relation);
			} catch (IllegalArgumentException e) {
				throw new SchemaException(e.getMessage()+" in type field of reference "+parent.getElementName());
			}
		}
		if (value.getTargetType() != null) {
			// Make the namespace prefix explicit due to JAXB bug
			QName targetType = value.getTargetType();
			targetType = getNamespacePrefixMapper().setQNamePrefixExplicit(targetType);
			try {
				DOMUtil.setQNameAttribute(element, PrismConstants.ATTRIBUTE_REF_TYPE_LOCAL_NAME, targetType);
			} catch (IllegalArgumentException e) {
				throw new SchemaException(e.getMessage()+" in type field of reference "+parent.getElementName());
			}
		}
		if (value.getDescription() != null) {
			String namespace = determineElementNamespace(parent,
					PrismConstants.ELEMENT_DESCRIPTION_LOCAL_NAME);
			Document doc = element.getOwnerDocument();
			Element descriptionElement = doc.createElementNS(namespace, PrismConstants.ELEMENT_DESCRIPTION_LOCAL_NAME);
			element.appendChild(descriptionElement);
			descriptionElement.setTextContent(value.getDescription());
		}
		if (value.getFilter() != null) {
			String namespace = determineElementNamespace(parent,
					PrismConstants.ELEMENT_FILTER_LOCAL_NAME);
			Document doc = element.getOwnerDocument();
			Element filterElement = doc.createElementNS(namespace, PrismConstants.ELEMENT_FILTER_LOCAL_NAME);
			element.appendChild(filterElement);
			Element adoptedElement = (Element) doc.adoptNode(((Element) value.getFilter()).cloneNode(true));
			filterElement.appendChild(adoptedElement);
		}
		
	}

	void serialize(PrismValue pval, Element parentElement) throws SchemaException {
		if (doc == null) {
			doc = parentElement.getOwnerDocument();
		}
		if (pval instanceof PrismContainerValue) {
			serialize((PrismContainerValue)pval, parentElement);
		} else if (pval instanceof PrismPropertyValue) {
			serialize((PrismPropertyValue)pval, parentElement);
		} else if (pval instanceof PrismReferenceValue) {
			serializeReferenceValue((PrismReferenceValue)pval, parentElement);
		} else {
			throw new IllegalArgumentException("Unknown value type "+pval);
		}
	}

	/**
	 * Create XML element with the correct namespace prefix and namespace definition.
	 * @param qname element QName
	 * @return created DOM element
	 */
	private Element createElement(QName qname) {
		QName qnameWithPrefix = getNamespacePrefixMapper().setQNamePrefix(qname);
		if (topElement != null) {
			return DOMUtil.createElement(doc, qnameWithPrefix, topElement, topElement);
		} else {
			// This is needed otherwise the root element itself could not be created
			return DOMUtil.createElement(doc, qnameWithPrefix);
		}
	}

	/**
	 * Determines proper name for the element with specified local name.
	 */
	public String determineElementNamespace(Itemable parent, String elementDescriptionLocalName) {
		ItemDefinition definition = parent.getDefinition();
		if (definition == null) {
			return parent.getElementName().getNamespaceURI();
		}
		// This is very simplistic now, it assumes that all elements are in the
		// "tns" namespace.
		// TODO: Improve it later.
		return definition.getTypeName().getNamespaceURI();
	}

}
