/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.TimerProgressPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingDto;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author lskublik
 */
public abstract class MultiWaitingConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(MultiWaitingConnectorStepPanel.class);

    private static final String CLASS_DOT = MultiWaitingConnectorStepPanel.class.getName() + ".";
    private static final String OP_DETERMINE_STATUS = CLASS_DOT + "determineStatus";
    private static final String OP_LOAD_CONNECTOR = CLASS_DOT + "loadConnector";
    private static final String OP_RESTART_ACTIVITY = CLASS_DOT + "restartActivity";

    private static final String ID_TITLE_ICON = "titleIcon";
    private static final String ID_TITLE_LABEL = "titleLabel";
    private static final String ID_SUBTITLE_LABEL = "subtitleLabel";
    private static final String ID_TASK_CONTAINER = "taskContainer";
    private static final String ID_TASK_REPEATER = "taskRepeater";
    private static final String ID_TASK_ICON = "taskIcon";
    private static final String ID_TASK_LABEL = "taskLabel";
    private static final String ID_STOP_BUTTON = "stopButton";
    private static final String ID_RETRY_BUTTON = "retryButton";
    private static final String ID_ELAPSED_TIME = "elapsedTime";

    private LoadableModel<List<TaskStatusDto>> statusesModel;
    private WebMarkupContainer taskListContainer;
    private WebMarkupContainer titleIconContainer;
    private org.apache.wicket.ajax.AbstractAjaxTimerBehavior refreshTimer;
    private boolean isReloaded = false;

    public MultiWaitingConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    protected final void markAsReloaded() {
        isReloaded = true;
    }

    protected final boolean isReloaded() {
        return isReloaded;
    }

    @Override
    public void init(WizardModel wizard) {
        super.init(wizard);

        statusesModel = new LoadableModel<>() {
            @Override
            protected List<TaskStatusDto> load() {
                String oid = getDetailsModel().getObjectWrapper().getOid();

                Task task = getDetailsModel().getPageAssignmentHolder().createSimpleTask(OP_DETERMINE_STATUS);
                OperationResult result = task.getResult();

                List<ItemName> activityTypes = getActivityTypes();
                List<String> tokens = new ArrayList<>();
                boolean anyMissing = false;

                for (ItemName activityType : activityTypes) {
                    try {
                        String token = ConnectorDevelopmentWizardUtil.getTaskToken(
                                activityType,
                                getObjectClassName(),
                                null,
                                oid,
                                getDetailsModel().getPageAssignmentHolder());
                        if (StringUtils.isEmpty(token)) {
                            anyMissing = true;
                        }
                        tokens.add(token);
                    } catch (CommonException e) {
                        LOGGER.error("Couldn't search tasks for " + activityType);
                        tokens.add(null);
                        anyMissing = true;
                    }
                }

                if (anyMissing && !isReloaded) {
                    if (oid == null) {
                        List<TaskStatusDto> dtos = new ArrayList<>();
                        for (int i = 0; i < activityTypes.size(); i++) {
                            dtos.add(createDto(activityTypes.get(i), tokens.get(i), task, result));
                        }
                        return dtos;
                    }
                    String objectClassName = null;
                    try {
                        objectClassName = getObjectClassName();
                    } catch (Exception ignored) {}
                    if (objectClassRequired() && StringUtils.isEmpty(objectClassName)) {
                        List<TaskStatusDto> dtos = new ArrayList<>();
                        for (int i = 0; i < activityTypes.size(); i++) {
                            dtos.add(createDto(activityTypes.get(i), tokens.get(i), task, result));
                        }
                        return dtos;
                    }
                    triggerOperation(task, result);
                    markAsReloaded();
                    // Refresh tokens after submission
                    tokens.clear();
                    for (ItemName activityType : activityTypes) {
                        try {
                            String refreshed = ConnectorDevelopmentWizardUtil.getTaskToken(
                                    activityType,
                                    getObjectClassName(),
                                    null,
                                    oid,
                                    getDetailsModel().getPageAssignmentHolder());
                            tokens.add(refreshed);
                        } catch (CommonException e) {
                            LOGGER.error("Couldn't refresh token for " + activityType, e);
                            tokens.add(null);
                        }
                    }
                }

                List<TaskStatusDto> dtos = new ArrayList<>();
                for (int i = 0; i < activityTypes.size(); i++) {
                    dtos.add(createDto(activityTypes.get(i), tokens.get(i), task, result));
                }
                return dtos;
            }
        };
    }

    protected abstract List<ItemName> getActivityTypes();

    protected abstract StatusInfo<?> obtainResult(ItemName activityType, String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException;

    protected abstract String triggerOperation(Task task, OperationResult result);

    protected String getObjectClassName() {
        return null;
    }

    protected abstract boolean objectClassRequired();

    private TaskStatusDto createDto(ItemName activityType, String token, Task opTask, OperationResult opResult) {
        if (StringUtils.isEmpty(token)) {
            return new TaskStatusDto(activityType, getTaskTitleModel(activityType), null, null);
        }

        LoadableModel<StatusInfo<?>> statusInfoModel = new LoadableModel<>() {
            @Override
            protected StatusInfo<?> load() {
                try {
                    MidPointApplication app = MidPointApplication.get();
                    Task task = app.createSimpleTask(OP_DETERMINE_STATUS);
                    OperationResult result = task.getResult();
                    return obtainResult(activityType, token, task, result);
                } catch (SchemaException|ObjectNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        PrismObject<TaskType> taskTypePrismObject = WebModelServiceUtils.loadObject(TaskType.class, token, getDetailsModel().getPageAssignmentHolder(), opTask, opResult);
        return new TaskStatusDto(activityType, getTaskTitleModel(activityType), statusInfoModel, () -> taskTypePrismObject);
    }

    protected IModel<String> getTaskTitleModel(ItemName activityType) {
        return Model.of(activityType.getLocalPart());
    }

    protected IModel<String> getSubtitle() {
        return Model.of("");
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        refreshTimer = new org.apache.wicket.ajax.AbstractAjaxTimerBehavior(java.time.Duration.ofSeconds(2)) {
            @Override
            protected void onTimer(AjaxRequestTarget target) {
                statusesModel.reset();
                List<TaskStatusDto> dtos = statusesModel.getObject();
                target.add(taskListContainer);
                target.add(titleIconContainer);
                if (dtos.stream().anyMatch(SmartGeneratingDto::isFailed)) {
                    reportFailedTasksToRightPanel(dtos, target);
                }
                target.add(get(ID_STOP_BUTTON));
                target.add(get(ID_RETRY_BUTTON));
                target.add(get(ID_ELAPSED_TIME));
                boolean allDone = !dtos.isEmpty() && dtos.stream().allMatch(dto -> dto.isFinished() || dto.isFailed());
                if (allDone) {
                    stop(target);
                    if (dtos.stream().noneMatch(dto -> dto.isFailed() || dto.isSuspended())) {
                        MultiWaitingConnectorStepPanel.this.onNextPerformed(target);
                    }
                }
            }
        };
        add(refreshTimer);
    }

    @Override
    protected IModel<String> getTextModel() {
        return getTitle();
    }

    private void initLayout() {
        getSubtextLabel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        getFeedback().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        // parent's title label renders above the child markup, we show our own title below the spinner instead
        getTextLabel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);

        titleIconContainer = new WebMarkupContainer(ID_TITLE_ICON);
        titleIconContainer.setOutputMarkupId(true);
        titleIconContainer.add(AttributeModifier.replace("class", () -> resolveTitleIconClass()));
        add(titleIconContainer);

        add(new Label(ID_TITLE_LABEL, getTitle()));

        IModel<String> subtitleModel = getSubtitle();
        Label subtitleLabel = new Label(ID_SUBTITLE_LABEL, subtitleModel);
        subtitleLabel.add(new VisibleBehaviour(() -> StringUtils.isNotBlank(subtitleModel.getObject())));
        add(subtitleLabel);

        taskListContainer = new WebMarkupContainer(ID_TASK_CONTAINER);
        taskListContainer.setOutputMarkupId(true);
        add(taskListContainer);

        ListView<TaskStatusDto> taskList = new ListView<>(ID_TASK_REPEATER, statusesModel) {
            @Override
            protected void populateItem(ListItem<TaskStatusDto> item) {
                WebMarkupContainer icon = new WebMarkupContainer(ID_TASK_ICON);
                icon.add(AttributeModifier.replace("class", () -> resolveIconClass(item.getModelObject())));
                item.add(icon);
                Label label = new Label(ID_TASK_LABEL, item.getModelObject().getTitleModel());
                // finished rows are grey, the running one keeps the default (toned) color
                label.add(AttributeAppender.append("class",
                        () -> item.getModelObject().isFinished() || item.getModelObject().isFailed() ? "text-muted" : ""));
                item.add(label);
            }
        };
        taskList.setReuseItems(false);
        taskListContainer.add(taskList);

        TimerProgressPanel elapsedTime = new TimerProgressPanel(ID_ELAPSED_TIME,
                () -> getEarliestStartTime(),
                () -> getLatestEndTime());
        add(elapsedTime);

        AjaxLink<Void> stopButton = new AjaxLink<>(ID_STOP_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                stopRunningTasks(target);
            }
        };
        stopButton.setOutputMarkupPlaceholderTag(true);
        stopButton.add(new VisibleBehaviour(this::anyTaskRunning));
        add(stopButton);

        AjaxLink<Void> retryButton = new AjaxLink<>(ID_RETRY_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                retryNotSuccessfulTasks(target);
            }
        };
        retryButton.setOutputMarkupPlaceholderTag(true);
        retryButton.add(new VisibleBehaviour(() -> !anyTaskRunning() && anyTaskNotSuccessful()));
        add(retryButton);
    }

    private XMLGregorianCalendar getEarliestStartTime() {
        return statusesModel.getObject().stream()
                .map(SmartGeneratingDto::getSuggestedObjectsStartTime)
                .filter(Objects::nonNull)
                .min(Comparator.comparingLong(calendar -> calendar.toGregorianCalendar().getTimeInMillis()))
                .orElse(null);
    }

    private XMLGregorianCalendar getLatestEndTime() {
        List<TaskStatusDto> dtos = statusesModel.getObject();
        boolean allDone = !dtos.isEmpty() && dtos.stream().allMatch(dto -> dto.isFinished() || dto.isFailed());
        if (!allDone) {
            return null;
        }
        return dtos.stream()
                .map(SmartGeneratingDto::getSuggestedObjectsEndTime)
                .filter(Objects::nonNull)
                .max(Comparator.comparingLong(calendar -> calendar.toGregorianCalendar().getTimeInMillis()))
                .orElse(null);
    }

    private boolean anyTaskRunning() {
        return statusesModel.getObject().stream()
                .anyMatch(dto -> !dto.isFinished() && !dto.isFailed());
    }

    private boolean anyTaskNotSuccessful() {
        return statusesModel.getObject().stream()
                .anyMatch(dto -> dto.isFailed() || dto.isSuspended());
    }

    private void retryNotSuccessfulTasks(AjaxRequestTarget target) {
        List<ItemName> activitiesToRetry = statusesModel.getObject().stream()
                .filter(dto -> dto.isFailed() || dto.isSuspended())
                .map(TaskStatusDto::getActivityType)
                .toList();
        if (activitiesToRetry.isEmpty()) {
            return;
        }

        Task task = getDetailsModel().getPageAssignmentHolder().createSimpleTask(OP_RESTART_ACTIVITY);
        OperationResult result = task.getResult();
        for (ItemName activityType : activitiesToRetry) {
            restartActivity(activityType, task, result);
            if (getWizard() instanceof WizardModelWithParentSteps wizardModel) {
                wizardModel.removeOperationResult(getStepId() + "." + activityType.getLocalPart());
            }
        }

        refreshAfterRestart(target);
    }

    private void stopRunningTasks(AjaxRequestTarget target) {
        List<TaskType> runningTasks = statusesModel.getObject().stream()
                .map(TaskStatusDto::getTaskObject)
                .filter(Objects::nonNull)
                .filter(taskBean -> taskBean.getExecutionState() != TaskExecutionStateType.CLOSED
                        && taskBean.getExecutionState() != TaskExecutionStateType.SUSPENDED)
                .toList();
        if (!runningTasks.isEmpty()) {
            TaskOperationUtils.suspendTasks(runningTasks, getPageBase());
        }
        statusesModel.reset();
        target.add(taskListContainer);
        target.add(titleIconContainer);
        target.add(get(ID_STOP_BUTTON));
        target.add(get(ID_RETRY_BUTTON));
        target.add(get(ID_ELAPSED_TIME));
    }

    private String resolveTitleIconClass() {
        if (statusesModel == null || !statusesModel.isLoaded()) {
            return "fa fa-spinner fa-spin fa-2x text-primary";
        }
        List<TaskStatusDto> dtos = statusesModel.getObject();
        if (dtos.stream().anyMatch(SmartGeneratingDto::isFailed)) {
            return "fa fa-exclamation-triangle fa-2x text-danger";
        }
        if (dtos.stream().anyMatch(SmartGeneratingDto::isSuspended)) {
            return "fa fa-pause-circle fa-2x text-info";
        }
        if (dtos.stream().allMatch(SmartGeneratingDto::isFinished)) {
            return "fa fa-check fa-2x text-success";
        }
        return "fa fa-spinner fa-spin fa-2x text-primary";
    }

    @Override
    public String appendCssToWizard() {
        return "col-12";
    }

    @Override
    protected boolean isSubmitVisible() {
        return false;
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
    }

    @Override
    public boolean isCompleted() {
        List<TaskStatusDto> dtos = statusesModel.getObject();
        if (dtos == null || dtos.isEmpty()) {
            return false;
        }

        for (TaskStatusDto dto : dtos) {
            String token = dto.getToken();
            if (StringUtils.isEmpty(token)) {
                return false;
            }

            Task operationTask = getDetailsModel().getPageAssignmentHolder().createSimpleTask("create_task_instance");
            try {
                Task task = getDetailsModel().getPageAssignmentHolder().getTaskManager().getTask(
                        token, null, operationTask.getResult());
                if (task == null || task.getExecutionState() != TaskExecutionStateType.CLOSED || task.getResultStatus() != OperationResultStatusType.SUCCESS) {
                    return false;
                }
            } catch (CommonException e) {
                LOGGER.error("Couldn't create Task instance");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        String oid = getDetailsModel().getObjectWrapper().getOid();
        Task task = getPageBase().createSimpleTask(OP_LOAD_CONNECTOR);

        getDetailsModel().reset();
        getDetailsModel().reloadPrismObjectModel(
                WebModelServiceUtils.loadObject(ConnectorDevelopmentType.class, oid, getPageBase(), task, task.getResult()));
        isReloaded = false;
        return super.onNextPerformed(target);
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    protected boolean isSubmitEnable() {
        return false;
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> {
            if (statusesModel == null || !statusesModel.isLoaded()) {
                boolean completed = isCompleted();
                return !completed;
            }
            boolean anyNotFinished = statusesModel.getObject().stream()
                    .anyMatch(dto -> !dto.isFinished() || dto.isSuspended());
            return anyNotFinished;
        };
    }

    private void reportFailedTasksToRightPanel(List<TaskStatusDto> dtos, AjaxRequestTarget target) {
        if (!(getWizard() instanceof WizardModelWithParentSteps wizardModel)) {
            return;
        }

        for (ItemName activityType : getActivityTypes()) {
            wizardModel.removeOperationResult(getStepId() + "." + activityType.getLocalPart());
        }

        for (TaskStatusDto dto : dtos) {
            if (!dto.isFailed()) {
                continue;
            }
            String taskTitle = dto.getTitleModel() != null ? dto.getTitleModel().getObject() : "Unknown task";
            String localizedMsg = (dto.getStatusInfo() != null && dto.getStatusInfo().getObject() != null)
                    ? dto.getStatusInfo().getObject().getLocalizedMessage()
                    : null;
            String message = (localizedMsg != null && !localizedMsg.isBlank()) ? localizedMsg : taskTitle + " failed";

            OperationResult taskResult = new OperationResult(taskTitle);
            taskResult.recordFatalError(message);
            ItemName activityType = dto.getActivityType();
            wizardModel.addOperationResult(
                    getStepId() + "." + activityType.getLocalPart(),
                    taskResult,
                    fixTarget -> restartFailedActivity(activityType, fixTarget));
        }

        Component collapsedInfoPanel = getWizard().getPanel().get("mainForm:collapsedInfoPanel");
        if (collapsedInfoPanel != null) {
            target.add(collapsedInfoPanel);
        }
    }

    private void restartFailedActivity(ItemName activityType, AjaxRequestTarget target) {
        Task task = getDetailsModel().getPageAssignmentHolder().createSimpleTask(OP_RESTART_ACTIVITY);
        OperationResult result = task.getResult();
        restartActivity(activityType, task, result);

        if (getWizard() instanceof WizardModelWithParentSteps wizardModel) {
            wizardModel.removeOperationResult(getStepId() + "." + activityType.getLocalPart());
        }

        refreshAfterRestart(target);
    }

    private void refreshAfterRestart(AjaxRequestTarget target) {
        statusesModel.reset();
        if (refreshTimer != null && refreshTimer.isStopped()) {
            refreshTimer.restart(target);
        }
        target.add(taskListContainer);
        target.add(titleIconContainer);
        target.add(get(ID_STOP_BUTTON));
        target.add(get(ID_RETRY_BUTTON));
        target.add(get(ID_ELAPSED_TIME));

        Component collapsedInfoPanel = getWizard().getPanel().get("mainForm:collapsedInfoPanel");
        if (collapsedInfoPanel != null) {
            target.add(collapsedInfoPanel);
        }
    }

    /** Resubmits the task for the given activity. Used by the 'Fix it' button when the previous run failed. */
    protected abstract String restartActivity(ItemName activityType, Task task, OperationResult result);

    private static String resolveIconClass(TaskStatusDto dto) {
        if (dto.getStatusInfo() == null || dto.getStatusInfo().getObject() == null) {
            return "fa fa-spinner fa-spin text-secondary";
        }
        if (dto.isFailed()) return "fa fa-exclamation-triangle text-danger";
        if (dto.isSuspended()) return "fa fa-pause-circle text-info";
        if (dto.isFinished()) return "fa fa-check text-success";
        return "fa fa-spinner fa-spin";
    }

    private static class TaskStatusDto extends SmartGeneratingDto {
        private final ItemName activityType;
        private final IModel<String> titleModel;

        public TaskStatusDto(ItemName activityType, IModel<String> titleModel, LoadableModel<StatusInfo<?>> statusInfo, IModel<PrismObject<TaskType>> taskModel) {
            super(statusInfo, taskModel);
            this.activityType = activityType;
            this.titleModel = titleModel;
        }

        public IModel<String> getTitleModel() {
            return titleModel;
        }

        public ItemName getActivityType() {
            return activityType;
        }
    }
}
