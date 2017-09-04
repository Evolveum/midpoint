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

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import org.apache.commons.lang.Validate;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 *
 * TODO: account for concurrent structural modifications using modCount property
 */
public abstract class PrismReferenceArrayList<T> extends AbstractList<T>  implements Serializable {

    private PrismReference reference;
    private PrismContainerValue<?> parent;

    public PrismReferenceArrayList(PrismReference reference, PrismContainerValue<?> parent) {
        Validate.notNull(reference, "Prism reference must not be null.");
        this.reference = reference;
        this.parent = parent;
    }

    protected PrismReference getReference() {
        return reference;
    }

    @Override
    public T get(int i) {
        testIndex(i);

        return createItem(getReference().getValues().get(i));
    }

    @Override
    public int size() {
        return reference.getValues().size();
    }

    protected abstract T createItem(PrismReferenceValue value);

    protected abstract PrismReferenceValue getValueFrom(T t);

    private void testIndex(int i) {
        if (i < 0 || i >= getReference().getValues().size()) {
            throw new IndexOutOfBoundsException("Can't get index '" + i
                    + "', values size is '" + getReference().getValues().size() + "'.");
        }
    }

    @Override
    public T remove(int i) {
        testIndex(i);

        PrismReferenceValue value = reference.getValues().get(i);
        reference.getValues().remove(i);

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
        PrismReferenceValue value = getValueFrom(t);
        return reference.getValues().remove(value);
    }

    @Override
    public boolean add(T t) {
        PrismReferenceValue value = getValueFrom(t);
        if (reference.getParent() == null) {
            try {
                parent.add(reference);
            } catch (SchemaException e) {
                throw new SystemException(e.getMessage(), e);
            }
        }
        return reference.merge(value);
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

    /**
     * JAXB unmarshaller is calling clear() on lists even though they were just
     * created. As the references should be visible as two JAXB fields, clearing one
     * of them will also clear the other. Therefore we need this hack. Calling clear()
     * will only clear the values that naturally "belong" to the list.
     */
    @Override
	public void clear() {
    	List<PrismReferenceValue> values = reference.getValues();
    	if (values == null) {
    		return;
    	}
    	Iterator<PrismReferenceValue> iterator = values.iterator();
    	while (iterator.hasNext()) {
    		PrismReferenceValue value = iterator.next();
    		if (willClear(value)) {
    			iterator.remove();
    		}
    	}
	}

	protected abstract boolean willClear(PrismReferenceValue value);

	@Override
    public boolean isEmpty() {
        return size() == 0;
    }
}
