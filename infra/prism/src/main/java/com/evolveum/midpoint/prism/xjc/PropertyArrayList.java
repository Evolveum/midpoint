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

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import org.apache.commons.lang.Validate;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * This class is used to wrap {@link PrismProperty} values for JAXB objects with
 * {@link java.util.List} properties.
 * <p/>
 * This list implementation is based on {@link java.util.Set} so indexes are
 * not guaranteed. Objects positions can change in time :)
 *
 * @author lazyman
 */
public class PropertyArrayList<T> extends AbstractList<T> implements Serializable {

    private PrismProperty property;

    public PropertyArrayList(PrismProperty property) {
        Validate.notNull(property, "Property must not be null.");
        this.property = property;
    }

    @Override
    public int size() {
        return property.getValues().size();
    }

    @Override
    public T get(int index) {
        //todo fix PropertyValue set generics in Property, Property class should be generifiable
    	Object propertyRealValue = getPropertyValue(index).getValue();
        return (T) JaxbTypeConverter.mapPropertyRealValueToJaxb(propertyRealValue);
    }

    @Override
    public boolean addAll(Collection<? extends T> ts) {
        Validate.notNull(ts, "Collection must not be null.");

        if (ts.isEmpty()) {
            return false;
        }

        for (T jaxbObject : ts) {
        	Object propertyRealValue = JaxbTypeConverter.mapJaxbToPropertyRealValue(jaxbObject);
            property.addValue(new PrismPropertyValue<Object>(propertyRealValue, null, null));
        }

        return true;
    }

    @Override
    public boolean addAll(int i, Collection<? extends T> ts) {
        return addAll(ts);
    }

    @Override
    public boolean add(T t) {
        Collection<T> collection = new ArrayList<T>();
        collection.add(t);

        return addAll(collection);
    }

    @Override
    public void add(int i, T t) {
        add(t);
    }

    private PrismPropertyValue<Object> getPropertyValue(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Can't get object on position '"
                    + index + "', list size is '" + size() + "'.");
        }

        //at least we try to get object on index defined by parameter
        Iterator<PrismPropertyValue<Object>> iterator = property.getValues().iterator();
        for (int i = 0; i < index; i++) {
            iterator.next();
        }

        return iterator.next();
    }

    @Override
    public T remove(int i) {
        PrismPropertyValue<Object> value = getPropertyValue(i);
        property.deleteValue(value);

        return (T) value.getValue();
    }

    @Override
    public boolean remove(Object o) {
        PrismPropertyValue<Object> value = null;
        Iterator<PrismPropertyValue<Object>> iterator = property.getValues().iterator();
        while (iterator.hasNext()) {
            PrismPropertyValue prismValue = iterator.next();
            if (o != null && o.equals(prismValue.getValue())) {
                value = prismValue;
                break;
            } else if (o == null && prismValue.getValue() == null) {
                value = prismValue;
                break;
            }
        }

        if (value == null) {
            return false;
        }

        return property.deleteValue(value);
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
}
