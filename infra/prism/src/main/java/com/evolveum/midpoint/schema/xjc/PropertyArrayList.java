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

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;

import org.apache.commons.lang.Validate;

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
public class PropertyArrayList<T> extends AbstractList<T> {

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
        return (T) getPropertyValue(index).getValue();
    }

    @Override
    public boolean addAll(Collection<? extends T> ts) {
        Validate.notNull(ts, "Collection must not be null.");

        if (ts.isEmpty()) {
            return false;
        }

        for (T object : ts) {
            property.addValue(new PrismPropertyValue<Object>(object, null, null));
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
        Collection<Object> list = new ArrayList<Object>();
        list.add(o);
        return removeAll(list);
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        return super.removeAll(objects); //todo implement
    }
}
