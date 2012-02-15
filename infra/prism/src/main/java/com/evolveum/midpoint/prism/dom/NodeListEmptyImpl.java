package com.evolveum.midpoint.prism.dom;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeListEmptyImpl implements NodeList {

	public static final NodeList IMMUTABLE_EMPTY_NODELIST = new NodeListEmptyImpl();

	@Override
	public Node item(int index) {
		return null;
	}

	@Override
	public int getLength() {
		return 0;
	}

}
