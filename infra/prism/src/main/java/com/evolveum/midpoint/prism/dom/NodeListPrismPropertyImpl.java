package com.evolveum.midpoint.prism.dom;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeListPrismPropertyImpl implements NodeList {
	
	TextPrismPropertyImpl textNode;
	
	NodeListPrismPropertyImpl(ElementPrismPropertyImpl elementImpl) {
		super();
		this.textNode = new TextPrismPropertyImpl(elementImpl);
	}

	@Override
	public Node item(int index) {
		if (index != 0) {
			return null;
		}
		return textNode;
	}

	@Override
	public int getLength() {
		return 1;
	}

}
