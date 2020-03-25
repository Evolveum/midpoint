package com.evolveum.midpoint.web.page.admin.server;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceWrapper;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.util.ModelContextUtil;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
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
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.refresh.Refreshable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.reports.PageCreatedReports;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/task", matchUrlForSecurity = "/admin/task")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                        label = "PageAdminUsers.auth.usersAll.label",
                        description = "PageAdminUsers.auth.usersAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USER_URL,
                        label = "PageUser.auth.user.label",
                        description = "PageUser.auth.user.description")
        })
public class PageTask extends PageAdminObjectDetails<TaskType> implements Refreshable {
    private static final long serialVersionUID = 1L;

    private static final transient Trace LOGGER = TraceManager.getTrace(PageTask.class);
    private static final String DOT_CLASS = PageTask.class.getName() + ".";
    protected static final String OPERATION_EXECUTE_TASK_CHANGES = DOT_CLASS + "executeTaskChanges";
    private static final String OPERATION_LOAD_REPORT_OUTPUT = DOT_CLASS + "loadReport";

    private static final int REFRESH_INTERVAL = 2000;

    private Boolean refreshEnabled;

    private TaskTabsVisibility taskTabsVisibility;

    public PageTask() {
        initialize(null);
    }

    public PageTask(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize(null);
    }

    public PageTask(final PrismObject<TaskType> taskToEdit) {
        initialize(taskToEdit);
    }

    public PageTask(final PrismObject<TaskType> taskToEdit, boolean isNewObject) {
        initialize(taskToEdit, isNewObject);
    }

    @Override
    public Class<TaskType> getCompileTimeClass() {
        return TaskType.class;
    }

    @Override
    protected TaskType createNewObject() {
        return new TaskType();
    }

    @Override
    protected ObjectSummaryPanel<TaskType> createSummaryPanel(IModel<TaskType> summaryModel) {
        return new TaskSummaryPanel(ID_SUMMARY_PANEL, summaryModel, this, this);
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> buildGetOptions() {
        //TODO use full options as defined in TaskDtoProviderOptions.fullOptions()

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

        createRefreshNowIconButton(repeatingView);
        createResumePauseButton(repeatingView);



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

        final Label status = new Label(repeatingView.newChildId(), this::createRefreshingLabel);
        status.setOutputMarkupId(true);
        repeatingView.add(status);

    }

    private void createSuspendButton(RepeatingView repeatingView) {
        AjaxButton suspend = new AjaxButton(repeatingView.newChildId(), createStringResource("pageTaskEdit.button.suspend")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String taskOid = getObjectWrapper().getOid();
                OperationResult result = TaskOperationUtils.suspendPerformed(getTaskService(), Collections.singletonList(taskOid), PageTask.this);
                afterOperation(target, result);
            }
        };
        suspend.add(new VisibleBehaviour(() -> WebComponentUtil.canSuspendTask(getTask(), PageTask.this)));
        suspend.add(AttributeAppender.append("class", "btn-danger"));
        repeatingView.add(suspend);
    }

