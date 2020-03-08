package com.evolveum.midpoint.web.page.admin.server;

import java.util.Collection;
import java.util.Collections;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
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
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.refresh.Refreshable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
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

    private static final int REFRESH_INTERVAL = 2000;

    private Boolean refreshEnabled;

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
    protected ObjectSummaryPanel<TaskType> createSummaryPanel() {
        return new TaskSummaryPanel(ID_SUMMARY_PANEL, createSummaryPanelModel(), this, this);
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> buildGetOptions() {
        //TODO use full options as defined in TaskDtoProviderOptions.fullOptions()

        return getOperationOptionsBuilder()
                // retrieve
                .item(TaskType.F_SUBTASK).retrieve()
                .item(TaskType.F_NODE_AS_OBSERVED).retrieve()
                .item(TaskType.F_NEXT_RUN_START_TIMESTAMP).retrieve()
                .item(TaskType.F_NEXT_RETRY_TIMESTAMP).retrieve()
                .item(TaskType.F_RESULT).retrieve()         // todo maybe only when it is to be displayed
                .build();
    }

    private void afterOperation(AjaxRequestTarget target, OperationResult result) {
        showResult(result);
        getObjectModel().reset();
        refresh(target);
    }

    protected void initOperationalButtons(RepeatingView repeatingView) {
        super.initOperationalButtons(repeatingView);
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

        AjaxIconButton refreshNow = new AjaxIconButton(repeatingView.newChildId(), new Model<>("fa fa-refresh"), createStringResource("autoRefreshPanel.refreshNow")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                refresh(target);
            }
        };
        refreshNow.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        repeatingView.add(refreshNow);

        AjaxIconButton resumePauseRefreshing = new AjaxIconButton(repeatingView.newChildId(), (IModel<String>) this::createResumePauseButton, createStringResource("autoRefreshPanel.resumeRefreshing")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                refreshEnabled = !isRefreshEnabled();
                refresh(target);
            }
        };
        resumePauseRefreshing.add(AttributeAppender.append("class", "btn btn-default btn-margin-left btn-sm"));
        repeatingView.add(resumePauseRefreshing);

        AjaxIconButton cleanupPerformance = new AjaxIconButton(repeatingView.newChildId(), new Model<>(GuiStyleConstants.CLASS_ICON_TRASH),
                createStringResource("operationalButtonsPanel.cleanupEnvironmentalPerformance")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    getObjectWrapper().findProperty(ItemPath.create(TaskType.F_OPERATION_STATS,
                            OperationStatsType.F_ENVIRONMENTAL_PERFORMANCE_INFORMATION)).getValue().setRealValue(null);
                } catch (SchemaException e){
                    LOGGER.error("Cannot clear task results: {}", e.getMessage());
                }
                saveTaskChanges();
                refresh(target);
            }
        };
        cleanupPerformance.add(AttributeAppender.append("class", "btn btn-default btn-margin-left btn-sm"));
        cleanupPerformance.add(new VisibleBehaviour(this::isNotRunning));
        repeatingView.add(cleanupPerformance);

        AjaxIconButton cleanupResults = new AjaxIconButton(repeatingView.newChildId(), new Model<>(GuiStyleConstants.CLASS_ICON_TRASH),
                createStringResource("operationalButtonsPanel.cleanupResults")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    getObjectWrapper().findProperty(TaskType.F_RESULT).getValue().setRealValue(null);
                    getObjectWrapper().findProperty(TaskType.F_RESULT_STATUS).getValue().setRealValue(null);
                } catch (SchemaException e){
                    LOGGER.error("Cannot clear task results: {}", e.getMessage());
                }
                saveTaskChanges();
                refresh(target);
            }
        };
        cleanupResults.add(new VisibleBehaviour(this::isNotRunning));
        cleanupResults.add(AttributeAppender.append("class", "btn btn-default btn-margin-left btn-sm"));
        repeatingView.add(cleanupResults);

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

    private void saveTaskChanges(){
        OperationResult result = new OperationResult(OPERATION_EXECUTE_TASK_CHANGES);
        Task task = createSimpleTask(OPERATION_EXECUTE_TASK_CHANGES);
        try {
            ObjectDelta<TaskType> taskDelta = getObjectWrapper().getObjectDelta();
            if (!taskDelta.isEmpty()) {
                taskDelta.revive(getPrismContext());
                getModelService().executeChanges(MiscUtil.createCollection(taskDelta), null, task, result);
                result.computeStatus();
                getObjectModel().reset();
            }
        } catch (Exception e) {
            LOGGER.error("Cannot save tasks changes: {}", e.getMessage());
        }
        showResult(result);
    }

    public void saveAndRunPerformed(AjaxRequestTarget target) {
        PrismObjectWrapper<TaskType> taskWrapper = getObjectWrapper();
        try {
            PrismPropertyWrapper<TaskExecutionStatusType> executionStatus = taskWrapper.findProperty(ItemPath.create(TaskType.F_EXECUTION_STATUS));
            executionStatus.getValue().setRealValue(TaskExecutionStatusType.RUNNABLE);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error while setting task execution status", e);
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

    private String createResumePauseButton() {
        if (isRefreshEnabled()) {
            return "fa fa-pause";
        }
        return "fa fa-play";
    }

    private IModel<TaskType> createSummaryPanelModel() {
        return isEditingFocus() ?

                new LoadableModel<TaskType>(true) {
                    @Override
                    protected TaskType load() {
                        PrismObjectWrapper<TaskType> taskWrapper = getObjectWrapper();
                        if (taskWrapper == null) {
                            return null;
                        }
                        return taskWrapper.getObject().asObjectable();
                    }
                } : Model.of();
    }

    @Override
    protected AbstractObjectMainPanel<TaskType> createMainPanel(String id) {
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

    public void refresh(AjaxRequestTarget target) {

        target.add(getSummaryPanel());
        target.add(getOperationalButtonsPanel());
        target.add(getFeedbackPanel());
//        target.add(getMainPanel());

        for (Component component : getMainPanel().getTabbedPanel()) {
            if (component instanceof TaskTabPanel) {

                for (Component c : ((TaskTabPanel) component).getComponentsToUpdate()) {
                    target.add(c);
                }
            }
        }

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
}
