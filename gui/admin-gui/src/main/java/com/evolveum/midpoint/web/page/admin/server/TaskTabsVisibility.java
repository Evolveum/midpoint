/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Used to determine whether tabs have to be refreshed - by comparing instances of this class before and after task update.
 *
 * @author mederly
 */
class TaskTabsVisibility implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(TaskTabsVisibility.class);

    private boolean basicVisible;
    private boolean schedulingVisible;
    private boolean workManagementVisible;
    private boolean subtasksAndThreadsVisible;
    private boolean cleanupPolicyVisible;
    private boolean progressVisible;
    private boolean environmentalPerformanceVisible;
    private boolean internalPerformanceVisible;
    private boolean operationVisible;
    private boolean resultVisible;
    private boolean errorsVisible;

    public boolean computeBasicVisible(PageTask parentPage, TaskType task) {
        basicVisible = !WebComponentUtil.isWorkflowTask(task);
        return basicVisible;
    }

    public boolean computeSchedulingVisible(PageTask parentPage, TaskType task) {
        schedulingVisible = !WebComponentUtil.isWorkflowTask(task);
        return schedulingVisible;
    }

    public boolean computeWorkManagementVisible(TaskType taskType) {
        String taskHandler = taskType.getHandlerUri();
        if (WebComponentUtil.hasArchetypeAssignment(taskType, SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value()) || (taskHandler != null &&
                (taskHandler.endsWith("task/lightweight-partitioning/handler-3")
                        || taskHandler.endsWith("model/partitioned-focus-validity-scanner/handler-3")
                        || taskHandler.endsWith("model/synchronization/task/partitioned-reconciliation/handler-3")
                        || taskHandler.endsWith("task/generic-partitioning/handler-3")
                        || taskHandler.endsWith("task/workers-creation/handler-3")))) {
            workManagementVisible = true;
        }
        return workManagementVisible;
    }

    public boolean computeCleanupPolicyVisible() {
        cleanupPolicyVisible = false;   //todo when cleanup policy should be visible?
        return cleanupPolicyVisible;
    }

    public boolean computeSubtasksAndThreadsVisible(TaskType task) {

        List<ObjectReferenceType> subtasks = task.getSubtaskRef();
        if (CollectionUtils.isEmpty(subtasks)) {
            return subtasksAndThreadsVisible = false;
        }

        boolean allEmpty = true;
        for (ObjectReferenceType subtask : subtasks) {
            if (!subtask.asReferenceValue().isEmpty()) {
                allEmpty = false;
            }
        }

        return subtasksAndThreadsVisible = !allEmpty;

        // TODO we want to show subtasks always when subtasks exist. Following should be the behavior for tables on subtasks tab.
//        boolean isThreadsReadable = isTaskItemReadable(taskWrapper, ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_WORKER_THREADS));
//        TaskType task = taskWrapper.getObject().asObjectable();
//
//        if (parentPage.isEditingFocus()) {
//            subtasksAndThreadsVisible = configuresWorkerThreads(task) && isThreadsReadable;
//        } else if (!parentPage.isAdd() && !WebComponentUtil.isWorkflowTask(taskWrapper.getObject().asObjectable())) {
//            subtasksAndThreadsVisible = configuresWorkerThreads(task) && isThreadsReadable
//                    || !CollectionUtils.isNotEmpty(task.getSubtaskRef());
//        } else {
//            subtasksAndThreadsVisible = false;
//        }
//        return subtasksAndThreadsVisible;
    }

    public boolean configuresWorkerThreads(TaskType task) {
        return WebComponentUtil.isReconciliation(task) || WebComponentUtil.isImport(task) || WebComponentUtil.isRecomputation(task) || isExecuteChanges(task.getHandlerUri())
                || isShadowIntegrityCheck(task.getHandlerUri()) || isFocusValidityScanner(task.getHandlerUri()) || isTriggerScanner(task.getHandlerUri());
    }

    public boolean computeEnvironmentalPerformanceVisible(PageTask parentPage, PrismObjectWrapper<TaskType> taskWrapper) {
        final OperationStatsType operationStats = taskWrapper.getObject().asObjectable().getOperationStats();
        environmentalPerformanceVisible = parentPage.isEditingFocus();
        return environmentalPerformanceVisible;
    }

    public boolean computeInternalPerformanceVisible(PageTask parentPage, PrismObjectWrapper<TaskType> taskWrapper) {
        internalPerformanceVisible = parentPage.isEditingFocus();
        return internalPerformanceVisible;
    }

    public boolean computeOperationVisible(PageTask parentPage, PrismObjectWrapper<TaskType> taskWrapper) {
        PrismContainerWrapper lensContext = null;
        try {
            lensContext = taskWrapper.findContainer(TaskType.F_MODEL_OPERATION_CONTEXT);
        } catch (SchemaException ex) {
            LOGGER.warn("Unable to find modelOperationContext in task {}", taskWrapper.getObject().asObjectable());
        }
        operationVisible = parentPage.isEditingFocus()
                && isTaskItemReadable(taskWrapper, TaskType.F_MODEL_OPERATION_CONTEXT)
                && lensContext != null && !lensContext.isEmpty()
                && !WebComponentUtil.isWorkflowTask(taskWrapper.getObject().asObjectable());
        return operationVisible;
    }

    public boolean computeResultVisible(PageTask parentPage, PrismObjectWrapper<TaskType> taskWrapper) {
        resultVisible = parentPage.isEditingFocus()
                && isTaskItemReadable(taskWrapper, TaskType.F_RESULT)
                && !WebComponentUtil.isWorkflowTask(taskWrapper.getObject().asObjectable());
        return resultVisible;
    }

    public boolean computeErrorsVisible(PageTask parentPage, TaskType task) {
        errorsVisible = parentPage.isEditingFocus()
                && !WebComponentUtil.isWorkflowTask(task);
        return errorsVisible;
    }

    public void computeAll(PageTask parentPage, PrismObjectWrapper<TaskType> taskWrapper) {
        TaskType taskType = taskWrapper.getObject().asObjectable();
        computeBasicVisible(parentPage, taskType);
        computeSchedulingVisible(parentPage, taskType);
        computeWorkManagementVisible(taskType);
        computeCleanupPolicyVisible();
        computeSubtasksAndThreadsVisible(taskType);
        computeProgressVisible(parentPage);
        computeEnvironmentalPerformanceVisible(parentPage, taskWrapper);
        computeInternalPerformanceVisible(parentPage, taskWrapper);
        computeOperationVisible(parentPage, taskWrapper);
        computeResultVisible(parentPage, taskWrapper);
        computeErrorsVisible(parentPage, taskType);
    }

    public boolean computeProgressVisible(PageTask parentPage) {
        progressVisible = parentPage.isEditingFocus();
        return progressVisible;
    }

    private boolean isShadowIntegrityCheck(String handlerUri) {
        return ModelPublicConstants.SHADOW_INTEGRITY_CHECK_TASK_HANDLER_URI.equals(handlerUri);
    }

    private boolean isFocusValidityScanner(String handlerUri) {
        return ModelPublicConstants.FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI.equals(handlerUri)
                || ModelPublicConstants.DEPRECATED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI.equals(handlerUri);
    }

    private boolean isTriggerScanner(String handlerUri) {
        return ModelPublicConstants.TRIGGER_SCANNER_TASK_HANDLER_URI.equals(handlerUri);
    }

    private boolean isExecuteChanges(String handlerUri) {
        return ModelPublicConstants.EXECUTE_CHANGES_TASK_HANDLER_URI.equals(handlerUri);
    }

    private boolean isTaskItemReadable(PrismObjectWrapper<TaskType> taskWrapper, ItemPath itemPath) {
        ItemWrapper taskProperty = null;
        try {
            taskProperty = taskWrapper.findProperty(itemPath);
        } catch (SchemaException ex) {
            LOGGER.warn("Unable to find {} property in task object {}, {}", itemPath,
                    taskWrapper.getObject().asObjectable(), ex.getLocalizedMessage());
        }
        return taskProperty != null && taskProperty.canRead();
    }

    public boolean isBasicVisible() {
        return basicVisible;
    }

    public boolean isSchedulingVisible() {
        return schedulingVisible;
    }

    public boolean isWorkManagementVisible(TaskType task) {
        workManagementVisible = computeWorkManagementVisible(task);
        return workManagementVisible;
    }

    public boolean isCleanupPolicyVisible() {
        return cleanupPolicyVisible;
    }

    public boolean isSubtasksAndThreadsVisible(TaskType task) {
        subtasksAndThreadsVisible = computeSubtasksAndThreadsVisible(task);
        return subtasksAndThreadsVisible;
    }

    public boolean isProgressVisible() {
        return progressVisible;
    }

    public boolean isEnvironmentalPerformanceVisible() {
        return environmentalPerformanceVisible;
    }

    public boolean isInternalPerformanceVisible() {
        return internalPerformanceVisible;
    }

    public boolean isOperationVisible() {
        return operationVisible;
    }

    public boolean isResultVisible() {
        return resultVisible;
    }

    public boolean isErrorsVisible() {
        return errorsVisible;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        TaskTabsVisibility that = (TaskTabsVisibility) o;

        if (basicVisible != that.basicVisible) { return false; }
        if (schedulingVisible != that.schedulingVisible) { return false; }
        if (subtasksAndThreadsVisible != that.subtasksAndThreadsVisible) { return false; }
        if (progressVisible != that.progressVisible) { return false; }
        if (workManagementVisible != that.workManagementVisible) { return false; }
        if (environmentalPerformanceVisible != that.environmentalPerformanceVisible) { return false; }
        if (operationVisible != that.operationVisible) { return false; }
        if (errorsVisible != that.errorsVisible) { return false; }
        return resultVisible == that.resultVisible;

    }

    @Override
    public int hashCode() {
        int result = (basicVisible ? 1 : 0);
        result = 31 * result + (schedulingVisible ? 1 : 0);
        result = 31 * result + (subtasksAndThreadsVisible ? 1 : 0);
        result = 31 * result + (progressVisible ? 1 : 0);
        result = 31 * result + (workManagementVisible ? 1 : 0);
        result = 31 * result + (environmentalPerformanceVisible ? 1 : 0);
        result = 31 * result + (operationVisible ? 1 : 0);
        result = 31 * result + (resultVisible ? 1 : 0);
        result = 31 * result + (errorsVisible ? 1 : 0);
        return result;
    }
}
