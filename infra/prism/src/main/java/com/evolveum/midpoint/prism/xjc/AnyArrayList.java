/*
 * Copyright (c) 2010-2015 Evolveum
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

import java.util.AbstractList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

/**
 * A list used for JAXB getAny() methods.
 * It is not used for normal prism operation, not even if compilte-time (JAXB) classes are used.
 * It is quite a bad way to use getAny() methods from the JAXB classes, it is much better to use
 * prism facet instead. However we need this to be fully JAXB compliant and therefore support
 * XML marshalling/unmarshalling. This is important e.g. for JAX-WS.
 *
 * @author Radovan Semancik
 */
public class AnyArrayList<C extends Containerable> extends AbstractList<Object> {

    private PrismContainerValue<C> containerValue;

    public AnyArrayList(PrismContainerValue<C> containerValue) {
        Validate.notNull(containerValue, "Container value must not be null.");
        this.containerValue = containerValue;
    }

    @Override
    public int size() {
    	if (isSchemaless()) {
			throw new UnsupportedOperationException("Definition-less containers are not supported any more.");
    	} else {
	    	// Each item and each value are presented as one list entry
    		// (multi-valued items are represented as multiple occurrences of the same element)
	    	int size = 0;
	    	if (containerValue.isEmpty()){
	    		return size;
	    	}
	    	for (Item<?,?> item: containerValue.getItems()) {
	    		size += item.getValues().size();
	    	}
	        return size;
    	}
    }

    @Override
    public Object get(int index) {
    	if (isSchemaless()) {
			throw new UnsupportedOperationException("Definition-less containers are not supported any more.");
    	} else {
			if (containerValue != null) {
				for (Item<?,?> item : containerValue.getItems()) {
					if (index < item.getValues().size()) {
						return asElement(item.getValue(index));
					} else {
						index -= item.getValues().size();
					}
				}
				throw new IndexOutOfBoundsException();
			}
			return null;  //TODO: is this OK??
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
    	try {
    		return containerValue.addRawElement(element);
		} catch (SchemaException e) {
			QName elementName = JAXBUtil.getElementQName(element);
			throw new IllegalArgumentException("Element "+elementName+" cannot be added because is violates object schema: "+e.getMessage(),e);
		}
    }

	@Override
    public void add(int i, Object element) {
        add(element);
    }

    @Override
    public Object remove(int index) {
    	if (isSchemaless()) {
			throw new UnsupportedOperationException("Definition-less containers are not supported any more.");
    	} else {
    		for (Item<?,?> item: containerValue.getItems()) {
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
    		return containerValue.removeRawElement(element);
    	} else {
        	try {
        		return containerValue.deleteRawElement(element);
			} catch (SchemaException e) {
	    		QName elementName = JAXBUtil.getElementQName(element);
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

    private boolean isSchemaless() {
    	return containerValue.getComplexTypeDefinition() == null;
    }

    private PrismContainer<C> getContainer() {
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
			return prismContext.getJaxbDomHack().toAny(itemValue);
		} catch (SchemaException e) {
			throw new SystemException("Unexpected schema problem: "+e.getMessage(),e);
		}
	}


}
