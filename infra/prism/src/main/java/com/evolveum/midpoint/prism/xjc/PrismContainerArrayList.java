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

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import org.apache.commons.lang.Validate;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 *
 * Changed to extend AbstractList instead of ArrayList, as some functionality of ArrayList
 * (e.g. its optimized Itr class) does not work with class (PrismContainerArrayList), as of Java7.
 *
 * TODO: account for concurrent structural modifications using modCount property
 */
public abstract class PrismContainerArrayList<T extends Containerable> extends AbstractList<T> implements Serializable {

    private PrismContainer<T> container;

    // For deserialization
    public PrismContainerArrayList() {
    }
    
    public PrismContainerArrayList(PrismContainer<T> container) {
        Validate.notNull(container);
        this.container = container;
    }

    protected abstract T createItem(PrismContainerValue value);

    protected abstract PrismContainerValue getValueFrom(T t);

    @Override
    public T get(int i) {
        testIndex(i);

        return createItem(getValues().get(i));
    }

    private List<PrismContainerValue<T>> getValues() {
        return container.getValues();
    }

    @Override
    public int size() {
        return getValues().size();
    }

    private void testIndex(int i) {
        if (i < 0 || i >= getValues().size()) {
            throw new IndexOutOfBoundsException("Can't get index '" + i
                    + "', values size is '" + getValues().size() + "'.");
        }
    }

    @Override
    public T remove(int i) {
        testIndex(i);

        PrismContainerValue value = getValues().get(i);
        getValues().remove(i);

        return createItem(value);
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
    public boolean remove(Object o) {
        T t = (T) o;
        PrismContainerValue value = getValueFrom(t);
        return container.remove(value);
    }

    @Override
    public boolean add(T t) {
        PrismContainerValue value = getValueFrom(t);
        try {
            return container.add(value);
        } catch (SchemaException ex) {
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean addAll(Collection<? extends T> ts) {
        boolean changed = false;
        for (T t : ts) {
            if (!changed) {
                changed = add(t);
            } else {
                add(t);
            }
        }
        return changed;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }
}
