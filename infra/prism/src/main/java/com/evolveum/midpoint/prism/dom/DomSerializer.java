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

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
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

	private void serialize(PrismContainerValue<?> value, Element parentElement) throws SchemaException {
		PrismContainer<?> parent = value.getParent();
		QName elementQName = parent.getName();
		Element element = createElement(elementQName);
		parentElement.appendChild(element);
		serializeItems(value.getItems(), element);
		if (value.getId() != null) {
			element.setAttribute(PrismConstants.ATTRIBUTE_ID_LOCAL_NAME, value.getId());
		}
	}
	
	private void serialize(PrismPropertyValue<?> value, Element parentElement) throws SchemaException {
		Item parent = value.getParent();
		QName elementName = parent.getName();
		Class<? extends Object> type = value.getValue().getClass();
		boolean recordType = false;
		ItemDefinition definition = parent.getDefinition();
		if (definition != null) {
			recordType = definition.isDynamic();
		}
		if (XmlTypeConverter.canConvert(type)) {
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
	
	private void serialize(PrismReferenceValue value, Element parentElement) throws SchemaException {
		Item parent = value.getParent();
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
