package com.evolveum.midpoint.prism.dom;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class NamedNodeMapEmptyImpl implements NamedNodeMap {

	public static final NamedNodeMap IMMUTABLE_EMPTY_NAMED_NODE_MAP = new NamedNodeMapEmptyImpl();

	@Override
	public Node getNamedItem(String name) {
		return null;
	}

	@Override
	public Node setNamedItem(Node arg) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This NodeMap is immutable");
	}

	@Override
	public Node removeNamedItem(String name) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This NodeMap is immutable");
	}

	@Override
	public Node item(int index) {
		return null;
	}

	@Override
	public int getLength() {
		return 0;
	}

	@Override
	public Node getNamedItemNS(String namespaceURI, String localName) throws DOMException {
		return null;
	}

	@Override
	public Node setNamedItemNS(Node arg) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This NodeMap is immutable");
	}

	@Override
	public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This NodeMap is immutable");
	}

}
