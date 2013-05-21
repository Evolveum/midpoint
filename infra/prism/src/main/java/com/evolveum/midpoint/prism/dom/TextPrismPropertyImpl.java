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

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;

public class TextPrismPropertyImpl implements Text {
	
	ElementPrismPropertyImpl elementImpl;
	
	TextPrismPropertyImpl(ElementPrismPropertyImpl elementImpl) {
		super();
		this.elementImpl = elementImpl;
	}
	
	private String getContent() {
		return elementImpl.getXmlValue();
	}
	
	private void setContent(String content) {
		elementImpl.setXmlValue(content);
	}

	@Override
	public String getData() throws DOMException {
		return getContent();
	}

	@Override
	public void setData(String data) throws DOMException {
		setContent(data);
	}

	@Override
	public int getLength() {
		return getContent().length();
	}

	@Override
	public String substringData(int offset, int count) throws DOMException {
		return getContent().substring(offset, offset + count);
	}

	@Override
	public void appendData(String arg) throws DOMException {
		setContent(getContent() + arg);
	}

	@Override
	public void insertData(int offset, String arg) throws DOMException {
		setContent(getContent().substring(0, offset) + arg + getContent().substring(offset));
	}

	@Override
	public void deleteData(int offset, int count) throws DOMException {
		setContent(getContent().substring(0, offset) + getContent().substring(offset+count));
	}

	@Override
	public void replaceData(int offset, int count, String arg) throws DOMException {
		setContent(getContent().substring(0, offset) + arg + getContent().substring(offset + arg.length()));
	}

	@Override
	public String getNodeName() {
		return "#text";
	}

	@Override
	public String getNodeValue() throws DOMException {
		return getContent();
	}

	@Override
	public void setNodeValue(String nodeValue) throws DOMException {
		setContent(nodeValue);
	}

	@Override
	public short getNodeType() {
		return Node.TEXT_NODE;
	}

	@Override
	public Node getParentNode() {
		return elementImpl;
	}

	@Override
	public NodeList getChildNodes() {
		return NodeListEmptyImpl.IMMUTABLE_EMPTY_NODELIST;
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
		return null;
	}

	@Override
	public Node getNextSibling() {
		return null;
	}

	@Override
	public NamedNodeMap getAttributes() {
		return NamedNodeMapEmptyImpl.IMMUTABLE_EMPTY_NAMED_NODE_MAP;
	}

	@Override
	public Document getOwnerDocument() {
		return elementImpl.getOwnerDocument();
	}

	@Override
	public Node insertBefore(Node newChild, Node refChild) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No node can be inserted under the "+elementImpl.getNodeName()+" node");
	}

	@Override
	public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No node can be inserted under the "+elementImpl.getNodeName()+" node");
	}

	@Override
	public Node removeChild(Node oldChild) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No node can be inserted under the "+elementImpl.getNodeName()+" node");
	}

	@Override
	public Node appendChild(Node newChild) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No node can be inserted under the "+elementImpl.getNodeName()+" node");
	}

	@Override
	public boolean hasChildNodes() {
		return false;
	}

	@Override
	public Node cloneNode(boolean deep) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "DOM clonning is not supported (yet)");
	}

	@Override
	public void normalize() {
		// nothing to do
	}

	@Override
	public boolean isSupported(String feature, String version) {
		return getParentNode().isSupported(feature, version);
	}

	@Override
	public String getNamespaceURI() {
		return null;
	}

	@Override
	public String getPrefix() {
		return null;
	}

	@Override
	public void setPrefix(String prefix) throws DOMException {
		throw new DOMException(DOMException.INVALID_ACCESS_ERR, "Not supported");
	}

	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public boolean hasAttributes() {
		return false;
	}

	@Override
	public String getBaseURI() {
		return getParentNode().getBaseURI();
	}

	@Override
	public short compareDocumentPosition(Node other) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Operation is not supported (yet)");
	}

	@Override
	public String getTextContent() throws DOMException {
		return getContent();
	}

	@Override
	public void setTextContent(String textContent) throws DOMException {
		setContent(textContent);
	}

	@Override
	public boolean isSameNode(Node other) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Operation is not supported (yet)");
	}

	@Override
	public String lookupPrefix(String namespaceURI) {
		return getParentNode().lookupPrefix(namespaceURI);
	}

	@Override
	public boolean isDefaultNamespace(String namespaceURI) {
		return getParentNode().isDefaultNamespace(namespaceURI);
	}

	@Override
	public String lookupNamespaceURI(String prefix) {
		return getParentNode().lookupNamespaceURI(prefix);
	}

	@Override
	public boolean isEqualNode(Node arg) {
		if (arg.getNodeType() == this.getNodeType()) {
			return getContent().equals(arg.getNodeValue());
		}
		return false;
	}

	@Override
	public Object getFeature(String feature, String version) {
		return getParentNode().getFeature(feature, version);
	}

	@Override
	public Object setUserData(String key, Object data, UserDataHandler handler) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Operation is not supported (yet)");
	}

	@Override
	public Object getUserData(String key) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Operation is not supported (yet)");
	}

	@Override
	public Text splitText(int offset) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Operation is not supported (yet)");
	}

	@Override
	public boolean isElementContentWhitespace() {
		return false;
	}

	@Override
	public String getWholeText() {
		return getContent();
	}

	@Override
	public Text replaceWholeText(String content) throws DOMException {
		setContent(content);
		return this;
	}

}
