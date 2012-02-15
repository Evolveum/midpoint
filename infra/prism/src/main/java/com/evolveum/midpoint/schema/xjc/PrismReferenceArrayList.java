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

    @Override
    public T get(int i) {
        List<PrismReferenceValue> values = reference.getValues();
        if (i < 0 || i >= values.size()) {
            throw new IndexOutOfBoundsException("Can't get index '" + i
                    + "', values size is '" + values.size() + "'.");
        }

        return createItem(values.get(i));
    }

    @Override
    public int size() {
        return reference.getValues().size();
    }

    public abstract T createItem(PrismReferenceValue value);

    //todo implement add/remove methods
}
