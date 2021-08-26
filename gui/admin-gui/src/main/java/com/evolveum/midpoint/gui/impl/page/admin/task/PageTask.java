/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.task;

import static java.util.Collections.singletonList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ActivityStatisticsUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.reports.PageCreatedReports;
import com.evolveum.midpoint.web.page.admin.server.LivesyncTokenEditorPanel;
import com.evolveum.midpoint.web.page.admin.server.TaskSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/taskNew")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL,
                        label = "PageAdminUsers.auth.usersAll.label",
                        description = "PageAdminUsers.auth.usersAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TASK_URL,
                        label = "PageUser.auth.user.label",
                        description = "PageUser.auth.user.description")
        })
public class PageTask extends PageAssignmentHolderDetails<TaskType, AssignmentHolderDetailsModel<TaskType>> {

    private static final Trace LOGGER = TraceManager.getTrace(PageTask.class);

    private static final String DOT_CLASS = PageTask.class.getName() + ".";
    protected static final String OPERATION_EXECUTE_TASK_CHANGES = DOT_CLASS + "executeTaskChanges";
    private static final String OPERATION_LOAD_REPORT_OUTPUT = DOT_CLASS + "loadReport";

    private Boolean refreshEnabled;

    public PageTask(PageParameters pageParameters) {
        super(pageParameters);
    }

    @Override
    protected Class<TaskType> getType() {
        return TaskType.class;
    }

