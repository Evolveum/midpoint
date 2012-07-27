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

package com.evolveum.midpoint.prism.xjc;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import org.apache.commons.lang.Validate;

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
public abstract class PrismContainerArrayList<T extends Containerable> extends AbstractList<T> {

    private PrismContainer<T> container;

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
