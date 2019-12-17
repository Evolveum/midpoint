/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
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
import com.evolveum.midpoint.web.component.util.TaskSelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author lazyman
 */
public class TaskSelectableBeanProvider extends BaseSortableDataProvider<TaskSelectableBean> {

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskSelectableBeanProvider.class);
    private static final String DOT_CLASS = TaskSelectableBeanProvider.class.getName() + ".";
    private static final String OPERATION_LIST_TASKS = DOT_CLASS + "listTasks";
    private static final String OPERATION_COUNT_TASKS = DOT_CLASS + "countTasks";

    private TaskDtoProviderOptions options;
    private PageBase pagebase;

    public TaskSelectableBeanProvider(PageBase pagebase, TaskDtoProviderOptions options) {
        super(pagebase);
        this.options = options;
        this.pagebase = pagebase;
    }

    public TaskSelectableBeanProvider(PageBase pagebase) {
        this(pagebase, TaskDtoProviderOptions.fullOptions());
    }

    @Override
    public Iterator<TaskSelectableBean> internalIterator(long first, long count) {
        Collection<String> selectedOids = getSelectedOids();
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_LIST_TASKS);
        Task operationTask = getTaskManager().createTaskInstance(OPERATION_LIST_TASKS);
        try {
            ObjectPaging paging = createPaging(first, count);
            ObjectQuery query = getQuery();
            if (query == null){
                query = getPrismContext().queryFactory().createQuery();
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

            GetOperationOptionsBuilder getOperationOptionsBuilder = getDefaultOptionsBuilder();
            if (options.isResolveNames()) {
                getOperationOptionsBuilder = getOperationOptionsBuilder.resolveNames();
            }
            Collection<SelectorOptions<GetOperationOptions>> searchOptions = getOperationOptionsBuilder
                    .items(propertiesToGet.toArray(new Object[0])).retrieve()
                    .build();

            List<PrismObject<TaskType>> tasks = getModel().searchObjects(TaskType.class, query, searchOptions, operationTask, result);
            for (PrismObject<TaskType> task : tasks) {
                try {
                    TaskSelectableBean taskDto = createTaskDto(task, false, operationTask, result);
                    getAvailableData().add(taskDto);
                } catch (Exception ex) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when getting task {} details", ex, task.getOid());
                    result.recordPartialError("Couldn't get details of task " + task.getOid(), ex);
                    // todo display the result somehow
                }
            }
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when listing tasks", ex);
            result.recordFatalError(getPage().createStringResource("TaskDtoProvider.message.internalIterator.fatalError").getString(), ex);
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
        for (TaskSelectableBean taskDto : getAvailableData()) {
            if (taskDto.isSelected()) {
                oids.add(taskDto.getValue().getOid());
            }
        }
        return oids;
    }

    public List<TaskType> getSelectedTasks() {
        List<TaskType> selected = new ArrayList<>();
        for (TaskSelectableBean taskDto : getAvailableData()) {
            if (taskDto.isSelected()) {
                selected.add(taskDto.getValue());
            }
        }
        return selected;
    }

    private void setSelectedOids(Collection<String> selectedOids) {
        for (TaskSelectableBean taskDto : getAvailableData()) {
            if (selectedOids.contains(taskDto.getValue().getOid())) {
                taskDto.setSelected(true);
            }
        }
    }

    public TaskSelectableBean createTaskDto(PrismObject<TaskType> task, boolean subtasksLoaded, Task opTask, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {

        return new TaskSelectableBean(task.asObjectable());


//        return new TaskDto(task.asObjectable(), null, getModel(), getTaskService(),
//                getModelInteractionService(), getTaskManager(), getWorkflowManager(), options, subtasksLoaded, opTask, result, pagebase);
    }

    @Override
    protected int internalSize() {
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_TASKS);
        Task task = getTaskManager().createTaskInstance(OPERATION_COUNT_TASKS);
        try {
            count = getModel().countObjects(TaskType.class, getQuery(), getDefaultOptionsBuilder().build(), task, result);
            result.recomputeStatus();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when counting tasks", ex);
            result.recordFatalError(getPage().createStringResource("TaskDtoProvider.message.internalSize.fatalError").getString(), ex);
        }
        if (!result.isSuccess()) {
            getPage().showResult(result);
        }
        return count;
    }
}
