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

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
import org.apache.wicket.Component;

import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class TaskDtoProvider extends BaseSortableDataProvider<TaskDto> {

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskDtoProvider.class);
    private static final String DOT_CLASS = TaskDtoProvider.class.getName() + ".";
    private static final String OPERATION_LIST_TASKS = DOT_CLASS + "listTasks";
    private static final String OPERATION_COUNT_TASKS = DOT_CLASS + "countTasks";

    private TaskDtoProviderOptions options;

    public TaskDtoProvider(Component component, TaskDtoProviderOptions options) {
        super(component);
        this.options = options;
    }

    public TaskDtoProvider(Component component) {
        this(component, TaskDtoProviderOptions.fullOptions());
    }

    @Override
    public Iterator<? extends TaskDto> internalIterator(long first, long count) {
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_LIST_TASKS);
        Task operationTask = getTaskManager().createTaskInstance(OPERATION_LIST_TASKS);
        try {
        	ObjectPaging paging = createPaging(first, count);
        	ObjectQuery query = getQuery();
        	if (query == null){
        		query = new ObjectQuery();
        	}
        	query.setPaging(paging);
        	
            List<PrismObject<TaskType>> tasks = getModel().searchObjects(TaskType.class, query, null, operationTask, result);
            for (PrismObject<TaskType> task : tasks) {
                try {
                    TaskDto taskDto = new TaskDto(task.asObjectable(), getModel(), getTaskService(), getModelInteractionService(), getTaskManager(), options, result);
                    getAvailableData().add(taskDto);
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Unhandled exception when getting task {} details", ex, task.getOid());
                    result.recordPartialError("Couldn't get details of task " + task.getOid(), ex);
                    // todo display the result somehow
                }
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Unhandled exception when listing tasks", ex);
            result.recordFatalError("Couldn't list tasks.", ex);
        } finally {
            if (result.hasUnknownStatus()) {
                result.recomputeStatus();
            }
        }
        return getAvailableData().iterator();
    }

    @Override
    protected int internalSize() {
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_TASKS);
        Task task = getTaskManager().createTaskInstance(OPERATION_COUNT_TASKS);
        try {
            count = getModel().countObjects(TaskType.class, getQuery(), null, task, result);
            result.recomputeStatus();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Unhandled exception when counting tasks", ex);
            result.recordFatalError("Couldn't count tasks.", ex);
        }
        if (!result.isSuccess()) {
            getPage().showResult(result);
        }
        return count;
    }
}
