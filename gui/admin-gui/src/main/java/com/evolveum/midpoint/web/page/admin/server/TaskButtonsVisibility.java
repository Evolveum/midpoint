package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;

import java.io.Serializable;

/**
 * Used to determine whether tabs have to be refreshed - by comparing instances of this class before and after task update.
 *
 * @author mederly
 */
class TaskButtonsVisibility implements Serializable {
    private boolean backVisible;
    private boolean editVisible;
    private boolean cancelEditVisible;
    private boolean saveVisible;
    private boolean suspendVisible;
    private boolean resumeVisible;
    private boolean runNowVisible;
    private boolean stopVisible;

    public boolean computeBackVisible(PageTaskEdit parentPage) {
        backVisible = !parentPage.isEdit();
        return backVisible;
    }

    public boolean computeEditVisible(PageTaskEdit parentPage) {
        editVisible =
				!parentPage.isEdit()
						&& parentPage.isEditable()
						&& (!parentPage.getTaskDto().isWorkflow() || parentPage.isShowAdvanced());
        return editVisible;
    }

    public boolean computeCancelEditVisible(PageTaskEdit parentPage) {
        cancelEditVisible = parentPage.isEdit();
        return cancelEditVisible;
    }

    public boolean computeSaveVisible(PageTaskEdit parentPage) {
        saveVisible = parentPage.isEdit();
        return saveVisible;
    }

    public boolean computeSuspendVisible(PageTaskEdit parentPage) {
		final TaskDto taskDto = parentPage.getTaskDto();
		suspendVisible = !parentPage.isEdit() && taskDto.isRunnableOrRunning() && (!taskDto.isWorkflow() || parentPage.isShowAdvanced())
			&& parentPage.canSuspend();
        return suspendVisible;
    }

    public boolean computeResumeVisible(PageTaskEdit parentPage) {
		final TaskDto taskDto = parentPage.getTaskDto();
		resumeVisible = !parentPage.isEdit()
				&& (taskDto.isSuspended() || (taskDto.isClosed() && taskDto.isRecurring()))
				&& (!taskDto.isWorkflow() || parentPage.isShowAdvanced())
				&& parentPage.canResume();
        return resumeVisible;
    }

    public boolean computeRunNowVisible(PageTaskEdit parentPage) {
		final TaskDto taskDto = parentPage.getTaskDto();
		runNowVisible = !parentPage.isEdit()
				&& (taskDto.isRunnable() || (taskDto.isClosed() && !taskDto.isRecurring()))
				&& (!taskDto.isWorkflow() || parentPage.isShowAdvanced())
				&& parentPage.canRunNow();
        return runNowVisible;
    }

	public boolean computeStopVisible(PageTaskEdit parentPage) {
		final TaskDto taskDto = parentPage.getTaskDto();
		stopVisible = !parentPage.isEdit()
				&& taskDto.isWorkflowChild()
				&& !taskDto.isClosed()
				&& parentPage.canStop();
		return stopVisible;
	}

	public void computeAll(PageTaskEdit parentPage) {
		computeBackVisible(parentPage);
		computeEditVisible(parentPage);
		computeCancelEditVisible(parentPage);
		computeSaveVisible(parentPage);
		computeSuspendVisible(parentPage);
		computeResumeVisible(parentPage);
		computeRunNowVisible(parentPage);
		computeStopVisible(parentPage);
	}

	public boolean isBackVisible() {
        return backVisible;
    }

    public boolean isEditVisible() {
        return editVisible;
    }

    public boolean isCancelEditVisible() {
        return cancelEditVisible;
    }

    public boolean isSaveVisible() {
        return saveVisible;
    }

    public boolean isSuspendVisible() {
        return suspendVisible;
    }

    public boolean isResumeVisible() {
        return resumeVisible;
    }

    public boolean isRunNowVisible() {
        return runNowVisible;
    }

	public boolean isStopVisible() {
		return stopVisible;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		TaskButtonsVisibility that = (TaskButtonsVisibility) o;

		if (backVisible != that.backVisible)
			return false;
		if (editVisible != that.editVisible)
			return false;
		if (cancelEditVisible != that.cancelEditVisible)
			return false;
		if (saveVisible != that.saveVisible)
			return false;
		if (suspendVisible != that.suspendVisible)
			return false;
		if (resumeVisible != that.resumeVisible)
			return false;
		if (runNowVisible != that.runNowVisible)
			return false;
		return stopVisible == that.stopVisible;

	}

	@Override
	public int hashCode() {
		int result = (backVisible ? 1 : 0);
		result = 31 * result + (editVisible ? 1 : 0);
		result = 31 * result + (cancelEditVisible ? 1 : 0);
		result = 31 * result + (saveVisible ? 1 : 0);
		result = 31 * result + (suspendVisible ? 1 : 0);
		result = 31 * result + (resumeVisible ? 1 : 0);
		result = 31 * result + (runNowVisible ? 1 : 0);
		result = 31 * result + (stopVisible ? 1 : 0);
		return result;
	}
}
