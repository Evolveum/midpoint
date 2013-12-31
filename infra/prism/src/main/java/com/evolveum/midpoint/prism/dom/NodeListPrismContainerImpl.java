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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;

/**
 * @author semancik
 *
 */
public class NodeListPrismContainerImpl<T extends Containerable> implements NodeList {
	
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
		for (Item<?> item: getContainerValue().getItems()) {
			if (selected(item)) {
	    		if (index < item.getValues().size()) {
	    			return item.getValue(index).asDomElement();
	    		} else {
	    			index -= item.getValues().size(); 
	    		}
			}
    	}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.NodeList#getLength()
	 */
	@Override
	public int getLength() {
		// Each item and each value are presented as one list entry
    	int size = 0;
    	for (Item<?> item: getContainerValue().getItems()) {
    		if (selected(item)) {
    			size += item.getValues().size();
    		}
    	}
        return size;
	}
	
	boolean isEmpty() {
		return getLength() == 0;
	}
	
	private PrismContainerValue<T> getContainerValue() {
		return elementPrismContainerImpl.getValue();
	}
	
	private boolean selected(Item item) {
		if (selectionLocalName == null) {
			return true;
		} else {
			if (selectionLocalName.equals(item.getElementName().getLocalPart())) {
				if (selectionNamespace == null || selectionNamespace.equals(item.getElementName().getNamespaceURI())) {
					return true;	
				}
			}
		}
		return false;
	}

}
