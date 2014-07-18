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

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.SchemaXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.sun.org.apache.xml.internal.utils.XMLChar;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.Map.Entry;

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

    private void initializeWithExistingDocument(Document document) {
        doc = document;
        topElement = document.getDocumentElement();         // TODO is this ok?
    }

	public Element serialize(RootXNode rootxnode) throws SchemaException {
		initialize();
        return serializeInternal(rootxnode);
    }

    // this one is used only from within JaxbDomHack.toAny(..) - hopefully it will disappear soon
    @Deprecated
    public Element serialize(RootXNode rootxnode, Document document) throws SchemaException {
        initializeWithExistingDocument(document);
        return serializeInternal(rootxnode);
    }

    private Element serializeInternal(RootXNode rootxnode) throws SchemaException {
		QName rootElementName = rootxnode.getRootElementName();
		Element topElement = createElement(rootElementName, null);
		QName typeQName = rootxnode.getTypeQName();
        if (typeQName == null && rootxnode.getSubnode().getTypeQName() != null) {
            typeQName = rootxnode.getSubnode().getTypeQName();
        }
		if (typeQName != null && !schemaRegistry.hasImplicitTypeDefinition(rootElementName, typeQName)) {
			DOMUtil.setXsiType(topElement, typeQName);
		}
		XNode subnode = rootxnode.getSubnode();
		if (subnode instanceof PrimitiveXNode){
			serializePrimitiveElementOrAttribute((PrimitiveXNode) subnode, topElement, rootElementName, false);
			return DOMUtil.getFirstChildElement(topElement);
		} 
		if (!(subnode instanceof MapXNode)) {
			throw new SchemaException("Sub-root xnode is not map, cannot serialize to XML (it is "+subnode+")");
		}
		serializeMap((MapXNode)subnode, topElement);
		return topElement;
	}
	
	public Element serializeToElement(MapXNode xmap, QName elementName) throws SchemaException {
		initialize();
		Element element = createElement(elementName, null);
        topElement = element;
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
        if (xsubnode instanceof RootXNode) {
            Element element = createElement(elementName, parentElement);
            appendCommentIfPresent(element, xsubnode);
            parentElement.appendChild(element);
            serializeSubnode(((RootXNode) xsubnode).getSubnode(), element, ((RootXNode) xsubnode).getRootElementName());
        } else if (xsubnode instanceof MapXNode) {
			Element element = createElement(elementName, parentElement);
            appendCommentIfPresent(element, xsubnode);
			if (xsubnode.isExplicitTypeDeclaration() && xsubnode.getTypeQName() != null){
				DOMUtil.setXsiType(element, xsubnode.getTypeQName());
			}
			parentElement.appendChild(element);
//			System.out.println("subnode " + xsubnode.debugDump());
			serializeMap((MapXNode)xsubnode, element);
		} else if (xsubnode instanceof PrimitiveXNode<?>) {
			PrimitiveXNode<?> xprim = (PrimitiveXNode<?>)xsubnode;
			if (xprim.isAttribute()) {
                serializePrimitiveElementOrAttribute(xprim, parentElement, elementName, true);
			} else {
				serializePrimitiveElementOrAttribute(xprim, parentElement, elementName, false);
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

	public Element serializeXPrimitiveToElement(PrimitiveXNode<?> xprim, QName elementName) throws SchemaException {
		initialize();
		Element parent = DOMUtil.createElement(doc, new QName("fake","fake"));
		serializePrimitiveElementOrAttribute(xprim, parent, elementName, false);
		return DOMUtil.getFirstChildElement(parent);
	}

	private void serializePrimitiveElementOrAttribute(PrimitiveXNode<?> xprim, Element parentElement, QName elementOrAttributeName, boolean asAttribute) throws SchemaException {
		QName typeQName = xprim.getTypeQName();

        // if typeQName is not explicitly specified, we try to determine it from parsed value
        // TODO we should probably set typeQName when parsing the value...
        if (typeQName == null && xprim.isParsed()) {
            Object v = xprim.getValue();
            if (v != null) {
                typeQName = XsdTypeMapper.toXsdType(v.getClass());
            }
        }

        if (typeQName == null) {
			if (com.evolveum.midpoint.prism.PrismContext.isAllowSchemalessSerialization()) {
				// We cannot correctly serialize without a type. But this is needed
				// sometimes. So just default to string
				String stringValue = xprim.getStringValue();
				if (stringValue != null) {
                    if (asAttribute) {
                        DOMUtil.setAttributeValue(parentElement, elementOrAttributeName.getLocalPart(), stringValue);
                    } else {
                        Element element;
                        try {
                            element = createElement(elementOrAttributeName, parentElement);
                            appendCommentIfPresent(element, xprim);
                        } catch (DOMException e) {
                            throw new DOMException(e.code, e.getMessage() + "; creating element "+elementOrAttributeName+" in element "+DOMUtil.getQName(parentElement));
                        }
                        parentElement.appendChild(element);
                        DOMUtil.setElementTextContent(element, stringValue);
                    }
				}
                return;
			} else {
				throw new IllegalStateException("No type for primitive element "+elementOrAttributeName+", cannot serialize (schemaless serialization is disabled)");
			}
		}

        // typeName != null after this point

        if (StringUtils.isBlank(typeQName.getNamespaceURI())) {
            typeQName = XsdTypeMapper.determineQNameWithNs(typeQName);
        }

        Element element = null;

        if (typeQName.equals(ItemPath.XSD_TYPE)) {
            ItemPath itemPath = (ItemPath)xprim.getValue();
            if (itemPath != null) {
                if (asAttribute) {
                    throw new UnsupportedOperationException("Serializing ItemPath as an attribute is not supported yet");
                }
                XPathHolder holder = new XPathHolder(itemPath);
                element = holder.toElement(elementOrAttributeName, parentElement.getOwnerDocument());
                parentElement.appendChild(element);
            }

        } else {
            // not an ItemType

            if (!asAttribute) {
                try {
                    element = createElement(elementOrAttributeName, parentElement);
                } catch (DOMException e) {
                    throw new DOMException(e.code, e.getMessage() + "; creating element "+elementOrAttributeName+" in element "+DOMUtil.getQName(parentElement));
                }
                appendCommentIfPresent(element, xprim);
                parentElement.appendChild(element);
            }

            if (typeQName.equals(DOMUtil.XSD_QNAME)) {
                QName value = (QName) xprim.getParsedValueWithoutRecording(typeQName);
                if (asAttribute) {
                    try {
                        DOMUtil.setQNameAttribute(parentElement, elementOrAttributeName.getLocalPart(), value);
                    } catch (DOMException e) {
                        throw new DOMException(e.code, e.getMessage() + "; setting attribute "+elementOrAttributeName.getLocalPart()+" in element "+DOMUtil.getQName(parentElement)+" to QName value "+value);
                    }
                } else {
                    DOMUtil.setQNameValue(element, value);
                }
            } else {
                // not ItemType nor QName
                String value = xprim.getGuessedFormattedValue();

                if (asAttribute) {
                    DOMUtil.setAttributeValue(parentElement, elementOrAttributeName.getLocalPart(), value);
                } else {
                    DOMUtil.setElementTextContent(element, value);
                }
            }

        }
        if (!asAttribute && xprim.isExplicitTypeDeclaration()) {
            DOMUtil.setXsiType(element, typeQName);
        }
	}

    private void appendCommentIfPresent(Element element, XNode xnode) {
        String text = xnode.getComment();
        if (StringUtils.isNotEmpty(text)) {
            DOMUtil.createComment(element, text);
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
	private Element createElement(QName qname, Element parentElement) {
		String namespaceURI = qname.getNamespaceURI();
		if (!StringUtils.isBlank(namespaceURI)) {
			qname = setQNamePrefix(qname);
		}
		if (parentElement != null) {
			return DOMUtil.createElement(doc, qname, parentElement, parentElement);
		} else {
			// This is needed otherwise the root element itself could not be created
            // Caller of this method is responsible for setting the topElement
			return DOMUtil.createElement(doc, qname);
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
