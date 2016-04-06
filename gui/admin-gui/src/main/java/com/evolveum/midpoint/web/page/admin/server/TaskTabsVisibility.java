package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
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
    private boolean resultVisible;

	public boolean computeBasicVisible(PageTaskEdit parentPage) {
		basicVisible = parentPage.isShowAdvanced() || !parentPage.isWorkflow();
		return basicVisible;
	}

	public boolean computeSchedulingVisible(PageTaskEdit parentPage) {
		schedulingVisible = parentPage.isShowAdvanced() || !parentPage.isWorkflow();
		return schedulingVisible;
	}

	public boolean computeSubtasksAndThreadsVisible(PageTaskEdit parentPage) {
        if (parentPage.isEdit()) {
            subtasksAndThreadsVisible = parentPage.configuresWorkerThreads();
        } else if (parentPage.isShowAdvanced() || !parentPage.isWorkflow()) {
            IModel<TaskDto> taskDtoModel = parentPage.getTaskDtoModel();
            subtasksAndThreadsVisible = parentPage.configuresWorkerThreads() || !taskDtoModel.getObject().getSubtasks().isEmpty() || !taskDtoModel.getObject().getTransientSubtasks().isEmpty();
        } else {
			subtasksAndThreadsVisible = false;
		}
        return subtasksAndThreadsVisible;
    }

	public boolean computeProgressVisible(PageTaskEdit parentPage) {
        final OperationStatsType operationStats = parentPage.getTaskDto().getTaskType().getOperationStats();
        progressVisible = !parentPage.isEdit() && operationStats != null &&
                (operationStats.getIterativeTaskInformation() != null ||
                        operationStats.getSynchronizationInformation() != null ||
                        operationStats.getActionsExecutedInformation() != null);
        return progressVisible;
    }

    public boolean computeEnvironmentalPerformanceVisible(PageTaskEdit parentPage) {
        final OperationStatsType operationStats = parentPage.getTaskDto().getTaskType().getOperationStats();
        environmentalPerformanceVisible = !parentPage.isEdit()
				&& operationStats != null
				&& !StatisticsUtil.isEmpty(operationStats.getEnvironmentalPerformanceInformation());
        return environmentalPerformanceVisible;
    }

    public boolean computeApprovalsVisible(PageTaskEdit parentPage) {
        approvalsVisible = !parentPage.isEdit()
				&& (parentPage.isWorkflowChild() || parentPage.isWorkflowParent());
                //&& parentPage.getTaskDto().getTaskType().getWorkflowContext() != null
                //&& parentPage.getTaskDto().getWorkflowDeltaIn() != null;
        return approvalsVisible;
    }

    public boolean computeResultVisible(PageTaskEdit parentPage) {
        resultVisible = !parentPage.isEdit() && (parentPage.isShowAdvanced() || !parentPage.isWorkflow());
        return resultVisible;
    }

	public void computeAll(PageTaskEdit parentPage) {
		computeBasicVisible(parentPage);
		computeSchedulingVisible(parentPage);
		computeSubtasksAndThreadsVisible(parentPage);
		computeProgressVisible(parentPage);
		computeEnvironmentalPerformanceVisible(parentPage);
		computeApprovalsVisible(parentPage);
		computeResultVisible(parentPage);
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

	public boolean isResultVisible() {
		return resultVisible;
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
		result = 31 * result + (resultVisible ? 1 : 0);
		return result;
	}
}