    @Override
    protected Panel getSummaryPanel(String id, LoadableModel<TaskType> summaryModel) {
        return new TaskSummaryPanel(id, summaryModel, this);
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> getOperationOptions() {
        return getOperationOptionsBuilder()
                // retrieve
                .item(TaskType.F_SUBTASK_REF).retrieve()
                .item(TaskType.F_NODE_AS_OBSERVED).retrieve()
                .item(TaskType.F_NEXT_RUN_START_TIMESTAMP).retrieve()
                .item(TaskType.F_NEXT_RETRY_TIMESTAMP).retrieve()
                .item(TaskType.F_RESULT).retrieve()         // todo maybe only when it is to be displayed
                .build();
    }

    protected void initOperationalButtons(RepeatingView repeatingView) {
        createSuspendButton(repeatingView);
        createResumeButton(repeatingView);
        createRunNowButton(repeatingView);

        createManageLivesyncTokenButton(repeatingView);
        createDownloadReportButton(repeatingView);
        createCleanupPerformanceButton(repeatingView);
        createCleanupResultsButton(repeatingView);

//        AjaxIconButton cleanupErrors = new AjaxIconButton(repeatingView.newChildId(), new Model<>(GuiStyleConstants.CLASS_ICON_TRASH),
//        createStringResource("operationalButtonsPanel.cleanupErrors")) {
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                refresh(target);
//            }
//        };
//        cleanupErrors.add(AttributeAppender.append("class", "btn btn-default btn-margin-left btn-sm"));
//        cleanupErrors.add(new VisibleBehaviour(this::isNotRunning));
//        repeatingView.add(cleanupErrors);

        setOutputMarkupId(true);

    }

    //TODO later migrate higher.. this might be later used for all focuses
    @Override
    protected void initStateButtons(RepeatingView stateButtonsView) {
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
                PrismObject<TaskType> task = getModelPrismObject();
                if (task == null) {
                    return;
                }
                OperationResult result = TaskOperationUtils.suspendTasks(singletonList(task.asObjectable()), PageTask.this);
                afterOperation(target, result);
            }
        };
        suspend.add(new VisibleBehaviour(() -> WebComponentUtil.canSuspendTask(getModelObjectType(), PageTask.this)));
        suspend.add(AttributeAppender.append("class", "btn-danger btn-sm"));
        repeatingView.add(suspend);
    }

    private void createResumeButton(RepeatingView repeatingView) {
        AjaxButton resume = new AjaxButton(repeatingView.newChildId(), createStringResource("pageTaskEdit.button.resume")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                PrismObject<TaskType> task = getModelPrismObject();
                if (task == null) {
                    return;
                }
                OperationResult result = TaskOperationUtils.resumeTasks(singletonList(task.asObjectable()), PageTask.this);
                afterOperation(target, result);
            }
        };
        resume.add(AttributeAppender.append("class", "btn-primary btn-sm"));
        resume.add(new VisibleBehaviour(() -> WebComponentUtil.canResumeTask(getModelObjectType(), PageTask.this)));
        repeatingView.add(resume);
    }

    private void createRunNowButton(RepeatingView repeatingView) {
        AjaxButton runNow = new AjaxButton(repeatingView.newChildId(), createStringResource("pageTaskEdit.button.runNow")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String oid = getModelPrismObject().getOid();
                refreshEnabled = Boolean.TRUE;
                OperationResult result = TaskOperationUtils.runNowPerformed(singletonList(oid), PageTask.this);
                afterOperation(target, result);
            }
        };
        runNow.add(AttributeAppender.append("class", "btn-success btn-sm"));
        runNow.add(new VisibleBehaviour(() -> WebComponentUtil.canRunNowTask(getModelObjectType(), PageTask.this)));
        repeatingView.add(runNow);
    }

    private void afterOperation(AjaxRequestTarget target, OperationResult result) {
//        taskTabsVisibility = new TaskTabsVisibility();
        showResult(result);
        getModel().reset();
        refresh(target);
    }

    private void createManageLivesyncTokenButton(RepeatingView repeatingView) {
        AjaxButton manageLivesyncToken = new AjaxButton(repeatingView.newChildId(), createStringResource("PageTask.livesync.token")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                LivesyncTokenEditorPanel tokenEditor = new LivesyncTokenEditorPanel(PageTask.this.getMainPopupBodyId(), PageTask.this.getModel()) {

                    @Override
                    protected void saveTokenPerformed(ObjectDelta<TaskType> tokenDelta, AjaxRequestTarget target) {
                        saveTaskChanges(target, tokenDelta);
                    }
                };
                tokenEditor.setOutputMarkupId(true);
                PageTask.this.showMainPopup(tokenEditor, ajaxRequestTarget);
            }
        };
        manageLivesyncToken.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return WebComponentUtil.isLiveSync(getModelObjectType()) && isNotRunning() && getModelObjectType().asPrismObject().findProperty(LivesyncTokenEditorPanel.PATH_TOKEN) != null;
            }

            @Override
            public boolean isEnabled() {
                return isNotRunning();
            }
        });
        manageLivesyncToken.add(AttributeAppender.append("class", "btn-default btn-sm"));
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
                    return PageCreatedReports.createReport(reportObject, this, PageTask.this);
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
        download.add(AttributeAppender.append("class", "btn-primary btn-sm"));
        repeatingView.add(download);
    }

    private boolean isDownloadReportVisible() {
        return WebComponentUtil.isReport(getModelObjectType())
                && getReportDataOid() != null;
    }

    private ReportDataType getReportData() {
        String reportData = getReportDataOid();
        if (reportData == null) {
            return null;
        }

        Task opTask = createSimpleTask(OPERATION_LOAD_REPORT_OUTPUT);
        OperationResult result = opTask.getResult();

        PrismObject<ReportDataType> report = WebModelServiceUtils.loadObject(ReportDataType.class, reportData, this, opTask, result);
        if (report == null) {
            return null;
        }
        result.computeStatusIfUnknown();
        showResult(result, false);

        return report.asObjectable();

    }

    private String getReportDataOid() {
        PrismObject<TaskType> task = getModelObjectType().asPrismObject();
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
        refreshNow.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
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
        resumePauseRefreshing.add(AttributeAppender.append("class", "btn btn-default btn-margin-left btn-sm"));
        repeatingView.add(resumePauseRefreshing);
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
                ConfirmationPanel dialog = new ConfirmationPanel(getMainPopupBodyId(), createStringResource("operationalButtonsPanel.cleanupEnvironmentalPerformance.confirmation")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public StringResourceModel getTitle() {
                        return createStringResource("pageUsers.message.confirmActionPopupTitle");
                    }

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        try {
                            deleteStatistics(target);
                        } catch (Exception e) {
                            LOGGER.error("Cannot delete task operation statistics, {}", e.getMessage(), e);
                            getSession().error(PageTask.this.getString("PageTask.cleanup.operationStatistics.failed"));
                        }
                    }
                };
                showMainPopup(dialog, target);
            }
        };
        cleanupPerformance.add(AttributeAppender.append("class", "btn btn-default btn-margin-left btn-sm"));
        cleanupPerformance.add(new VisibleBehaviour(this::isNotRunning));
        repeatingView.add(cleanupPerformance);
    }

    private void deleteStatistics(AjaxRequestTarget target) throws SchemaException {
        List<ItemPath> statisticsPaths = new ArrayList<>();
        statisticsPaths.add(TaskType.F_OPERATION_STATS);
        statisticsPaths.addAll(ActivityStatisticsUtil.getAllStatisticsPaths(getModelObjectType()));
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
                .asObjectDelta(getModelObjectType().getOid());
        taskDelta.addModifications(itemDeltas);

        saveTaskChanges(target, taskDelta);
    }

    private void createCleanupResultsButton(RepeatingView repeatingView) {
        AjaxCompositedIconButton cleanupResults = new AjaxCompositedIconButton(repeatingView.newChildId(), getTaskCleanupCompositedIcon(GuiStyleConstants.CLASS_ICON_TASK_RESULTS),
                createStringResource("operationalButtonsPanel.cleanupResults")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ConfirmationPanel dialog = new ConfirmationPanel(getMainPopupBodyId(), createStringResource("operationalButtonsPanel.cleanupResults.confirmation")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public StringResourceModel getTitle() {
                        return createStringResource("pageUsers.message.confirmActionPopupTitle");
                    }

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        try {
                            deleteItem(target, TaskType.F_RESULT, TaskType.F_RESULT_STATUS);
                        } catch (Exception e) {
                            LOGGER.error("Cannot clear task results: {}", e.getMessage());
                            getSession().error(PageTask.this.getString("PageTask.cleanup.result.failed"));
                        }
                    }
                };
                showMainPopup(dialog, target);
            }
        };
        cleanupResults.add(new VisibleBehaviour(this::isNotRunning));
        cleanupResults.add(AttributeAppender.append("class", "btn btn-default btn-margin-left btn-sm"));
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
            target.add(getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_EXECUTE_TASK_CHANGES);
        Task task = createSimpleTask(OPERATION_EXECUTE_TASK_CHANGES);

        try {
            taskDelta.revive(getPrismContext()); //do we need revive here?
            getModelService().executeChanges(MiscUtil.createCollection(taskDelta), null, task, result);
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
        return !WebComponentUtil.isRunningTask(getModelObjectType());
    }
}
