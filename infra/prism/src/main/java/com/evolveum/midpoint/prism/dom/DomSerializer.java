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

import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContainerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
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
	
	private PrismContext prismContext;
	private Document doc;
	private Element topElement;
	
	DomSerializer(PrismContext prismContext) {
		super();
		this.prismContext = prismContext;
	}

	private DynamicNamespacePrefixMapper getNamespacePrefixMapper() {
		return prismContext.getSchemaRegistry().getNamespacePrefixMapper();
	}

	public Element serialize(PrismObject<?> object) throws SchemaException {
		initialize();		
		Element topElement = createElement(object.getName()); 
		serializeItems(object.getValue().getItems(), topElement);
		if (object.getOid() != null) {
			topElement.setAttribute(PrismConstants.ATTRIBUTE_OID_LOCAL_NAME, object.getOid());
		}
		if (object.getVersion() != null) {
			topElement.setAttribute(PrismConstants.ATTRIBUTE_VERSION_LOCAL_NAME, object.getVersion());
		}
		if (object.getDefinition() != null &&
				!prismContext.getSchemaRegistry().hasImplicitTypeDefinition(object.getName(), object.getDefinition().getTypeName())) {
			DOMUtil.setXsiType(topElement, object.getDefinition().getTypeName());
		}
		return topElement;
	}

	private void initialize() {
		doc = DOMUtil.getDocument();
		topElement = null;
	}

	public void serialize(PrismContainerValue<?> value, Element parentElement) throws SchemaException {
		PrismContainerable parent = value.getParent();
		QName elementQName = parent.getName();
		Element element = createElement(elementQName);
		parentElement.appendChild(element);
		serializeItems(value.getItems(), element);
		if (value.getId() != null) {
			element.setAttribute(PrismConstants.ATTRIBUTE_ID_LOCAL_NAME, value.getId());
		}
	}
	
	private void serialize(PrismPropertyValue<?> value, Element parentElement) throws SchemaException {
		Itemable parent = value.getParent();
		QName elementName = parent.getName();
		if (value.getRawElement() != null) {
			// This element was not yet touched by the schema, but we still can serialize it
			serializeRawElement(value.getRawElement(), parentElement);
			return;
		}
		Class<? extends Object> type = value.getValue().getClass();
		boolean recordType = false;
		ItemDefinition definition = parent.getDefinition();
		if (definition != null) {
			recordType = definition.isDynamic();
		}
		if (value.getValue() instanceof Element) {
			// No conversion needed, just adopt the element
			Element originalValueElement = (Element)value.getValue();
			Element adoptedElement = (Element) parentElement.getOwnerDocument().importNode(originalValueElement, true);
			parentElement.appendChild(adoptedElement);
			// Make sure that all applicable namespace declarations are placed on this element
			DOMUtil.fixNamespaceDeclarations(adoptedElement);
			// HACK HACK HACK. Make sure that the declarations survive stupid XML normalization by placing them
			// in explicit elements.
			PrismUtil.fortifyNamespaceDeclarations(adoptedElement);
		} else if (XmlTypeConverter.canConvert(type)) {
			// Primitive value
			Element element = createElement(elementName);
			parentElement.appendChild(element);
			XmlTypeConverter.toXsdElement(value.getValue(), element, recordType);
		} else {
			// JAXB value
			PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
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
	
	private void serializeRawElement(Object rawElement, Element parentElement) throws SchemaException {
		if (rawElement instanceof Element) {
			Document ownerDocument = parentElement.getOwnerDocument();
			Element adoptedElement = (Element) ownerDocument.adoptNode((Element)rawElement);
			parentElement.appendChild(adoptedElement);
		} else if (rawElement instanceof JAXBElement){
			PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
			try {
				jaxbProcessor.marshalElementToDom((JAXBElement)rawElement, parentElement);
			} catch (JAXBException e) {
				throw new SchemaException("Error processing element "+rawElement+": "+e.getMessage(),e);
			}
		} else {
			throw new IllegalArgumentException("Cannot process raw element "+rawElement+" of type "+rawElement.getClass());
		}
	}

	private void serialize(PrismReferenceValue value, Element parentElement) throws SchemaException {
		Itemable parent = value.getParent();
		Element element = createElement(parent.getName());
		parentElement.appendChild(element);
		element.setAttribute(PrismConstants.ATTRIBUTE_OID_LOCAL_NAME, value.getOid());
		if (value.getTargetType() != null) {
			try {
				DOMUtil.setQNameAttribute(element, PrismConstants.ATTRIBUTE_REF_TYPE_LOCAL_NAME, value.getTargetType());
			} catch (IllegalArgumentException e) {
				throw new SchemaException(e.getMessage()+" in type field of reference "+parent.getName());
			}
		}
	}
	
	private void serializeItems(List<Item<?>> items, Element parentElement) throws SchemaException {
		for (Item<?> item: items) {
			serialize(item, parentElement);
		}
	}

	private <V extends PrismValue> void serialize(Item<V> item, Element parentElement) throws SchemaException {
		for (V pval: item.getValues()) {
			if (!item.equals(pval.getParent())) {
				throw new IllegalArgumentException("The parent for value "+pval+" of item "+item+" is incorrect");
			}
			serialize(pval, parentElement);
		}
	}
	
	private void serialize(PrismValue pval, Element parentElement) throws SchemaException {
		if (pval instanceof PrismContainerValue) {
			serialize((PrismContainerValue)pval, parentElement);
		} else if (pval instanceof PrismPropertyValue) {
			serialize((PrismPropertyValue)pval, parentElement);
		} else if (pval instanceof PrismReferenceValue) {
			serialize((PrismReferenceValue)pval, parentElement);
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

}
