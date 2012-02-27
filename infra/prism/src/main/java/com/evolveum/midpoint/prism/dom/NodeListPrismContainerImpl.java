/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.prism.dom;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;

/**
 * @author semancik
 *
 */
public class NodeListPrismContainerImpl<T> implements NodeList {
	
	private ElementPrismContainerImpl<T> elementPrismContainerImpl;
	private String selectionNamespace;
	private String selectionLocalName;

	NodeListPrismContainerImpl(ElementPrismContainerImpl<T> elementPrismContainerImpl) {
		this.elementPrismContainerImpl = elementPrismContainerImpl;
	}

	NodeListPrismContainerImpl(ElementPrismContainerImpl<T> elementPrismContainerImpl,
			String selectionNamespace, String selectionLocalName) {
		this.elementPrismContainerImpl = elementPrismContainerImpl;
		this.selectionNamespace = selectionNamespace;
		this.selectionLocalName = selectionLocalName;
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.NodeList#item(int)
	 */
	@Override
	public Node item(int index) {
		return (Node) getValueList().get(index).asDomElements();
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.NodeList#getLength()
	 */
	@Override
	public int getLength() {
		return getValueList().size();
	}
	
	boolean isEmpty() {
		return getValueList().isEmpty();
	}
	
	private List<Item<?>> getValueList() {
		PrismContainerValue<T> pvalue = elementPrismContainerImpl.getValue();
		if (selectionLocalName == null) {
			return pvalue.getItems();
		} else {
			List<Item<?>> list = new ArrayList<Item<?>>();
			for (Item<?> item: pvalue.getItems()) {
				if (selectionLocalName.equals(item.getName().getLocalPart())) {
					if (selectionNamespace == null || selectionNamespace.equals(item.getName().getNamespaceURI())) {
						list.add(item);	
					}
				}
			}
			return list;
		}
	}

}