    private void createResumeButton(RepeatingView repeatingView) {
        AjaxButton resume = new AjaxButton(repeatingView.newChildId(), createStringResource("pageTaskEdit.button.resume")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String oid = getObjectWrapper().getOid();
                OperationResult result = TaskOperationUtils.resumePerformed(getTaskService(), Collections.singletonList(oid), PageTask.this);
                afterOperation(target, result);
            }
        };
        resume.add(AttributeAppender.append("class", "btn-primary"));
        resume.add(new VisibleBehaviour(() -> WebComponentUtil.canResumeTask(getTask(), PageTask.this)));
        repeatingView.add(resume);
    }

    private void createRunNowButton(RepeatingView repeatingView) {
        AjaxButton runNow = new AjaxButton(repeatingView.newChildId(), createStringResource("pageTaskEdit.button.runNow")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String oid = getObjectWrapper().getOid();
                refreshEnabled = Boolean.TRUE;
                OperationResult result = TaskOperationUtils.runNowPerformed(getTaskService(), Collections.singletonList(oid), PageTask.this);
                afterOperation(target, result);
            }
        };
        runNow.add(AttributeAppender.append("class", "btn-success"));
        runNow.add(new VisibleBehaviour(() -> WebComponentUtil.canRunNowTask(getTask(), PageTask.this)));
        repeatingView.add(runNow);
    }

    private void createManageLivesyncTokenButton(RepeatingView repeatingView) {
        AjaxButton manageLivesyncToken = new AjaxButton(repeatingView.newChildId(), createStringResource("PageTask.livesync.token")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                LivesyncTokenEditorPanel tokenEditor = new LivesyncTokenEditorPanel(PageTask.this.getMainPopupBodyId(), getObjectModel()) {

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
                return WebComponentUtil.isLiveSync(getTask()) && isNotRunning() && getTask().asPrismObject().findProperty(LivesyncTokenEditorPanel.PATH_TOKEN) != null;
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
                ReportOutputType reportObject = getReportOutput();
                if (reportObject != null) {
                    return PageCreatedReports.createReport(reportObject, this, PageTask.this);
                } else {
                    return null;
                }
            }


            @Override
            public String getFileName() {
                ReportOutputType reportObject = getReportOutput();
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
        download.add(new VisibleBehaviour(() -> isDownloadReportVisible()));
        download.add(AttributeAppender.append("class", "btn-primary"));
        repeatingView.add(download);
    }

    private boolean isDownloadReportVisible() {
        return WebComponentUtil.isReport(getTask())
                && getReportOutputProperty() != null;
    }

    private ReportOutputType getReportOutput() {
        PrismProperty<String> reportOutput = getReportOutputProperty();
        if (reportOutput == null) {
            return null;
        }

        String reportOutputOid = reportOutput.getRealValue();
        if (reportOutputOid == null) {
            return null;
        }

        Task opTask = createSimpleTask(OPERATION_LOAD_REPORT_OUTPUT);
        OperationResult result = opTask.getResult();

        PrismObject<ReportOutputType> report = WebModelServiceUtils.loadObject(ReportOutputType.class, reportOutputOid, this, opTask, result);
        if (report == null) {
            return null;
        }
        result.computeStatusIfUnknown();
        showResult(result, false);

        return report.asObjectable();

    }

    private PrismProperty<String> getReportOutputProperty() {
        PrismObject<TaskType> task = getTask().asPrismObject();
        return task.findProperty(ItemPath.create(TaskType.F_EXTENSION, ReportConstants.REPORT_OUTPUT_OID_PROPERTY_NAME));
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

    private void createCleanupPerformanceButton(RepeatingView repeatingView) {
        AjaxCompositedIconButton cleanupPerformance = new AjaxCompositedIconButton(repeatingView.newChildId(), getTaskCleanupCompositedIcon(GuiStyleConstants.CLASS_ICON_PERFORMANCE),
                createStringResource("operationalButtonsPanel.cleanupEnvironmentalPerformance")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    getObjectWrapper().findProperty(ItemPath.create(TaskType.F_OPERATION_STATS,
                            OperationStatsType.F_ENVIRONMENTAL_PERFORMANCE_INFORMATION)).getValue().setRealValue(null);
                } catch (SchemaException e){
                    LOGGER.error("Cannot clear task results: {}", e.getMessage());
                }
                saveTaskChanges(target);
            }
        };
        cleanupPerformance.add(AttributeAppender.append("class", "btn btn-default btn-margin-left btn-sm"));
        cleanupPerformance.add(new VisibleBehaviour(this::isNotRunning));
        repeatingView.add(cleanupPerformance);
    }

    private void createCleanupResultsButton(RepeatingView repeatingView) {
        AjaxCompositedIconButton cleanupResults = new AjaxCompositedIconButton(repeatingView.newChildId(), getTaskCleanupCompositedIcon(GuiStyleConstants.CLASS_ICON_TASK_RESULTS),
                createStringResource("operationalButtonsPanel.cleanupResults")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    getObjectWrapper().findProperty(TaskType.F_RESULT).getValue().setRealValue(null);
                    getObjectWrapper().findProperty(TaskType.F_RESULT_STATUS).getValue().setRealValue(null);
                } catch (SchemaException e){
                    LOGGER.error("Cannot clear task results: {}", e.getMessage());
                }
                saveTaskChanges(target);
                refresh(target);
            }
        };
        cleanupResults.add(new VisibleBehaviour(this::isNotRunning));
        cleanupResults.add(AttributeAppender.append("class", "btn btn-default btn-margin-left btn-sm"));
        repeatingView.add(cleanupResults);
    }

    private void saveTaskChanges(AjaxRequestTarget target) {
        try {
            ObjectDelta<TaskType> taskDelta = getObjectWrapper().getObjectDelta();
            saveTaskChanges(target, taskDelta);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot get task delta.", e);
            getSession().error("Cannot save changes, there were problems with computing changes: " + e.getMessage());
            target.add(getFeedbackPanel());
        }
    }

    private void saveTaskChanges(AjaxRequestTarget target, ObjectDelta<TaskType> taskDelta){
        if (taskDelta.isEmpty()) {
            getSession().warn("Nothing to save, no changes were made.");
            return;
        }

        OperationResult result = new OperationResult(OPERATION_EXECUTE_TASK_CHANGES);
        Task task = createSimpleTask(OPERATION_EXECUTE_TASK_CHANGES);

        try {
            taskDelta.revive(getPrismContext()); //do we need revive here?
            getModelService().executeChanges(MiscUtil.createCollection(taskDelta), null, task, result);
            result.computeStatus();
            getObjectModel().reset();
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot save tasks changes", e);
            result.recordFatalError("Cannot save tasks changes, " + e.getMessage(), e);
        }
        afterOperation(target, result);
    }

    private CompositedIcon getTaskCleanupCompositedIcon(String basicIconClass){
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder();
        CompositedIcon icon = iconBuilder
                .setBasicIcon(basicIconClass, IconCssStyle.IN_ROW_STYLE)
                .appendLayerIcon(WebComponentUtil.createIconType(GuiStyleConstants.CLASS_ICON_TRASH), IconCssStyle.BOTTOM_RIGHT_STYLE)
                .build();
        return icon;
    }

    private void afterOperation(AjaxRequestTarget target, OperationResult result) {
        showResult(result);
        getObjectModel().reset();
        refresh(target);
    }

    public void saveAndRunPerformed(AjaxRequestTarget target) {
        PrismObjectWrapper<TaskType> taskWrapper = getObjectWrapper();
        try {
            PrismPropertyWrapper<TaskExecutionStatusType> executionStatus = taskWrapper.findProperty(ItemPath.create(TaskType.F_EXECUTION_STATUS));
            executionStatus.getValue().setRealValue(TaskExecutionStatusType.RUNNABLE);

            setupOwner(taskWrapper);
            setupRecurrence(taskWrapper);

        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error while finishing task settings.", e);
            target.add(getFeedbackPanel());
            return;
        }

        if (!checkScheduleFilledForReccurentTask(taskWrapper)) {
            getSession().error("Cannot run recurring task without setting scheduling for it.");
            target.add(getFeedbackPanel());
            return;
        }

        super.savePerformed(target);
    }

    private void setupOwner(PrismObjectWrapper<TaskType> taskWrapper) throws SchemaException {
        PrismReferenceWrapper<Referencable> taskOwner = taskWrapper.findReference(ItemPath.create(TaskType.F_OWNER_REF));
        if (taskOwner == null) {
            return;
        }
        PrismReferenceValueWrapperImpl<Referencable> taskOwnerValue = taskOwner.getValue();
        if (taskOwnerValue == null){
            return;
        }

        if (taskOwnerValue.getNewValue() == null || taskOwnerValue.getNewValue().isEmpty()) {
            GuiProfiledPrincipal guiPrincipal = SecurityUtils.getPrincipalUser();
            if (guiPrincipal == null) {
                //BTW something very strange must happened
                return;
            }
            FocusType focus = guiPrincipal.getFocus();
            taskOwnerValue.setRealValue(ObjectTypeUtil.createObjectRef(focus, SchemaConstants.ORG_DEFAULT));
        }
    }

    private void setupRecurrence(PrismObjectWrapper<TaskType> taskWrapper) throws SchemaException {
        PrismPropertyWrapper<TaskRecurrenceType> recurrenceWrapper = taskWrapper.findProperty(ItemPath.create(TaskType.F_RECURRENCE));
        if (recurrenceWrapper == null) {
            return;
        }

        PrismPropertyValueWrapper<TaskRecurrenceType> recurrenceWrapperValue = recurrenceWrapper.getValue();
        if (recurrenceWrapperValue == null) {
            return;
        }

        if (recurrenceWrapperValue.getNewValue() == null || recurrenceWrapperValue.getNewValue().isEmpty()) {
            recurrenceWrapperValue.setRealValue(TaskRecurrenceType.SINGLE);
        }


    }

    @Override
    public void finishProcessing(AjaxRequestTarget target, Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, boolean returningFromAsync, OperationResult result) {
        if (isPreviewRequested()) {
            super.finishProcessing(target, executedDeltas, returningFromAsync, result);
            return;
        }

        if (result.isSuccess() && executedDeltas != null) {
            //TODO change to inProgress result, so there is a link to existing task
            String taskOid = ObjectDeltaOperation.findFocusDeltaOidInCollection(executedDeltas);
            if (taskOid != null) {
                result.recordInProgress();
                result.setBackgroundTaskOid(taskOid);
            }
        }
        super.finishProcessing(target, executedDeltas, returningFromAsync, result);
    }

    private boolean checkScheduleFilledForReccurentTask(PrismObjectWrapper<TaskType> taskWrapper) {
        PrismObject<TaskType> task = taskWrapper.getObject();

        PrismProperty<TaskRecurrenceType> recurrenceType = task.findProperty(ItemPath.create(TaskType.F_RECURRENCE));
        if (recurrenceType == null) {
            return true;
        }

        TaskRecurrenceType recurenceValue = recurrenceType.getRealValue();
        if (recurenceValue == null || TaskRecurrenceType.SINGLE ==  recurenceValue) {
            return true;
        }

        ScheduleType schedule = task.asObjectable().getSchedule();
        //if schedule is not set and task is recurring, show warning.
        return schedule.getCronLikePattern() != null || schedule.getEarliestStartTime() != null
                || schedule.getInterval() != null || schedule.getLatestFinishTime() != null
                || schedule.getLatestStartTime() != null || schedule.getMisfireAction() != null;
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

    @Override
    protected AbstractObjectMainPanel<TaskType> createMainPanel(String id) {
        taskTabsVisibility = new TaskTabsVisibility();
        taskTabsVisibility.computeAll(this, getObjectWrapper());
        return new TaskMainPanel(id, getObjectModel(), this);
    }

    TaskType getTask(){
        return getObjectWrapper().getObject().asObjectable();
    }

    @Override
    protected Class<? extends Page> getRestartResponsePage() {
        return PageTasks.class;
    }

    @Override
    public void continueEditing(AjaxRequestTarget target) {

    }

    @Override
    public int getRefreshInterval() {
        return REFRESH_INTERVAL;
    }

    private boolean isNotRunning(){
        return !WebComponentUtil.isRunningTask(getTask());
    }

    public boolean isRefreshEnabled() {
        if (refreshEnabled == null) {
            return WebComponentUtil.isRunningTask(getTask());
        }

        return refreshEnabled;

    }

    @Override
    public void refresh(AjaxRequestTarget target) {
        TaskTabsVisibility taskTabsVisibilityNew = new TaskTabsVisibility();
        taskTabsVisibilityNew.computeAll(this, getObjectWrapper());

        boolean soft = false;
        if (taskTabsVisibilityNew.equals(taskTabsVisibility)) {
            soft = true;
        }

        taskTabsVisibility = taskTabsVisibilityNew;
        super.refresh(target, soft);
    }

    public TaskTabsVisibility getTaskTabVisibilty() {
        return taskTabsVisibility;
    }
}
