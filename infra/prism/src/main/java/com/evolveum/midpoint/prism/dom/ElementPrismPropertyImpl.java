package com.evolveum.midpoint.prism.dom;

import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

public class ElementPrismPropertyImpl extends ElementPrismAbstractImpl {
	
	private NodeList valueNodeList;
	private int index;
	
	ElementPrismPropertyImpl(PrismPropertyValue value, int index) {
		super(value);
		valueNodeList = new NodeListPrismPropertyImpl(this);
		this.index = index;
	}
	
	protected PrismPropertyValue getValue() {
		return (PrismPropertyValue)super.getValue();
	}
	
	protected PrismProperty getProperty() {
		return (PrismProperty)getItem();
	}

	@Override
	public String getTextContent() throws DOMException {
		return getXmlValue();
	}
	
	String getXmlValue() {
		// TODO: Special handling for QNames
		PrismPropertyValue propertyValue = getValue();
		String xmlStringValue = XmlTypeConverter.toXmlTextContent(propertyValue, getElementName());
		return xmlStringValue;
	}
	
	void setXmlValue(String newValue) {
		PrismPropertyValue propertyValue = getValue();
		PrismProperty property = getProperty();
		Class type = property.getValueClass();
		Object javaValue = XmlTypeConverter.toJavaValue(newValue, type);
		propertyValue.setValue(javaValue);
	}
	
	protected NodeList getValueNodeList() {
		return valueNodeList;
	}

	@Override
	public NodeList getElementsByTagName(String name) {
		return NodeListEmptyImpl.IMMUTABLE_EMPTY_NODELIST;
	}

	@Override
	public Node getPreviousSibling() {
		if (index == 0) {
			return null;
		}
		PrismProperty property = getProperty();
		PrismPropertyValue<Object> previousPVal = property.getValues().get(index-1);
		return previousPVal.asDomElement();
	}

	@Override
	public Node getNextSibling() {
		List<PrismPropertyValue<Object>> pvals = getProperty().getValues();
		if (index >= pvals.size()-1) {
			return null;
		}
		PrismPropertyValue<Object> nextPVal = pvals.get(index+1);
		return nextPVal.asDomElement();
	}

	@Override
	public NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException {
		return null;
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
		return (getValue().getValue() != null);
	}

	@Override
	public Node cloneNode(boolean deep) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "DOM clonning is not supported (yet)");
	}

	// ATTRIBUTES
	
	// TODO: namespace declarations for qnames
	
	@Override
	public NamedNodeMap getAttributes() {
		// TODO Auto-generated method stub
		return NamedNodeMapEmptyImpl.IMMUTABLE_EMPTY_NAMED_NODE_MAP;
	}
	
	@Override
	public String getAttribute(String name) {
		return null;
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
		return null;
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
		// TODO Auto-generated method stub
		return null;
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
		return null;
	}

	@Override
	public Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public boolean hasAttribute(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setIdAttribute(String name, boolean isId) throws DOMException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

	@Override
	public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This kind of modification is not supported (yet)");
	}

}
