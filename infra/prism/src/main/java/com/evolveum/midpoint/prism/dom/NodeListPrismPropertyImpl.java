package com.evolveum.midpoint.prism.dom;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeListPrismPropertyImpl implements NodeList {
	
	ElementPrismPropertyImpl elementImpl;
	
	NodeListPrismPropertyImpl(ElementPrismPropertyImpl elementImpl) {
		super();
		this.elementImpl = elementImpl;
	}

	@Override
	public Node item(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLength() {
		// TODO Auto-generated method stub
		return 0;
	}

}
