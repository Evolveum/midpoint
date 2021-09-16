/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;

import static java.util.Collections.singletonList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ActivityStatisticsUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.reports.PageCreatedReports;
import com.evolveum.midpoint.web.page.admin.server.LivesyncTokenEditorPanel;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class TaskOperationalButtonsPanel extends AssignmentHolderOperationalButtonsPanel<TaskType> {

    private static final Trace LOGGER = TraceManager.getTrace(TaskOperationalButtonsPanel.class);

    private static final String DOT_CLASS = TaskOperationalButtonsPanel.class.getName() + ".";
    protected static final String OPERATION_EXECUTE_TASK_CHANGES = DOT_CLASS + "executeTaskChanges";
    private static final String OPERATION_LOAD_REPORT_OUTPUT = DOT_CLASS + "loadReport";

    private static final String ID_TASK_BUTTONS = "taskButtons";
    private static final String ID_REFRESHING_BUTTONS = "refreshingButtons";

    private Boolean refreshEnabled;

    public TaskOperationalButtonsPanel(String id, LoadableModel<PrismObjectWrapper<TaskType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        RepeatingView taskButtons = new RepeatingView(ID_TASK_BUTTONS);
        add(taskButtons);

        createSuspendButton(taskButtons);
        createResumeButton(taskButtons);
        createRunNowButton(taskButtons);

        createManageLivesyncTokenButton(taskButtons);
        createDownloadReportButton(taskButtons);
        createCleanupPerformanceButton(taskButtons);
        createCleanupResultsButton(taskButtons);

        RepeatingView refreshingButtons = new RepeatingView(ID_REFRESHING_BUTTONS);
        initRefreshingButtons(refreshingButtons);
        add(refreshingButtons);

    }

    protected void initRefreshingButtons(RepeatingView stateButtonsView) {
        createRefreshNowIconButton(stateButtonsView);
        createResumePauseButton(stateButtonsView);
        final Label status = new Label(stateButtonsView.newChildId(), this::createRefreshingLabel);
        status.setOutputMarkupId(true);
        stateButtonsView.add(status);
    }

    private void createSuspendButton(RepeatingView repeatingView) {
        AjaxButton suspend = new AjaxButton(repeatingView.newChildId(), createStringResource("pageTaskEdit.button.suspend")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                suspendPerformed(target, getPrismObject());
            }
        };
        suspend.add(new VisibleBehaviour(() -> WebComponentUtil.canSuspendTask(getObjectType(), getPageBase())));
        suspend.add(AttributeAppender.append("class", "btn-danger"));
        repeatingView.add(suspend);
    }

    //TODO abstract
    protected void suspendPerformed(AjaxRequestTarget target, PrismObject<TaskType> taskPrism) {
        if (taskPrism == null) {
            return;
        }
        OperationResult result = TaskOperationUtils.suspendTasks(singletonList(taskPrism.asObjectable()), getPageBase());
        afterOperation(target, result);
    }

    protected void resumePerformed(AjaxRequestTarget target, PrismObject<TaskType> task) {
        if (task == null) {
            return;
        }
        OperationResult result = TaskOperationUtils.resumeTasks(singletonList(task.asObjectable()), getPageBase());
        afterOperation(target, result);
    }

    protected void runNowPerformed(AjaxRequestTarget target, PrismObject<TaskType> task) {
        String oid = task.getOid();
        refreshEnabled = Boolean.TRUE;
        OperationResult result = TaskOperationUtils.runNowPerformed(singletonList(oid), getPageBase());
        afterOperation(target, result);
    }


    private void createResumeButton(RepeatingView repeatingView) {
        AjaxButton resume = new AjaxButton(repeatingView.newChildId(), createStringResource("pageTaskEdit.button.resume")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                resumePerformed(target, getPrismObject());
            }
        };
        resume.add(AttributeAppender.append("class", "btn-primary"));
        resume.add(new VisibleBehaviour(() -> WebComponentUtil.canResumeTask(getObjectType(), getPageBase())));
        repeatingView.add(resume);
    }

    private void createRunNowButton(RepeatingView repeatingView) {
        AjaxButton runNow = new AjaxButton(repeatingView.newChildId(), createStringResource("pageTaskEdit.button.runNow")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                runNowPerformed(target, getPrismObject());
            }
        };
        runNow.add(AttributeAppender.append("class", "btn-success"));
        runNow.add(new VisibleBehaviour(() -> WebComponentUtil.canRunNowTask(getObjectType(), getPageBase())));
        repeatingView.add(runNow);
    }

    protected void afterOperation(AjaxRequestTarget target, OperationResult result) {
//        taskTabsVisibility = new TaskTabsVisibility();
//        showResult(result);
//        getModel().reset();
//        refresh(target);
    }

    private void createManageLivesyncTokenButton(RepeatingView repeatingView) {
        AjaxButton manageLivesyncToken = new AjaxButton(repeatingView.newChildId(), createStringResource("PageTask.livesync.token")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                LivesyncTokenEditorPanel tokenEditor = new LivesyncTokenEditorPanel(getPageBase().getMainPopupBodyId(), TaskOperationalButtonsPanel.this.getModel()) {

                    @Override
                    protected void saveTokenPerformed(ObjectDelta<TaskType> tokenDelta, AjaxRequestTarget target) {
                        saveTaskChanges(target, tokenDelta);
                    }
                };
                tokenEditor.setOutputMarkupId(true);
                getPageBase().showMainPopup(tokenEditor, ajaxRequestTarget);
            }
        };
        manageLivesyncToken.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return WebComponentUtil.isLiveSync(getObjectType()) && isNotRunning() && getObjectType().asPrismObject().findProperty(LivesyncTokenEditorPanel.PATH_TOKEN) != null;
            }

            @Override
            public boolean isEnabled() {
                return isNotRunning();
            }
        });
        manageLivesyncToken.add(AttributeAppender.append("class", "btn-default"));
        manageLivesyncToken.setOutputMarkupId(true);
        repeatingView.add(manageLivesyncToken);
    }

    private void createDownloadReportButton(RepeatingView repeatingView) {
        final AjaxDownloadBehaviorFromStream ajaxDownloadBehavior = new AjaxDownloadBehaviorFromStream() {
            private static final long serialVersionUID = 1L;

            @Override
            protected InputStream initStream() {
                ReportDataType reportObject = getReportData();
                if (reportObject != null) {
                    return PageCreatedReports.createReport(reportObject, this, getPageBase());
                } else {
                    return null;
                }
            }

            @Override
            public String getFileName() {
                ReportDataType reportObject = getReportData();
                return PageCreatedReports.getReportFileName(reportObject);
            }
        };
        add(ajaxDownloadBehavior);

        AjaxButton download = new AjaxButton(repeatingView.newChildId(), createStringResource("PageTask.download.report")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ajaxDownloadBehavior.initiate(target);
            }
        };
        download.add(new VisibleBehaviour(this::isDownloadReportVisible));
        download.add(AttributeAppender.append("class", "btn-primary"));
        repeatingView.add(download);
    }

    private boolean isDownloadReportVisible() {
        return WebComponentUtil.isReport(getObjectType())
                && getReportDataOid() != null;
    }

    private ReportDataType getReportData() {
        String reportData = getReportDataOid();
        if (reportData == null) {
            return null;
        }

        Task opTask = getPageBase().createSimpleTask(OPERATION_LOAD_REPORT_OUTPUT);
        OperationResult result = opTask.getResult();

        PrismObject<ReportDataType> report = WebModelServiceUtils.loadObject(ReportDataType.class, reportData, getPageBase(), opTask, result);
        if (report == null) {
            return null;
        }
        result.computeStatusIfUnknown();
        getPageBase().showResult(result, false);

        return report.asObjectable();

    }

    private String getReportDataOid() {
        PrismObject<TaskType> task = getObjectType().asPrismObject();
        PrismReference reportData = task.findReference(ItemPath.create(TaskType.F_EXTENSION, ReportConstants.REPORT_DATA_PROPERTY_NAME));
        if (reportData == null || reportData.getRealValue() == null || reportData.getRealValue().getOid() == null) {
            PrismProperty<String> reportOutputOid = task.findProperty(ItemPath.create(TaskType.F_EXTENSION, ReportConstants.REPORT_OUTPUT_OID_PROPERTY_NAME));
            if (reportOutputOid == null) {
                return null;
            }
            return reportOutputOid.getRealValue();
        }

        return reportData.getRealValue().getOid();
    }

    private void createRefreshNowIconButton(RepeatingView repeatingView) {
        AjaxIconButton refreshNow = new AjaxIconButton(repeatingView.newChildId(), new Model<>("fa fa-refresh"), createStringResource("autoRefreshPanel.refreshNow")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                refresh(target);
            }
        };
        refreshNow.add(AttributeAppender.append("class", ""));
        repeatingView.add(refreshNow);
    }

    private void createResumePauseButton(RepeatingView repeatingView) {
        AjaxIconButton resumePauseRefreshing = new AjaxIconButton(repeatingView.newChildId(), (IModel<String>) this::createResumePauseButtonLabel, createStringResource("autoRefreshPanel.resumeRefreshing")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                refreshEnabled = !isRefreshEnabled();
                refresh(target);
            }
        };
        resumePauseRefreshing.add(AttributeAppender.append("class", "btn-margin-left"));
        repeatingView.add(resumePauseRefreshing);
    }

    //TODO abstract
    protected boolean isRefreshEnabled() {
        return false;
    }

    protected void refresh(AjaxRequestTarget target) {

    }

    protected int getRefreshInterval() {
        return 0;
    }

    private String createRefreshingLabel() {
        if (isRefreshEnabled()) {
            return createStringResource("autoRefreshPanel.refreshingEach", getRefreshInterval() / 1000).getString();
        } else {
            return createStringResource("autoRefreshPanel.noRefreshing").getString();
        }
    }

    private String createResumePauseButtonLabel() {
        if (isRefreshEnabled()) {
            return "fa fa-pause";
        }
        return "fa fa-play";
    }

    private void createCleanupPerformanceButton(RepeatingView repeatingView) {
        AjaxCompositedIconButton cleanupPerformance = new AjaxCompositedIconButton(repeatingView.newChildId(), getTaskCleanupCompositedIcon(GuiStyleConstants.CLASS_ICON_PERFORMANCE),
                createStringResource("operationalButtonsPanel.cleanupEnvironmentalPerformance")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ConfirmationPanel dialog = new ConfirmationPanel(getPageBase().getMainPopupBodyId(), createStringResource("operationalButtonsPanel.cleanupEnvironmentalPerformance.confirmation")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        try {
                            deleteStatistics(target);
                        } catch (Exception e) {
                            LOGGER.error("Cannot delete task operation statistics, {}", e.getMessage(), e);
                            getSession().error(getPageBase().getString("PageTask.cleanup.operationStatistics.failed"));
                        }
                    }
                };
                getPageBase().showMainPopup(dialog, target);
            }
        };
        cleanupPerformance.add(AttributeAppender.append("class", "btn-default btn-margin-left"));
        cleanupPerformance.add(new VisibleBehaviour(this::isNotRunning));
        repeatingView.add(cleanupPerformance);
    }

    private void deleteStatistics(AjaxRequestTarget target) throws SchemaException {
        List<ItemPath> statisticsPaths = new ArrayList<>();
        statisticsPaths.add(TaskType.F_OPERATION_STATS);
        statisticsPaths.addAll(ActivityStatisticsUtil.getAllStatisticsPaths(getObjectType()));
        deleteItem(target, statisticsPaths.toArray(new ItemPath[0]));
    }

    private void deleteItem(AjaxRequestTarget target, ItemPath... paths) throws SchemaException {
        Collection<ItemDelta<?, ?>> itemDeltas = new ArrayList<>();
        for (ItemPath path : paths) {
            ItemDelta<?, ?> delta = createDeleteItemDelta(path);
            if (delta == null) {
                LOGGER.trace("Nothing to delete for {}", path);
                continue;
            }
            itemDeltas.add(delta);
        }

        ObjectDelta<TaskType> taskDelta = getPrismContext().deltaFor(TaskType.class)
                .asObjectDelta(getObjectType().getOid());
        taskDelta.addModifications(itemDeltas);

        saveTaskChanges(target, taskDelta);
    }

    private void createCleanupResultsButton(RepeatingView repeatingView) {
        AjaxCompositedIconButton cleanupResults = new AjaxCompositedIconButton(repeatingView.newChildId(), getTaskCleanupCompositedIcon(GuiStyleConstants.CLASS_ICON_TASK_RESULTS),
                createStringResource("operationalButtonsPanel.cleanupResults")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ConfirmationPanel dialog = new ConfirmationPanel(getPageBase().getMainPopupBodyId(), createStringResource("operationalButtonsPanel.cleanupResults.confirmation")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        try {
                            deleteItem(target, TaskType.F_RESULT, TaskType.F_RESULT_STATUS);
                        } catch (Exception e) {
                            LOGGER.error("Cannot clear task results: {}", e.getMessage());
                            getSession().error(getPageBase().getString("PageTask.cleanup.result.failed"));
                        }
                    }
                };
                getPageBase().showMainPopup(dialog, target);
            }
        };
        cleanupResults.add(new VisibleBehaviour(this::isNotRunning));
        cleanupResults.add(AttributeAppender.append("class", "btn-default btn-margin-left"));
        repeatingView.add(cleanupResults);
    }

    private ItemDelta<?, ?> createDeleteItemDelta(ItemPath itemPath) throws SchemaException {
        // Originally here was a code that looked at item wrapper - why?
        // Removed because it didn't allow to remove values not covered by wrapper, like activityState/activity/statistics.
        return getPrismContext().deltaFor(TaskType.class)
                .item(itemPath).replace()
                .asItemDelta();
    }

    private void saveTaskChanges(AjaxRequestTarget target, ObjectDelta<TaskType> taskDelta) {
        if (taskDelta.isEmpty()) {
            getSession().warn("Nothing to save, no changes were made.");
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_EXECUTE_TASK_CHANGES);
        Task task = getPageBase().createSimpleTask(OPERATION_EXECUTE_TASK_CHANGES);

        try {
            taskDelta.revive(getPrismContext()); //do we need revive here?
            getPageBase().getModelService().executeChanges(MiscUtil.createCollection(taskDelta), null, task, result);
            result.computeStatus();
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot save tasks changes", e);
            result.recordFatalError("Cannot save tasks changes, " + e.getMessage(), e);
        }
        afterOperation(target, result);
    }

    private CompositedIcon getTaskCleanupCompositedIcon(String basicIconClass) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder();
        return iconBuilder
                .setBasicIcon(basicIconClass, IconCssStyle.IN_ROW_STYLE)
                .appendLayerIcon(WebComponentUtil.createIconType(GuiStyleConstants.CLASS_ICON_TRASH), IconCssStyle.BOTTOM_RIGHT_STYLE)
                .build();
    }

    private boolean isNotRunning() {
        return !WebComponentUtil.isRunningTask(getObjectType());
    }
}
