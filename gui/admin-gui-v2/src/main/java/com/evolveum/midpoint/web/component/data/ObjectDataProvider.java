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

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class ObjectDataProvider<T extends ObjectType> extends SortableDataProvider<T> {

    private Class<T> type;

    public ObjectDataProvider(Class<T> type) {
        this.type = type;
        setSort("name", SortOrder.ASCENDING);
    }

    private ModelService getModel() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getModel();
    }

    @Override
    public Iterator<? extends T> iterator(int first, int count) {
        SortParam sortParam = getSort();
        OrderDirectionType order;
        if (sortParam.isAscending()) {
            order = OrderDirectionType.ASCENDING;
        } else {
            order = OrderDirectionType.DESCENDING;
        }

        OperationResult result = new OperationResult("list usersssss");
        PagingType paging = PagingTypeFactory.createPaging(first, count, order, sortParam.getProperty());

        ResultList<PrismObject<T>> list = getModel().listObjects(type, paging, result);

        List<T> users = new ArrayList<T>();
        for (PrismObject<T> object : list) {
            users.add(object.asObjectable());
        }

        //todo error and operation result handling

        return users.iterator();
    }

    @Override
    public int size() {
        //todo reimplement, use countObjects
        OperationResult result = new OperationResult("list objects");
        return getModel().listObjects(type,
                PagingTypeFactory.createPaging(1, 1, OrderDirectionType.ASCENDING, "name"),
                result).getTotalResultCount();
    }

    @Override
    public IModel<T> model(T object) {
        return new Model<T>(object);
    }
}
