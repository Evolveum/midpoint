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

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
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
		return value.getParent();
	}
	
	protected PrismContext getPrismContext() {
		return getItem().getPrismContext();
	}
	
	protected QName getElementName() {
		return getItem().getName();
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
	public abstract Node getPreviousSibling();

	@Override
	public abstract Node getNextSibling();

	@Override
	public abstract NamedNodeMap getAttributes();

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

	@Override
	public abstract String getAttribute(String name);

	@Override
	public abstract void setAttribute(String name, String value) throws DOMException;

	@Override
	public abstract void removeAttribute(String name) throws DOMException;
	
	@Override
	public abstract Attr getAttributeNode(String name);

	@Override
	public abstract Attr setAttributeNode(Attr newAttr) throws DOMException;

	@Override
	public abstract Attr removeAttributeNode(Attr oldAttr) throws DOMException;

	@Override
	public abstract NodeList getElementsByTagName(String name);

	@Override
	public abstract String getAttributeNS(String namespaceURI, String localName) throws DOMException;

	@Override
	public abstract void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException;
	
	@Override
	public abstract void removeAttributeNS(String namespaceURI, String localName) throws DOMException;
	
	@Override
	public abstract Attr getAttributeNodeNS(String namespaceURI, String localName) throws DOMException;

	@Override
	public abstract Attr setAttributeNodeNS(Attr newAttr) throws DOMException;

	@Override
	public abstract NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException;

	@Override
	public abstract boolean hasAttribute(String name);

	@Override
	public abstract boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException;

	@Override
	public TypeInfo getSchemaTypeInfo() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public abstract void setIdAttribute(String name, boolean isId) throws DOMException;
	
	@Override
	public abstract void setIdAttributeNS(String namespaceURI, String localName, boolean isId) throws DOMException;

	@Override
	public abstract void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException;
	
}
