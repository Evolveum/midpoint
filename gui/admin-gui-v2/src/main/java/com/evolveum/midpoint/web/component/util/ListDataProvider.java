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

import org.apache.commons.lang.Validate;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class ListDataProvider<T extends Serializable> extends SortableDataProvider<T> {

    private IModel<List<T>> model;

    public ListDataProvider(IModel<List<T>> model) {
        Validate.notNull(model);
        this.model = model;        
    }

    @Override
    public Iterator<? extends T> iterator(int first, int count) {
        List<T> list = model.getObject();
        if (list == null || list.isEmpty()) {
            return new ArrayList<T>().iterator();
        }

        List<T> out = new ArrayList<T>();
        for (int i = first; i < first + count; i++) {
            out.add(list.get(i));
        }

        return out.iterator();
    }

    @Override
    public int size() {
        List<T> list = model.getObject();
        if (list == null) {
            return 0;
        }

        return list.size();
    }

    @Override
    public IModel<T> model(T object) {
        return new Model<T>(object);
    }
}
