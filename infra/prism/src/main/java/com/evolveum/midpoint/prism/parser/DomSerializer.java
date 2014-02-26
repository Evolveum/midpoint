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
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.JaxbTestUtil;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.SchemaXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

/**
 * @author semancik
 *
 */
public class DomSerializer {
	
	private Document doc;
	private Element topElement;
	private boolean serializeCompositeObjects = false;
	private SchemaRegistry schemaRegistry;
	
	DomSerializer(DomParser parser, SchemaRegistry schemaRegistry) {
		super();
		this.schemaRegistry = schemaRegistry;
	}
	
	public boolean isSerializeCompositeObjects() {
		return serializeCompositeObjects;
	}

	public void setSerializeCompositeObjects(boolean serializeCompositeObjects) {
		this.serializeCompositeObjects = serializeCompositeObjects;
	}

	private DynamicNamespacePrefixMapper getNamespacePrefixMapper() {
		if (schemaRegistry == null) {
			return null;
		}
		return schemaRegistry.getNamespacePrefixMapper();
	}

	private void initialize() {
		doc = DOMUtil.getDocument();
		topElement = null;
	}

	public Element serialize(RootXNode rootxnode) throws SchemaException {
		initialize();
		QName rootElementName = rootxnode.getRootElementName();
		Element topElement = createElement(rootxnode.getRootElementName());
		QName typeQName = rootxnode.getTypeQName();
		if (typeQName != null && !schemaRegistry.hasImplicitTypeDefinition(rootElementName, typeQName)) {
			DOMUtil.setXsiType(topElement, rootxnode.getTypeQName());
		}
		XNode subnode = rootxnode.getSubnode();
		if (!(subnode instanceof MapXNode)) {
			throw new SchemaException("Sub-root xnode is not map, cannot serialize to XML (it is "+subnode+")");
		}
		serializeMap((MapXNode)subnode, topElement);
		return topElement;
	}
	
	public Element serializeToElement(MapXNode xmap, QName elementName) throws SchemaException {
		initialize();
		Element element = createElement(elementName);
		serializeMap(xmap, element);
		return element;
	}
	
	private void serializeMap(MapXNode xmap, Element topElement) throws SchemaException {
		for (Entry<QName,XNode> entry: xmap.entrySet()) {
			QName elementQName = entry.getKey();
			XNode xsubnode = entry.getValue();
			if (xsubnode instanceof ListXNode) {
				ListXNode xlist = (ListXNode)xsubnode;
				for (XNode xsubsubnode: xlist) {
					serializeSubnode(xsubsubnode, topElement, elementQName);
				}
			} else {
				serializeSubnode(xsubnode, topElement, elementQName);
			}
		}		
	}
	
	private void serializeSubnode(XNode xsubnode, Element parentElement, QName elementName) throws SchemaException {
		if (xsubnode == null) {
			return;
		}
		if (xsubnode instanceof MapXNode) {
			Element element = createElement(elementName);
			if (xsubnode.isExplicitTypeDeclaration() && xsubnode.getTypeQName() != null){
				DOMUtil.setXsiType(element, xsubnode.getTypeQName());
			}
			parentElement.appendChild(element);
			serializeMap((MapXNode)xsubnode, element);
		} else if (xsubnode instanceof PrimitiveXNode<?>) {
			PrimitiveXNode<?> xprim = (PrimitiveXNode<?>)xsubnode;
			if (xprim.isAttribute()) {
				serializePrimitiveAttribute(xprim, parentElement, elementName);
			} else {
				serializePrimitiveElement(xprim, parentElement, elementName);
			}
		} else if (xsubnode instanceof ListXNode) {
			ListXNode xlist = (ListXNode)xsubnode;
			for (XNode xsubsubnode: xlist) {
				serializeSubnode(xsubsubnode, parentElement, elementName);
			}
		} else if (xsubnode instanceof SchemaXNode) {
			serializeSchema((SchemaXNode)xsubnode, parentElement);
		} else {
			throw new IllegalArgumentException("Unknown subnode "+xsubnode);
		}
	}

