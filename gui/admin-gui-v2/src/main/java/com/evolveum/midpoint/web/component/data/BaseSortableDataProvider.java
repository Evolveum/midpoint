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
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.wf.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.Validate;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class BaseSortableDataProvider<T extends Serializable> extends SortableDataProvider<T> {

    private PageBase page;
    private List<T> availableData;

    private QueryType query;

    public BaseSortableDataProvider(PageBase page) {
        Validate.notNull(page, "Page must not be null.");
        this.page = page;

        setSort("name", SortOrder.ASCENDING);
    }

    protected ModelService getModel() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getModel();
    }

    protected RepositoryService getRepository() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getRepository();
    }

    protected TaskManager getTaskManager() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getTaskManager();
    }

    protected WorkflowManager getWorkflowManager() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getWorkflowManager();
    }

    public List<T> getAvailableData() {
        if (availableData == null) {
            availableData = new ArrayList<T>();
        }
        return availableData;
    }

    @Override
    public IModel<T> model(T object) {
        return new Model<T>(object);
    }

    protected PageBase getPage() {
        return page;
    }

    public QueryType getQuery() {
        return query;
    }

    public void setQuery(QueryType query) {
        this.query = query;
    }

    protected PagingType createPaging(int first, int count) {
        SortParam sortParam = getSort();
        OrderDirectionType order;
        if (sortParam.isAscending()) {
            order = OrderDirectionType.ASCENDING;
        } else {
            order = OrderDirectionType.DESCENDING;
        }

        return PagingTypeFactory.createPaging(first, count, order, sortParam.getProperty());
    }
}
