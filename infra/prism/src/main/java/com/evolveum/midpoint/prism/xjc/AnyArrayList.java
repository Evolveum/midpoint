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

package com.evolveum.midpoint.prism.xjc;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContainerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * 
 * @author Radovan Semancik
 */
public class AnyArrayList<T extends Containerable> extends AbstractList<Object> {

    private PrismContainerValue<T> containerValue;
    private Document document;

    public AnyArrayList(PrismContainerValue<T> containerValue) {
        Validate.notNull(containerValue, "Container value must not be null.");
        this.containerValue = containerValue;
        this.document = DOMUtil.getDocument();
    }

    @Override
    public int size() {
    	if (isSchemaless()) {
    		return getRawElements().size();
    	} else {
	    	// Each item and each value are presented as one list entry
	    	int size = 0;
	    	for (Item<?> item: containerValue.getItems()) {
	    		size += item.getValues().size();
	    	}
	        return size;
    	}
    }

    @Override
    public Object get(int index) {
    	if (isSchemaless()) {
    		return getRawElements().get(index);
    	} else {
	    	for (Item<?> item: containerValue.getItems()) {
	    		if (index < item.getValues().size()) {
	    			return asElement(item.getValue(index));
	    		} else {
	    			index -= item.getValues().size(); 
	    		}
	    	}
	    	throw new IndexOutOfBoundsException();
    	}
    }

	@Override
    public boolean addAll(Collection<? extends Object> elements) {
        Validate.notNull(elements, "Collection must not be null.");

        if (elements.isEmpty()) {
            return false;
        }

        for (Object element : elements) {
            add(element);
        }

        return true;
    }

    @Override
    public boolean addAll(int i, Collection<? extends Object> elements) {
        return addAll(elements);
    }

    @Override
    public boolean add(Object element) {
    	if (isSchemaless()) {
    		return getRawElements().add(element);
    	} else {
	    	QName elementName = JAXBUtil.getElementQName(element);
	    	Item<?> item;
			try {
				item = containerValue.findOrCreateItem(elementName);
			} catch (SchemaException e1) {
				// this should not happen
				throw new IllegalStateException("Internal schema error: "+e1.getMessage(),e1);
			}
	    	try {
				return getPrismContext().getPrismDomProcessor().addItemValue(item, element, getContainer());
			} catch (SchemaException e) {
				throw new IllegalArgumentException("Element "+elementName+" cannot be added because is violates object schema: "+e.getMessage(),e);
			}
    	}
    }

	@Override
    public void add(int i, Object element) {
        add(element);
    }

    @Override
    public Object remove(int index) {
    	if (isSchemaless()) {
    		return getRawElements().remove(index);
    	} else {
    		for (Item<?> item: containerValue.getItems()) {
	    		if (index < item.getValues().size()) {
	    			item.remove(index);
	    		} else {
	    			index -= item.getValues().size(); 
	    		}
	    	}
	    	throw new IndexOutOfBoundsException();
    	}
    }

    @Override
    public boolean remove(Object element) {
    	if (isSchemaless()) {
    		return getRawElements().remove(element);
    	} else {
    		QName elementName = JAXBUtil.getElementQName(element);
        	Item<?> item = containerValue.findItem(elementName);
        	try {
				return getPrismContext().getPrismDomProcessor().deleteItemValue(item, element, getContainer());
			} catch (SchemaException e) {
				throw new IllegalArgumentException("Element "+elementName+" cannot be removed because is violates object schema: "+e.getMessage(),e);
			}
    	}
    }
    
    @Override
    public boolean removeAll(Collection<?> objects) {
        boolean changed = false;
        for (Object object : objects) {
            if (!changed) {
                changed = remove(object);
            } else {
                remove(object);
            }
        }
        return changed;
    }
    
    private PrismContainerDefinition getDefinition() {
    	PrismContainerable<T> parent = containerValue.getParent();
    	if (parent == null) {
    		return null;
    	}
    	return parent.getDefinition();
    }
    
    private boolean isSchemaless() {
    	return getDefinition() == null;
    }
    
    private List<Object> getRawElements() {
    	return containerValue.getRawElements();
    }
    
    private PrismContainer<T> getContainer() {
    	return containerValue.getContainer();
    }
    
    private PrismContext getPrismContext() {
    	return getContainer().getPrismContext();
    }
        
	private Object asElement(PrismValue itemValue) {
		PrismContext prismContext = containerValue.getPrismContext();
        if (prismContext == null) {
            throw new IllegalStateException("prismContext is null in " + containerValue);
        }
		try {
			return prismContext.getPrismJaxbProcessor().toAny(itemValue, document);
		} catch (SchemaException e) {
			throw new SystemException("Unexpected schema problem: "+e.getMessage(),e);
		}
		// return itemValue.asDomElement();
	}
	

}
