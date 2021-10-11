/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.xjc;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.PrismList;
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
public abstract class PrismContainerArrayList<T extends Containerable> extends AbstractList<T> implements Serializable,
        PrismList {

    private PrismContainer<T> container;
    private PrismContainerValue<?> parent;

    // For deserialization
    public PrismContainerArrayList() {
    }

    public PrismContainerArrayList(PrismContainer<T> container) {
        Validate.notNull(container);
        this.container = container;
    }

    public PrismContainerArrayList(PrismContainer<T> container, PrismContainerValue<?> parent) {
        Validate.notNull(container);
        this.container = container;
        this.parent = parent;
    }

    protected abstract PrismContainerValue getValueFrom(T t);

    protected T createItemInternal(PrismContainerValue value) {
        ComplexTypeDefinition concreteDef = value.getComplexTypeDefinition();
        if (concreteDef != null &&
                (container.getCompileTimeClass() == null ||
                        !container.getCompileTimeClass().equals(concreteDef.getCompileTimeClass()))) {
            // the dynamic definition exists and the compile time class is different from the one at the container level
            // ("different" here means it is a subclass)
            // so we have to instantiate dynamically
            T bean = null;
            try {
                bean = (T) concreteDef.getCompileTimeClass().newInstance();
            } catch (InstantiationException|IllegalAccessException|RuntimeException e) {
                throw new SystemException("Couldn't instantiate " + concreteDef.getCompileTimeClass());
            }
            bean.setupContainerValue(value);
            return bean;
        }
        else {
            // otherwise let's use the static (faster) way
            return createItem(value);
        }
    }
    protected abstract T createItem(PrismContainerValue value);

    @Override
    public T get(int i) {
        testIndex(i);

        return createItemInternal(getValues().get(i));
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

        return createItemInternal(value);
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
            if (container.getParent() == null) {
                parent.add(container);
            }
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
