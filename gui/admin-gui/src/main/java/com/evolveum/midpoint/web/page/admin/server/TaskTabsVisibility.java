package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * Used to determine whether tabs have to be refreshed - by comparing instances of this class before and after task update.
 *
 * @author mederly
 */
class TaskTabsVisibility implements Serializable {
    private boolean basicVisible;
    private boolean schedulingVisible;
    private boolean subtasksAndThreadsVisible;
    private boolean progressVisible;
    private boolean environmentalPerformanceVisible;
    private boolean approvalsVisible;
    private boolean operationVisible;
    private boolean resultVisible;
    private boolean errorsVisible;

	public boolean computeBasicVisible(PageTaskEdit parentPage) {
		basicVisible = parentPage.isShowAdvanced() || !parentPage.getTaskDto().isWorkflow();
		return basicVisible;
	}

	public boolean computeSchedulingVisible(PageTaskEdit parentPage) {
		schedulingVisible = (parentPage.isShowAdvanced() || !parentPage.getTaskDto().isWorkflow())
			&& parentPage.isReadableSomeOf(
				TaskType.F_LAST_RUN_START_TIMESTAMP, TaskType.F_LAST_RUN_FINISH_TIMESTAMP,
				TaskType.F_NEXT_RUN_START_TIMESTAMP, TaskType.F_NEXT_RETRY_TIMESTAMP,
				TaskType.F_RECURRENCE, TaskType.F_BINDING, TaskType.F_SCHEDULE, TaskType.F_THREAD_STOP_ACTION);

		return schedulingVisible;
	}

	public boolean computeSubtasksAndThreadsVisible(PageTaskEdit parentPage) {
        if (parentPage.isEdit()) {
            subtasksAndThreadsVisible = parentPage.getTaskDto().configuresWorkerThreads()
					&& parentPage.isExtensionReadable(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS);
        } else if (parentPage.isShowAdvanced() || !parentPage.getTaskDto().isWorkflow()) {
            IModel<TaskDto> taskDtoModel = parentPage.getTaskDtoModel();
            subtasksAndThreadsVisible =
					(parentPage.getTaskDto().configuresWorkerThreads() && parentPage.isExtensionReadable(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS))
							|| !taskDtoModel.getObject().getSubtasks().isEmpty() || !taskDtoModel.getObject().getTransientSubtasks().isEmpty();
        } else {
			subtasksAndThreadsVisible = false;
		}
        return subtasksAndThreadsVisible;
    }

	public boolean computeProgressVisible(PageTaskEdit parentPage) {
        final OperationStatsType operationStats = parentPage.getTaskDto().getTaskType().getOperationStats();
        progressVisible = !parentPage.isEdit() && operationStats != null
				// readability is maybe not required, as the corresponding data would be null if not readable
				&& parentPage.isReadable(new ItemPath(TaskType.F_OPERATION_STATS))
                && (operationStats.getIterativeTaskInformation() != null ||
                        operationStats.getSynchronizationInformation() != null ||
                        operationStats.getActionsExecutedInformation() != null);
        return progressVisible;
    }

    public boolean computeEnvironmentalPerformanceVisible(PageTaskEdit parentPage) {
        final OperationStatsType operationStats = parentPage.getTaskDto().getTaskType().getOperationStats();
        environmentalPerformanceVisible = !parentPage.isEdit()
				&& parentPage.isReadable(new ItemPath(TaskType.F_OPERATION_STATS))
				&& operationStats != null
				&& !StatisticsUtil.isEmpty(operationStats.getEnvironmentalPerformanceInformation());
        return environmentalPerformanceVisible;
    }

    public boolean computeApprovalsVisible(PageTaskEdit parentPage) {
        approvalsVisible = !parentPage.isEdit()
				&& parentPage.isReadable(new ItemPath(TaskType.F_WORKFLOW_CONTEXT))
				&& (parentPage.getTaskDto().isWorkflowChild() || parentPage.getTaskDto().isWorkflowParent());
                //&& parentPage.getTaskDto().getTaskType().getWorkflowContext() != null
                //&& parentPage.getTaskDto().getWorkflowDeltaIn() != null;
        return approvalsVisible;
    }

	public boolean computeOperationVisible(PageTaskEdit parentPage) {
		operationVisible = !parentPage.isEdit()
				&& parentPage.isReadable(new ItemPath(TaskType.F_MODEL_OPERATION_CONTEXT))
				&& parentPage.getTaskDto().getTaskType().getModelOperationContext() != null
				&& (!parentPage.getTaskDto().isWorkflow() || parentPage.isShowAdvanced());
		return operationVisible;
	}

	public boolean computeResultVisible(PageTaskEdit parentPage) {
        resultVisible = !parentPage.isEdit()
				&& parentPage.isReadable(new ItemPath(TaskType.F_RESULT))
				&& (parentPage.isShowAdvanced() || !parentPage.getTaskDto().isWorkflow());
        return resultVisible;
    }

	public boolean computeErrorsVisible(PageTaskEdit parentPage) {
		//TODO what are the correct visibility conditions?
		errorsVisible = !parentPage.isEdit()
				&& (parentPage.isShowAdvanced() || !parentPage.getTaskDto().isWorkflow());
        return errorsVisible;
    }

	public void computeAll(PageTaskEdit parentPage) {
		computeBasicVisible(parentPage);
		computeSchedulingVisible(parentPage);
		computeSubtasksAndThreadsVisible(parentPage);
		computeProgressVisible(parentPage);
		computeEnvironmentalPerformanceVisible(parentPage);
		computeApprovalsVisible(parentPage);
		computeOperationVisible(parentPage);
		computeResultVisible(parentPage);
		computeErrorsVisible(parentPage);
	}

	public boolean isBasicVisible() {
		return basicVisible;
	}

	public boolean isSchedulingVisible() {
		return schedulingVisible;
	}

	public boolean isSubtasksAndThreadsVisible() {
		return subtasksAndThreadsVisible;
	}

	public boolean isProgressVisible() {
		return progressVisible;
	}

	public boolean isEnvironmentalPerformanceVisible() {
		return environmentalPerformanceVisible;
	}

	public boolean isApprovalsVisible() {
		return approvalsVisible;
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
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		TaskTabsVisibility that = (TaskTabsVisibility) o;

		if (basicVisible != that.basicVisible)
			return false;
		if (schedulingVisible != that.schedulingVisible)
			return false;
		if (subtasksAndThreadsVisible != that.subtasksAndThreadsVisible)
			return false;
		if (progressVisible != that.progressVisible)
			return false;
		if (environmentalPerformanceVisible != that.environmentalPerformanceVisible)
			return false;
		if (approvalsVisible != that.approvalsVisible)
			return false;
		if (operationVisible != that.operationVisible)
			return false;
		if (errorsVisible != that.errorsVisible)
			return false;
		return resultVisible == that.resultVisible;

	}

	@Override
	public int hashCode() {
		int result = (basicVisible ? 1 : 0);
		result = 31 * result + (schedulingVisible ? 1 : 0);
		result = 31 * result + (subtasksAndThreadsVisible ? 1 : 0);
		result = 31 * result + (progressVisible ? 1 : 0);
		result = 31 * result + (environmentalPerformanceVisible ? 1 : 0);
		result = 31 * result + (approvalsVisible ? 1 : 0);
		result = 31 * result + (operationVisible ? 1 : 0);
		result = 31 * result + (resultVisible ? 1 : 0);
		result = 31 * result + (errorsVisible ? 1 : 0);
		return result;
	}
}
