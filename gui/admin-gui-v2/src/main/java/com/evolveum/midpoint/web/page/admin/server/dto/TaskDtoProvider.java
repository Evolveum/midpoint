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

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ClusterStatusInformation;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
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
public class TaskDtoProvider extends SortableDataProvider<TaskDto> {

    private static final String OPERATION_LIST_TASKS = "taskDtoProvider.listTasks";
    private static final String OPERATION_COUNT_TASKS = "taskDtoProvider.countTasks";
    private PageBase page;
    private QueryType query;
    private List<TaskDto> availableData;

    private static final long ALLOWED_CLUSTER_INFO_AGE = 1200L;

    public TaskDtoProvider(PageBase page) {
        Validate.notNull(page, "Page must not be null.");
        this.page = page;

        setSort("name", SortOrder.ASCENDING);
    }

    private TaskManager getTaskManager() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getTaskManager();
    }

    @Override
    public Iterator<? extends TaskDto> iterator(int first, int count) {
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_LIST_TASKS);
        try {
            SortParam sortParam = getSort();
            OrderDirectionType order;
            if (sortParam.isAscending()) {
                order = OrderDirectionType.ASCENDING;
            } else {
                order = OrderDirectionType.DESCENDING;
            }

            PagingType paging = PagingTypeFactory.createPaging(first, count, order, sortParam.getProperty());

            TaskManager manager = getTaskManager();
            ClusterStatusInformation info = manager.getRunningTasksClusterwide(ALLOWED_CLUSTER_INFO_AGE);
            List<Task> tasks = manager.searchTasks(query, paging, info, result);

            for (Task task : tasks) {
                getAvailableData().add(new TaskDto(task, info, manager));
            }
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't list tasks.", ex);
        }

        return getAvailableData().iterator();
    }

    public List<TaskDto> getAvailableData() {
        if (availableData == null) {
            availableData = new ArrayList<TaskDto>();
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
        try {
            return getTaskManager().countTasks(query, new OperationResult("size"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    @Override
    public IModel<TaskDto> model(TaskDto object) {
        return new Model<TaskDto>(object);
    }
}
