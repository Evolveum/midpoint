/*
 * Copyright (c) 2012 Evolveum
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
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.schema.xjc;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.commons.lang.Validate;

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
public class AnyArrayList<T> extends AbstractList<Object> {

    private PrismContainerValue<T> containerValue;

    public AnyArrayList(PrismContainerValue<T> containerValue) {
        Validate.notNull(containerValue, "Container value must not be null.");
        this.containerValue = containerValue;
    }

    @Override
    public int size() {
    	if (isSchemaless()) {
    		return getElements().size();
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
    		return getElements().get(index);
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
    		return getElements().add(element);
    	} else {
	    	QName elementName = JAXBUtil.getElementQName(element);
	    	Item<?> item = containerValue.findOrCreateItem(elementName);
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
    		return getElements().remove(index);
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
    		return getElements().remove(element);
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
    	PrismContainer<T> container = getContainer();
    	if (container == null) {
    		return null;
    	}
    	return container.getDefinition();
    }
    
    private boolean isSchemaless() {
    	return getDefinition() == null;
    }
    
    private List<Object> getElements() {
    	return containerValue.getElements();
    }
    
    private PrismContainer<T> getContainer() {
    	return containerValue.getParent();
    }
    
    private PrismContext getPrismContext() {
    	return getContainer().getPrismContext();
    }
        
	private Object asElement(PrismValue itemValue) {
		return itemValue.asDomElement();
	}
	

}
