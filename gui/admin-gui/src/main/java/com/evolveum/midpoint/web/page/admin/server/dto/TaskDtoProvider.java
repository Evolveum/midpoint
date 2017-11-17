/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.Component;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author lazyman
 */
public class TaskDtoProvider extends BaseSortableDataProvider<TaskDto> {

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskDtoProvider.class);
    private static final String DOT_CLASS = TaskDtoProvider.class.getName() + ".";
    private static final String OPERATION_LIST_TASKS = DOT_CLASS + "listTasks";
    private static final String OPERATION_COUNT_TASKS = DOT_CLASS + "countTasks";

    private TaskDtoProviderOptions options;
    private Component component;

    public TaskDtoProvider(Component component, TaskDtoProviderOptions options) {
        super(component);
        this.options = options;
        this.component = component;
    }

    public TaskDtoProvider(Component component) {
        this(component, TaskDtoProviderOptions.fullOptions());
    }

    @Override
    public Iterator<? extends TaskDto> internalIterator(long first, long count) {
		Collection<String> selectedOids = getSelectedOids();
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

            List<QName> propertiesToGet = new ArrayList<>();
            if (options.isUseClusterInformation()) {
                propertiesToGet.add(TaskType.F_NODE_AS_OBSERVED);
            }
            if (options.isGetNextRunStartTime()) {
                propertiesToGet.add(TaskType.F_NEXT_RUN_START_TIMESTAMP);
                propertiesToGet.add(TaskType.F_NEXT_RETRY_TIMESTAMP);
            }
            Collection<SelectorOptions<GetOperationOptions>> searchOptions =
                    GetOperationOptions.createRetrieveAttributesOptions(propertiesToGet.toArray(new QName[0]));
            List<PrismObject<TaskType>> tasks = getModel().searchObjects(TaskType.class, query, searchOptions, operationTask, result);
            for (PrismObject<TaskType> task : tasks) {
                try {
                    TaskDto taskDto = createTaskDto(task, operationTask, result);
                    getAvailableData().add(taskDto);
                } catch (Exception ex) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when getting task {} details", ex, task.getOid());
                    result.recordPartialError("Couldn't get details of task " + task.getOid(), ex);
                    // todo display the result somehow
                }
            }
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when listing tasks", ex);
            result.recordFatalError("Couldn't list tasks.", ex);
        } finally {
            if (result.hasUnknownStatus()) {
                result.recomputeStatus();
            }
        }
		setSelectedOids(selectedOids);
        return getAvailableData().iterator();
    }

	private Collection<String> getSelectedOids() {
		Set<String> oids = new HashSet<>();
		for (TaskDto taskDto : getAvailableData()) {
			if (taskDto.isSelected()) {
				oids.add(taskDto.getOid());
			}
		}
		return oids;
	}

	private void setSelectedOids(Collection<String> selectedOids) {
		for (TaskDto taskDto : getAvailableData()) {
			if (selectedOids.contains(taskDto.getOid())) {
				taskDto.setSelected(true);
			}
		}
	}

	public TaskDto createTaskDto(PrismObject<TaskType> task, Task opTask, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {

        return new TaskDto(task.asObjectable(), null, getModel(), getTaskService(),
                getModelInteractionService(), getTaskManager(), getWorkflowManager(), options, opTask, result, (PageBase)component);
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
            LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when counting tasks", ex);
            result.recordFatalError("Couldn't count tasks.", ex);
        }
        if (!result.isSuccess()) {
            getPage().showResult(result);
        }
        return count;
    }
}
