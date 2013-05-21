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

/**
 * @author semancik
 *
 */
public class AttributeNodeImpl implements Attr {
	
	private QName name;
	private String value;
	private ElementPrismAbstractImpl parent;
	private AttributeNamedNodeMapImpl nodeMap;
	private int index;
	
	public AttributeNodeImpl(QName attrQName, String value) {
		this.name = attrQName;
		this.value = value;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
	
	public ElementPrismAbstractImpl getParent() {
		return parent;
	}

	public void setParent(ElementPrismAbstractImpl parent) {
		this.parent = parent;
	}

	public AttributeNamedNodeMapImpl getNodeMap() {
		return nodeMap;
	}

	public void setNodeMap(AttributeNamedNodeMapImpl nodeMap) {
		this.nodeMap = nodeMap;
	}

	@Override
	public String getNodeName() {
		return name.getLocalPart();
	}

	@Override
	public String getNodeValue() throws DOMException {
		return value;
	}

	@Override
	public void setNodeValue(String nodeValue) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public short getNodeType() {
		return Node.ATTRIBUTE_NODE;
	}

	@Override
	public Node getParentNode() {
		return parent;
	}

	@Override
	public NodeList getChildNodes() {
		return null;
	}

	@Override
	public Node getFirstChild() {
		return null;
	}

	@Override
	public Node getLastChild() {
		return null;
	}

	@Override
	public Node getPreviousSibling() {
		return nodeMap.getPrevious(this);
	}

	@Override
	public Node getNextSibling() {
		return nodeMap.getNext(this);
	}

	@Override
	public NamedNodeMap getAttributes() {
		return null;
	}

	@Override
	public Document getOwnerDocument() {
		return parent.getOwnerDocument();
	}

	@Override
	public Node insertBefore(Node newChild, Node refChild) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public Node removeChild(Node oldChild) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public Node appendChild(Node newChild) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public boolean hasChildNodes() {
		return false;
	}

	@Override
	public Node cloneNode(boolean deep) {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public void normalize() {
	}

	@Override
	public boolean isSupported(String feature, String version) {
		return false;
	}

	@Override
	public String getNamespaceURI() {
		return name.getNamespaceURI();
	}

	@Override
	public String getPrefix() {
		return name.getPrefix();
	}

	@Override
	public void setPrefix(String prefix) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public String getLocalName() {
		return name.getLocalPart();
	}

	@Override
	public boolean hasAttributes() {
		return false;
	}

	@Override
	public String getBaseURI() {
		return null;
	}

	@Override
	public short compareDocumentPosition(Node other) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public String getTextContent() throws DOMException {
		return value;
	}

	@Override
	public void setTextContent(String textContent) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public boolean isSameNode(Node other) {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public String lookupPrefix(String namespaceURI) {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public boolean isDefaultNamespace(String namespaceURI) {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public String lookupNamespaceURI(String prefix) {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public boolean isEqualNode(Node arg) {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public Object getFeature(String feature, String version) {
		return null;
	}

	@Override
	public Object setUserData(String key, Object data, UserDataHandler handler) {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public Object getUserData(String key) {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public String getName() {
		return name.getLocalPart();
	}

	@Override
	public boolean getSpecified() {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public void setValue(String value) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public Element getOwnerElement() {
		return parent;
	}

	@Override
	public TypeInfo getSchemaTypeInfo() {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public boolean isId() {
		return false;
	}

}