	private <T> void serializePrimitiveAttribute(PrimitiveXNode<T> xprim, Element parentElement, QName attributeName) {
    	QName typeQName = xprim.getTypeQName();
    	if (typeQName.equals(DOMUtil.XSD_QNAME)) {
    		QName value = (QName) xprim.getValue();
    		try {
    			DOMUtil.setQNameAttribute(parentElement, attributeName.getLocalPart(), value);
    		} catch (DOMException e) {
    			throw new DOMException(e.code, e.getMessage() + "; setting attribute "+attributeName.getLocalPart()+" in element "+DOMUtil.getQName(parentElement)+" to QName value "+value);
    		}
    	} else {
    		String value = xprim.getFormattedValue();
    		parentElement.setAttribute(attributeName.getLocalPart(), value);
    	}		
	}

	private void serializePrimitiveElement(PrimitiveXNode<?> xprim, Element parentElement, QName elementName) {
		QName typeQName = xprim.getTypeQName();
		if (typeQName == null) {
			if (PrismContext.isAllowSchemalessSerialization()) {
				// We cannot correctly serialize without a type. But this is needed
				// sometimes. So just default to string
				String stringValue = xprim.getStringValue();
				if (stringValue != null) {
					Element element;
					try {
						element = createElement(elementName);
					} catch (DOMException e) {
						throw new DOMException(e.code, e.getMessage() + "; creating element "+elementName+" in element "+DOMUtil.getQName(parentElement));
					}
					parentElement.appendChild(element);
					element.setTextContent(stringValue);
				}
			} else {
				throw new IllegalStateException("No type for primitive element "+elementName+", cannot serialize (schemaless serialization is disabled)");
			}
		} else  if (typeQName.equals(ItemPath.XSD_TYPE)) {
    		ItemPath itemPath = (ItemPath)xprim.getValue();
    		if (itemPath != null){
	    		XPathHolder holder = new XPathHolder(itemPath);
	    		Element element = holder.toElement(elementName, parentElement.getOwnerDocument());
	    		parentElement.appendChild(element);
    		}
		} else {
			Element element;
			try {
				element = createElement(elementName);
			} catch (DOMException e) {
				throw new DOMException(e.code, e.getMessage() + "; creating element "+elementName+" in element "+DOMUtil.getQName(parentElement));
			}
			parentElement.appendChild(element);
			
			if (xprim.isExplicitTypeDeclaration()) {
				DOMUtil.setXsiType(element, typeQName);
			}
			
	    	if (typeQName.equals(DOMUtil.XSD_QNAME)) {
	    		QName value = (QName) xprim.getValue();
				DOMUtil.setQNameValue(element, value);
	    	} else {
	    		String value = xprim.getFormattedValue();
	    		element.setTextContent(value);
	    	}
		}
	}
    
    private void serializeSchema(SchemaXNode xschema, Element parentElement) {
		Element schemaElement = xschema.getSchemaElement();
		if (schemaElement == null){
			return;
		}
		Element clonedSchema = (Element) schemaElement.cloneNode(true);
		doc.adoptNode(clonedSchema);
		parentElement.appendChild(clonedSchema);
	}

	/**
	 * Create XML element with the correct namespace prefix and namespace definition.
	 * @param qname element QName
	 * @return created DOM element
	 */
	private Element createElement(QName qname) {
		String namespaceURI = qname.getNamespaceURI();
		if (StringUtils.isBlank(namespaceURI)) {
			return doc.createElement(qname.getLocalPart());
		}
		QName qnameWithPrefix = setQNamePrefix(qname);
		if (topElement != null) {
			return DOMUtil.createElement(doc, qnameWithPrefix, topElement, topElement);
		} else {
			// This is needed otherwise the root element itself could not be created
			return DOMUtil.createElement(doc, qnameWithPrefix);
		}
	}

	private QName setQNamePrefix(QName qname) {
		DynamicNamespacePrefixMapper namespacePrefixMapper = getNamespacePrefixMapper();
		if (namespacePrefixMapper == null) {
			return qname;
		}
		return namespacePrefixMapper.setQNamePrefix(qname);
	}
	
	private QName setQNamePrefixExplicit(QName qname) {
		DynamicNamespacePrefixMapper namespacePrefixMapper = getNamespacePrefixMapper();
		if (namespacePrefixMapper == null) {
			return qname;
		}
		return namespacePrefixMapper.setQNamePrefixExplicit(qname);
	}

}
