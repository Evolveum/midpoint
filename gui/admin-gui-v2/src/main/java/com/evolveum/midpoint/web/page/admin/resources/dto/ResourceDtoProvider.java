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

package com.evolveum.midpoint.web.page.admin.resources.dto;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
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
public class ResourceDtoProvider extends SortableDataProvider<ResourceDto> {

    private static final String DOT_CLASS = ResourceDtoProvider.class.getName() + ".";
    private static final String OPERATION_LIST_RESOURCES = DOT_CLASS + "listResources";
    private static final String OPERATION_COUNT_RESOURCES = DOT_CLASS + "countResources";
    private PageBase page;
    private QueryType query;
    private List<ResourceDto> availableData;


    public ResourceDtoProvider(PageBase page) {
        Validate.notNull(page, "Page must not be null.");
        this.page = page;

        setSort("name", SortOrder.ASCENDING);
    }

    private ModelService getModel() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getModel();
    }

    private TaskManager getTaskManager() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getTaskManager();
    }

    @Override
    public Iterator<? extends ResourceDto> iterator(int first, int count) {
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_LIST_RESOURCES);
        try {
            SortParam sortParam = getSort();
            OrderDirectionType order;
            if (sortParam.isAscending()) {
                order = OrderDirectionType.ASCENDING;
            } else {
                order = OrderDirectionType.DESCENDING;
            }

            PagingType paging = PagingTypeFactory.createPaging(first, count, order, sortParam.getProperty());
            Task task = getTaskManager().createTaskInstance(OPERATION_LIST_RESOURCES);
            List<PrismObject<ResourceType>> resources = getModel().searchObjects(ResourceType.class, getQuery(),
                    paging, task, result);

            for (PrismObject<ResourceType> resource : resources) {
                ResourceType resourceType = resource.asObjectable();

                PrismObject<ConnectorType> connector = resolveConnector(resourceType, task, result);
                ConnectorType connectorType = connector != null ? connector.asObjectable() : null;

                getAvailableData().add(new ResourceDto(resourceType, connectorType));
            }
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't list resources.", ex); //todo i18n
        }

        return getAvailableData().iterator();
    }

    private PrismObject<ConnectorType> resolveConnector(ResourceType resource, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException {

        ObjectReferenceType ref = resource.getConnectorRef();
        String oid = ref != null ? ref.getOid() : null;
        if (StringUtils.isEmpty(oid)) {
            return null;
        }

        return getModel().getObject(ConnectorType.class, oid, null, task, result);
    }

    public List<ResourceDto> getAvailableData() {
        if (availableData == null) {
            availableData = new ArrayList<ResourceDto>();
        }
        return availableData;
    }

    public QueryType getQuery() {
        return query;
    }

    public void setQuery(QueryType query) {
        this.query = query;
    }

    @Override
    public int size() {
        OperationResult result = new OperationResult(OPERATION_COUNT_RESOURCES);
        int count = 0;
        try {
            Task task = getTaskManager().createTaskInstance(OPERATION_COUNT_RESOURCES);
            count = getModel().countObjects(ResourceType.class, getQuery(), task, result);

            result.recordSuccess();
        } catch (Exception ex) {
            result.recomputeStatus();
            result.recordFatalError("Couldn't count resource objects.", ex);
        }

        if (!result.isSuccess()) {
            page.showResult(result);
        }

        return count;
    }

    @Override
    public IModel<ResourceDto> model(ResourceDto object) {
        return new Model<ResourceDto>(object);
    }
}
