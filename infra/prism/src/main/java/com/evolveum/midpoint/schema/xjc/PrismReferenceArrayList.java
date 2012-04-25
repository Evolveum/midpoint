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

import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import org.apache.commons.lang.Validate;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class PrismReferenceArrayList<T> extends AbstractList<T> {

    private PrismReference reference;

    public PrismReferenceArrayList(PrismReference reference) {
        Validate.notNull(reference, "Prism reference must not be null.");
        this.reference = reference;
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
