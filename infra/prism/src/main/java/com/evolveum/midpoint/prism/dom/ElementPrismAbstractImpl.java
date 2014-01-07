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

package com.evolveum.midpoint.prism.dom;

import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.UserDataHandler;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;

/**
 * 
 * @author Radovan Semancik
 */
public abstract class ElementPrismAbstractImpl implements Element {

	private PrismValue value;
	
	ElementPrismAbstractImpl(PrismValue value) {
		super();
		this.value = value;
	}
	
	protected PrismValue getValue() {
		return value;
	}

	protected Item getItem() {
		return (Item)value.getParent();
	}
	
	protected PrismContext getPrismContext() {
		return getItem().getPrismContext();
	}
	
	protected QName getElementName() {
		return getItem().getElementName();
	}
	// NAME
	
	@Override
	public String getNodeName() {
		return getElementName().getLocalPart();
	}
	
	@Override
	public String getNamespaceURI() {
		return getElementName().getNamespaceURI();
	}

	@Override
	public String getPrefix() {
		return getElementName().getPrefix();
	}

	@Override
	public void setPrefix(String prefix) throws DOMException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public String getLocalName() {
		return getElementName().getLocalPart();
	}
	
	@Override
	public String getTagName() {
		return getElementName().getLocalPart();
	}

	// VALUE
	
	protected abstract NodeList getValueNodeList();
	
	@Override
	public String getNodeValue() throws DOMException {
		return null;
	}

	@Override
	public void setNodeValue(String nodeValue) throws DOMException {
		// no effect
	}
	
	@Override
	public abstract String getTextContent() throws DOMException;

	@Override
	public void setTextContent(String textContent) throws DOMException {
		throw new UnsupportedOperationException("The DOM element for "+getItem()+" is not writable");
	}

	@Override
	public NodeList getChildNodes() {
		return getValueNodeList();
	}

	@Override
	public Node getFirstChild() {
		NodeList valueNodeList = getValueNodeList();
		if (valueNodeList.getLength() == 0) {
			return null;
		}
		return valueNodeList.item(0);
	}

	@Override
	public Node getLastChild() {
		NodeList valueNodeList = getValueNodeList();
		if (valueNodeList.getLength() == 0) {
			return null;
		}
		return valueNodeList.item(valueNodeList.getLength()-1);
	}
	
	// TYPE and NAVIGATION

	@Override
	public short getNodeType() {
		return Node.ELEMENT_NODE;
	}

	@Override
	public Node getParentNode() {
		PrismValue parentValue = getItem().getParent();
		if (parentValue == null) {
			return null;
		} else {
			return parentValue.asDomElement();
		}
	}

	@Override
	public Node getPreviousSibling() {
		Item item = getItem();
		PrismValue previousPVal = item.getPreviousValue(value);
		if (previousPVal == null) {
			PrismValue parentValue = getItem().getParent();
			if (parentValue instanceof PrismContainerValue) {
				PrismContainerValue parentContainerValue = (PrismContainerValue)parentValue;
				Item prevItem = parentContainerValue.getPreviousItem(getItem());
				if (prevItem == null || prevItem.isEmpty()) {
					return null;
				}
				previousPVal = prevItem.getValue(-1);
			}
		}
		if (previousPVal == null) {
			return null;
		}
		return previousPVal.asDomElement();
	}

	@Override
	public Node getNextSibling() {
		Item item = getItem();
		PrismValue nextPVal = getItem().getNextValue(value);
		if (nextPVal == null) {
			PrismValue parentValue = getItem().getParent();
			if (parentValue instanceof PrismContainerValue) {
				PrismContainerValue parentContainerValue = (PrismContainerValue)parentValue;
				Item nextItem = parentContainerValue.getNextItem(getItem());
				if (nextItem == null || nextItem.isEmpty()) {
					return null;
				}
				nextPVal = nextItem.getValue(0);
			}
		}
		if (nextPVal == null) {
			return null;
		}
		return nextPVal.asDomElement();
	}

	@Override
	public NamedNodeMap getAttributes() {
		return new AttributeNamedNodeMapImpl(getAttributeMap(), getNamespaceURI(), this);
	}

	@Override
	public Document getOwnerDocument() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public abstract Node insertBefore(Node newChild, Node refChild) throws DOMException;

	@Override
	public abstract Node replaceChild(Node newChild, Node oldChild) throws DOMException;

	@Override
	public abstract Node removeChild(Node oldChild) throws DOMException;

	@Override
	public abstract Node appendChild(Node newChild) throws DOMException;

	@Override
	public abstract boolean hasChildNodes();

	@Override
	public abstract Node cloneNode(boolean deep);
	
	@Override
	public abstract NodeList getElementsByTagName(String name);

	@Override
	public abstract NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException;

	@Override
	public void normalize() {
		// nothing to do
	}

	@Override
	public boolean isSupported(String feature, String version) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}


	@Override
	public boolean hasAttributes() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getBaseURI() {
		return null;
	}

	@Override
	public short compareDocumentPosition(Node other) throws DOMException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isSameNode(Node other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String lookupPrefix(String namespaceURI) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDefaultNamespace(String namespaceURI) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public String lookupNamespaceURI(String prefix) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEqualNode(Node arg) {
		return equals(arg);
	}

	@Override
	public Object getFeature(String feature, String version) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object setUserData(String key, Object data, UserDataHandler handler) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getUserData(String key) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	// ATTRIBUTES
	
	protected Map<String,String> getAttributeMap() {
		return null;
	}
	
	@Override
	public String getAttribute(String name) {
		return getAttributeMap().get(name);
	}

	@Override
	public void setAttribute(String name, String value) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public void removeAttribute(String name) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public Attr getAttributeNode(String name) {
		return (Attr) getAttributes().getNamedItem(name);
	}

	@Override
	public Attr setAttributeNode(Attr newAttr) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public Attr removeAttributeNode(Attr oldAttr) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public String getAttributeNS(String namespaceURI, String localName) throws DOMException {
		return getAttributeNodeNS(namespaceURI, localName).getValue();
	}

	@Override
	public void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public void removeAttributeNS(String namespaceURI, String localName) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public Attr getAttributeNodeNS(String namespaceURI, String localName) throws DOMException {
		return (Attr) getAttributes().getNamedItemNS(namespaceURI, localName);
	}

	@Override
	public Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}


	@Override
	public boolean hasAttribute(String name) {
		return getAttributes().getLength() != 0;
	}

	@Override
	public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException {
		return getAttributeNodeNS(namespaceURI, localName) != null;
	}

	@Override
	public void setIdAttribute(String name, boolean isId) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public TypeInfo getSchemaTypeInfo() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return "Element/Prism(" + value + ")";
	}
	
}
