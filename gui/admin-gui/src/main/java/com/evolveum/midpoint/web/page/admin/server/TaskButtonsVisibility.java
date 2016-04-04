package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
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

    public boolean computeBackVisible(PageTaskEdit parentPage) {
        backVisible = !parentPage.isEdit();
        return backVisible;
    }

    public boolean computeEditVisible(PageTaskEdit parentPage) {
        editVisible = !parentPage.isEdit();
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
        suspendVisible = !parentPage.isEdit() && parentPage.isRunnableOrRunning();
        return suspendVisible;
    }

    public boolean computeResumeVisible(PageTaskEdit parentPage) {
        resumeVisible = !parentPage.isEdit() && (parentPage.isSuspended() || (parentPage.isClosed() && parentPage.isRecurring()));
        return resumeVisible;
    }

    public boolean computeRunNowVisible(PageTaskEdit parentPage) {
        runNowVisible = !parentPage.isEdit() && (parentPage.isRunnable() || (parentPage.isClosed() && !parentPage.isRecurring()));
        return runNowVisible;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskButtonsVisibility that = (TaskButtonsVisibility) o;

        if (backVisible != that.backVisible) return false;
        if (editVisible != that.editVisible) return false;
        if (cancelEditVisible != that.cancelEditVisible) return false;
        if (saveVisible != that.saveVisible) return false;
        if (suspendVisible != that.suspendVisible) return false;
        if (resumeVisible != that.resumeVisible) return false;
        return runNowVisible == that.runNowVisible;

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
        return result;
    }
}
