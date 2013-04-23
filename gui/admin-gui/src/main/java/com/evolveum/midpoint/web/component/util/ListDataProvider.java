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

package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class ListDataProvider<T extends Serializable> extends BaseSortableDataProvider<T> {

    private IModel<List<T>> model;

    public ListDataProvider(Component Component, IModel<List<T>> model) {
        super(Component);

        Validate.notNull(model);
        this.model = model;
    }

    @Override
    public Iterator<? extends T> internalIterator(long first, long count) {
        getAvailableData().clear();

        List<T> list = model.getObject();
        if (list != null) {
            for (long i = first; i < first + count; i++) {
                if (i < 0 || i >= list.size()) {
                    throw new ArrayIndexOutOfBoundsException("Trying to get item on index " + i
                            + " but list size is " + list.size());
                }
                getAvailableData().add(list.get(WebMiscUtil.safeLongToInteger(i)));
            }
        }

        return getAvailableData().iterator();
    }

    @Override
    protected int internalSize() {
        List<T> list = model.getObject();
        if (list == null) {
            return 0;
        }

        return list.size();
    }
}
