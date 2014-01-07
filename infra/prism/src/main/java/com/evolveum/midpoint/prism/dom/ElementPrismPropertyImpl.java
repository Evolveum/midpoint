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

import javax.xml.bind.JAXBException;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

public class ElementPrismPropertyImpl<T> extends ElementPrismAbstractImpl {
	
	private PrismPropertyValue value;
	private NodeList valueNodeList;
	private Element delegateElement;
	
	public ElementPrismPropertyImpl(PrismPropertyValue value) {
		super(value);
		this.value = value;
		
	}
	
	private void lazyInit() {
		if (valueNodeList != null || delegateElement != null) {
			// Nothing to do, already initialized
			return;
		}
		
		Class<? extends Object> type = value.getValue().getClass();
		if (XmlTypeConverter.canConvert(type)) {
			// Primitive value
			this.valueNodeList = new NodeListPrismPropertyImpl(this);
		
		} else {
			// JAXB value
			PrismContext prismContext = getItem().getPrismContext();
			PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
			if (jaxbProcessor.canConvert(type)) {
				try {
					delegateElement = jaxbProcessor.marshalObjectToDom(value.getValue(), getItem().getElementName(), (Document)null);
				} catch (JAXBException e) {
					DOMException domException = new DOMException(DOMException.INVALID_STATE_ERR, "Error converting the value of type "+type+": "+e.getMessage());
					domException.initCause(e);
					throw domException;
				}
			} else {
				throw new DOMException(DOMException.INVALID_STATE_ERR, "The value of type "+type+" cannot be converted to DOM");
			}
		}
	}
	
	private boolean isDelegate() {
		return delegateElement != null;
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
		String xmlStringValue = XmlTypeConverter.toXmlTextContent(propertyValue.getValue(), getElementName());
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
		lazyInit();
		if (isDelegate()) {
			return delegateElement.getChildNodes();
		}
		return valueNodeList;
	}

	@Override
	public NodeList getElementsByTagName(String name) {
		return NodeListEmptyImpl.IMMUTABLE_EMPTY_NODELIST;
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
		lazyInit();
		if (isDelegate()) {
			return delegateElement.hasChildNodes();
		}
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
		return false;
	}

	@Override
	public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException {
		return false;
	}

	@Override
	public void setIdAttribute(String name, boolean isId) throws DOMException {

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
