/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.xjc;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.util.PrismList;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

/**
 * This class is used to wrap {@link PrismProperty} values for JAXB objects with
 * {@link java.util.List} properties.
 * <p>
 * This list implementation is based on {@link java.util.Set} so indexes are
 * not guaranteed. Objects positions can change in time :)
 *
 * @author lazyman
 */
public class PropertyArrayList<T> extends AbstractList<T> implements Serializable, PrismList {

    private PrismProperty property;
    private PrismContainerValue<?> parent;

    public PropertyArrayList(@NotNull PrismProperty property, @NotNull PrismContainerValue<?> parent) {
        this.property = property;
        this.parent = parent;
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

        try {
            if (property.getParent() == null) {
                parent.add(property);
            }
        } catch (SchemaException e) {
            throw new SystemException(e.getMessage(), e);
        }

        for (T jaxbObject : ts) {
            Object propertyRealValue = JaxbTypeConverter.mapJaxbToPropertyRealValue(jaxbObject);
            property.addValue(new PrismPropertyValueImpl(propertyRealValue, null, null));
        }

        return true;
    }

    @Override
    public boolean addAll(int i, Collection<? extends T> ts) {
        return addAll(ts);
    }

    @Override
    public boolean add(T t) {
        return addAll(Collections.singleton(t));
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

    @Override
    public T set(int index, T element) {

        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Can't set object on position '"
                    + index + "', list size is '" + size() + "'.");
        }
        Object propertyRealValue = JaxbTypeConverter.mapJaxbToPropertyRealValue(element);
        getPropertyValue(index).setValue(propertyRealValue);

        return element;
    }
}
