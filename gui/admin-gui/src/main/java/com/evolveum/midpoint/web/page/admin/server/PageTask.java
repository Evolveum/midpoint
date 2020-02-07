package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.*;
import com.evolveum.midpoint.web.component.data.column.LinkIconPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.refresh.AutoRefreshDto;
import com.evolveum.midpoint.web.component.refresh.AutoRefreshPanel;
import com.evolveum.midpoint.web.component.refresh.Refreshable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskRecurrenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.time.Duration;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

    private static final String ID_BUTTONS = "buttons";

    private static final int REFRESH_INTERVAL_IF_RUNNING = 2000;
    private static final int REFRESH_INTERVAL_IF_RUNNABLE = 2000;
    private static final int REFRESH_INTERVAL_IF_SUSPENDED = 2000;
    private static final int REFRESH_INTERVAL_IF_WAITING = 2000;
    private static final int REFRESH_INTERVAL_IF_CLOSED = 2000;

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
        return new TaskSummaryPanelNew(ID_SUMMARY_PANEL, createSummaryPanelModel(), this, this);
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> buildGetOptions() {
        return getOperationOptionsBuilder()
                // retrieve
                .item(TaskType.F_SUBTASK).retrieve()
                .item(TaskType.F_NODE_AS_OBSERVED).retrieve()
                .item(TaskType.F_NEXT_RUN_START_TIMESTAMP).retrieve()
                .item(TaskType.F_NEXT_RETRY_TIMESTAMP).retrieve()
                .item(TaskType.F_RESULT).retrieve()         // todo maybe only when it is to be displayed
                .build();
    }

    @Override
    protected void initLayout() {
        super.initLayout();

        OperationalButtonsPanel opButtonPanel = new OperationalButtonsPanel(ID_BUTTONS) {

            @Override
            protected void addButtons(RepeatingView repeatingView) {
                initTaskOperationalButtons(repeatingView);
            }
        };

        AjaxSelfUpdatingTimerBehavior behavior = new AjaxSelfUpdatingTimerBehavior(Duration.milliseconds(getRefreshInterval())) {
            @Override
            protected void onPostProcessTarget(AjaxRequestTarget target) {
                refresh(target);
            }

            @Override
            protected boolean shouldTrigger() {
                return isRefreshEnabled();
            }
        };
        opButtonPanel.add(behavior);


        opButtonPanel.setOutputMarkupId(true);
        opButtonPanel.add(new VisibleBehaviour(this::isEditingFocus));
        add(opButtonPanel);
    }

    private void afterOperation(AjaxRequestTarget target, OperationResult result) {
        showResult(result);
        getObjectModel().reset();
        refresh(target);
//        target.add(PageTask.this);
        target.add(getFeedbackPanel());
    }



    private void initTaskOperationalButtons(RepeatingView repeatingView) {

        AjaxButton suspend = new AjaxButton(repeatingView.newChildId(), createStringResource("pageTaskEdit.button.suspend")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String taskOid = getObjectWrapper().getOid();
                OperationResult result = TaskOperationUtils.suspendPerformed(getTaskService(), Collections.singletonList(taskOid), PageTask.this);
                afterOperation(target, result);
            }
        };
        suspend.add(new VisibleBehaviour(this::canSuspend));
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
        resume.add(new VisibleBehaviour(this::canResume));
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
        runNow.add(new VisibleBehaviour(this::canRunNow));
        repeatingView.add(runNow);

        AjaxIconButton refreshNow = new AjaxIconButton(repeatingView.newChildId(), new Model<>("fa fa-refresh"), createStringResource("autoRefreshPanel.refreshNow")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                refresh(target);
            }
        };
        refreshNow.add(AttributeAppender.append("class", "btn btn-default btn-margin-left btn-sm"));
        repeatingView.add(refreshNow);

        AjaxIconButton resumePauseRefreshing = new AjaxIconButton(repeatingView.newChildId(), (IModel<String>) this::createResumePauseButton, createStringResource("autoRefreshPanel.resumeRefreshing")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                refreshEnabled = !isRefreshEnabled();
//                getModelObject().setEnabled(!getModelObject().isEnabled());
                refresh(target);
            }
        };
        resumePauseRefreshing.add(AttributeAppender.append("class", "btn btn-default btn-margin-left btn-sm"));
        repeatingView.add(resumePauseRefreshing);

        setOutputMarkupId(true);


        final Label status = new Label(repeatingView.newChildId(), () -> createRefreshingLabel());
        status.setOutputMarkupId(true);
        repeatingView.add(status);

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

    private boolean canSuspend() {
        PrismObject<TaskType> task = getObjectWrapper().getObject();
        TaskType taskType = task.asObjectable();
        return isAuthorized(ModelAuthorizationAction.SUSPEND_TASK, task)
                && isRunnable(taskType) || isRunning(taskType)
                && !isWorkflow(task.asObjectable());
    }

    private boolean canResume() {
        PrismObject<TaskType> task = getObjectWrapper().getObject();
        TaskType taskType = task.asObjectable();
        return isAuthorized(ModelAuthorizationAction.RESUME_TASK, task)
                && (isSuspended(taskType) || (isClosed(taskType) && isRecurring(taskType)))
                && !isWorkflow(taskType);
    }


    private boolean canRunNow() {
        PrismObject<TaskType> task = getObjectWrapper().getObject();
        TaskType taskType = task.asObjectable();
        return isAuthorized(ModelAuthorizationAction.RUN_TASK_IMMEDIATELY, task)
                && (isRunnable(taskType) || (isClosed(taskType) && !isRecurring(taskType)))
                && !isWorkflow(taskType);
    }

    private boolean isRunnable(TaskType task) {
        return  TaskExecutionStatusType.RUNNABLE == task.getExecutionStatus();
    }

    private boolean isRunning(TaskType task) {
        return task.getNodeAsObserved() != null;
    }

    private boolean isSuspended(TaskType task) {
        return TaskExecutionStatusType.SUSPENDED == task.getExecutionStatus();
    }

    private boolean isClosed(TaskType task) {
        return TaskExecutionStatusType.CLOSED == task.getExecutionStatus();
    }

    private boolean isRecurring(TaskType task) {
        return TaskRecurrenceType.RECURRING == task.getRecurrence();
    }

    private boolean isWorkflow(TaskType task) {
        return TaskCategory.WORKFLOW.equals(task.getCategory());
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

        return new AbstractObjectMainPanel<TaskType>(id, getObjectModel(), this) {

            @Override
            protected boolean getOptionsPanelVisibility() {
                return false;
            }

            @Override
            protected List<ITab> createTabs(PageAdminObjectDetails<TaskType> parentPage) {
                List<ITab> tabs = new ArrayList<>();
                tabs.add(new AbstractTab(createStringResource("pageTask.basic.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return createContainerPanel(panelId, TaskType.COMPLEX_TYPE, getObjectModel());
                    }
                });

                tabs.add(new AbstractTab(createStringResource("pageTask.schedule.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return createContainerPanel(panelId, TaskType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), TaskType.F_SCHEDULE));
                    }
                });

                tabs.add(new AbstractTab(createStringResource("pageTask.subtasks.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return new TaskSubtasksAndThreadsTabPanelNew(panelId, getObjectModel());
                    }

                    @Override
                    public boolean isVisible() {
                        return isEditingFocus();
                    }
                });

                tabs.add(new AbstractTab(createStringResource("pageTask.operationStats.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return new TaskOperationStatisticsPanel(panelId, getObjectModel());
                    }
                    @Override
                    public boolean isVisible() {
                        return isEditingFocus();
                    }
                });

                tabs.add(new AbstractTab(createStringResource("pageTask.internalPerformane.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return new TaskInternalPerformanceTabPanelNew(panelId, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), TaskType.F_OPERATION_STATS));
                    }
                    @Override
                    public boolean isVisible() {
                        return isEditingFocus();
                    }
                });

                tabs.add(new AbstractTab(createStringResource("pageTask.result.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return new TaskResultTabPanelNew(panelId, getObjectModel());
                    }
                    @Override
                    public boolean isVisible() {
                        return isEditingFocus();
                    }
                });


                tabs.add(new AbstractTab(createStringResource("pageTask.errors.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return new TaskErrorsTabPanelNew(panelId, getObjectModel());
                    }
                    @Override
                    public boolean isVisible() {
                        return isEditingFocus();
                    }
                });

                return tabs;
            }
        };
    }



    private <C extends Containerable> Panel createContainerPanel(String id, QName typeName, IModel<? extends PrismContainerWrapper<C>> model) {
            try {
                ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder()
                        .visibilityHandler(wrapper -> getVisibility(wrapper.getPath()))
                        .showOnTopLevel(true);
                Panel panel = initItemPanel(id, typeName, model, builder.build());
                return panel;
            } catch (SchemaException e) {
                LOGGER.error("Cannot create panel for {}, {}", typeName, e.getMessage(), e);
                getSession().error("Cannot create panel for " + typeName); // TODO opertion result? localization?
            }

            return null;
    }

    private ItemVisibility getVisibility(ItemPath path) {
        return ItemVisibility.AUTO;
    }

    @Override
    protected Class<? extends Page> getRestartResponsePage() {
        return PageTasks.class;
    }

    @Override
    public void finishProcessing(AjaxRequestTarget target, OperationResult result, boolean returningFromAsync) {

    }

    @Override
    public void continueEditing(AjaxRequestTarget target) {

    }

    @Override
    public int getRefreshInterval() {
        TaskType task = getObjectWrapper().getObject().asObjectable();
        TaskDtoExecutionStatus exec = TaskDtoExecutionStatus.fromTaskExecutionStatus(task.getExecutionStatus(), task.getNodeAsObserved() != null);
        if (exec == null) {
            return REFRESH_INTERVAL_IF_CLOSED;
        }
        switch (exec) {
            case RUNNING:
            case SUSPENDING: return REFRESH_INTERVAL_IF_RUNNING;
            case RUNNABLE:return REFRESH_INTERVAL_IF_RUNNABLE;
            case SUSPENDED: return REFRESH_INTERVAL_IF_SUSPENDED;
            case WAITING: return REFRESH_INTERVAL_IF_WAITING;
            case CLOSED: return REFRESH_INTERVAL_IF_CLOSED;
        }
        return REFRESH_INTERVAL_IF_RUNNABLE;
    }

    @Override
    public Component getRefreshingBehaviorParent() {
        return null; //nothing to do, this method will be removed
    }

    public void refresh(AjaxRequestTarget target) {

//        getObjectModel().reset();
        target.add(getSummaryPanel());
        target.add(get(ID_BUTTONS));

        for (Component component : getMainPanel().getTabbedPanel()) {
            if (component instanceof TaskTabPanel) {

                for (Component c : ((TaskTabPanel) component).getComponentsToUpdate()) {
                    target.add(c);
                }
            }
        }

    }

    public boolean isRefreshEnabled() {
        if (refreshEnabled == null) {
            return isRunning(getObjectWrapper().getObject().asObjectable());
        }

        return refreshEnabled;

    }
}
