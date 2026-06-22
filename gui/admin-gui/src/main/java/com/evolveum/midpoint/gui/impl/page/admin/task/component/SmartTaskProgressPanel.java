/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.TimerProgressPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.component.ThreadSelectionPanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.task.TaskInformation;
import com.evolveum.midpoint.schema.util.task.TaskResultStatus;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.TaskExecutionProgress;
import com.evolveum.midpoint.web.page.admin.server.dto.GuiTaskResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.time.Duration;

public abstract class SmartTaskProgressPanel extends BasePanel<String> implements Popupable {

    private static final String ID_CONTAINER = "container";

    private static final String ID_TITLE = "title";
    private static final String ID_SUBTITLE = "subtitle";
    private static final String ID_CONFIGURATION = "configuration";

    private static final String ID_STATUS_BOX = "statusBox";
    private static final String ID_TIME_LABEL = "timeLabel";

    private static final String ID_PROGRESS_INFO_BOX = "progressInfoBox";
    private static final String ID_PROGRESS_LABEL = "progressLabel";
    private static final String ID_PROGRESS_BAR = "progressBar";
    private static final String ID_PROGRESS_BOX = "progressBox";

    private static final String ID_BUTTONS = "buttons";
    private static final String ID_STOP = "stop";
    private static final String ID_NAVIGATE_TO_TASK = "navigateToTask";
    private static final String ID_SHOW_RESULT = "showResult";

    private static final String ID_CANCEL_BUTTON = "close";
    private static final String ID_START_PROCESSING = "startProcessing";

    private static final Duration REFRESH_INTERVAL = Duration.ofSeconds(2);

    private final IModel<String> titleModel;
    private final IModel<String> subtitleModel;
    private final IModel<Integer> threadsModel = Model.of(4);

    private IModel<TaskType> taskModel;
    private SerializableTaskRunner taskRunner;

    private Fragment footer;
    private boolean started;
    private boolean isTaskOperationCompleted;

    private AjaxSelfUpdatingTimerBehavior timerBehavior;

    @FunctionalInterface
    public interface SerializableTaskRunner extends Serializable {

        IModel<TaskType> run(AjaxRequestTarget target, @NotNull Integer threads);
    }

    //Direct progress
    public SmartTaskProgressPanel(
            String id,
            IModel<String> titleModel,
            IModel<String> subtitleModel,
            IModel<TaskType> taskModel) {
        super(id);
        this.titleModel = titleModel;
        this.subtitleModel = subtitleModel;
        this.taskModel = taskModel;
        this.started = true;
        setOutputMarkupId(true);
    }

    //Thread configuration
    public SmartTaskProgressPanel(
            String id,
            IModel<String> titleModel,
            IModel<String> subtitleModel,
            SerializableTaskRunner taskRunner) {
        super(id);
        this.titleModel = titleModel;
        this.subtitleModel = subtitleModel;
        this.taskRunner = taskRunner;
        this.started = false;
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        WebMarkupContainer container = initContainer();
        initHeader(container);
        initConfiguration(container);
        initStatus(container);
        initProgress(container);
        initFooter();
    }

    private @NotNull WebMarkupContainer initContainer() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);

        if (started) {
            addTimerBehavior(container);
        }

        add(container);
        return container;
    }

    private void addTimerBehavior(WebMarkupContainer container) {
        if (timerBehavior != null) {
            return;
        }

        timerBehavior = new AjaxSelfUpdatingTimerBehavior(REFRESH_INTERVAL) {

            @Override
            protected void onPostProcessTarget(AjaxRequestTarget target) {
                super.onPostProcessTarget(target);

                TaskExecutionProgress progress = getTaskExecutionProgress();
                if (progress == null) {
                    target.add(getFooter());
                    return;
                }

                if (isTaskOperationCompleted) {
                    stop(target);

                    if (showResultAfterCompletion()) {
                        onShowResults(target);
                    }
                    return;
                }

                OperationResultStatus status = progress.getTaskStatus();

                boolean finished = status != OperationResultStatus.FATAL_ERROR
                        && (progress.isComplete() || status != OperationResultStatus.IN_PROGRESS);

                if (finished) {
                    isTaskOperationCompleted = true;
                }

                target.add(getIconWithLabel());
                target.add(getProgressInfoBox());
                target.add(getProgressBox());
                target.add(getFooter());
            }
        };

        container.add(timerBehavior);
    }

    protected Component getProgressInfoBox() {
        return get(createComponentPath(ID_CONTAINER, ID_PROGRESS_INFO_BOX));
    }

    protected Component getProgressBox() {
        return get(createComponentPath(ID_CONTAINER, ID_PROGRESS_BOX));
    }

    private void initHeader(@NotNull WebMarkupContainer container) {
        IconWithLabel iconTitleLabel = new IconWithLabel(ID_TITLE, titleModel) {

            @Override
            protected @NotNull String getLabelComponentCssClass() {
                return "";
            }

            @Override
            protected @NotNull String getIconCssClass() {
                TaskExecutionProgress progress = getTaskExecutionProgress();

                if (progress == null) {
                    return "";
                }

                OperationResultStatus taskStatus = progress.getTaskStatus();

                if (taskStatus == OperationResultStatus.SUCCESS) {
                    return "fa-solid fa-check-circle text-success me-2 mr-1";
                } else if (taskStatus == OperationResultStatus.FATAL_ERROR) {
                    return "fa-solid fa-xmark-circle text-danger me-2 mr-1";
                } else if (taskStatus == OperationResultStatus.WARNING
                        || taskStatus == OperationResultStatus.PARTIAL_ERROR) {
                    return "fa-solid fa-triangle-exclamation text-warning me-2 mr-1";
                } else {
                    return "";
                }
            }
        };
        iconTitleLabel.setOutputMarkupId(true);

        container.add(iconTitleLabel);
        container.add(new Label(ID_SUBTITLE, subtitleModel));
    }

    private void initConfiguration(@NotNull WebMarkupContainer container) {
        Component configurationPanel = createConfigurationPanel();
        configurationPanel.setOutputMarkupId(true);
        container.add(configurationPanel);
    }

    protected Component createConfigurationPanel() {
        if (taskRunner == null) {
            WebMarkupContainer empty = new WebMarkupContainer(SmartTaskProgressPanel.ID_CONFIGURATION);
            empty.setVisible(false);
            return empty;
        }

        ThreadSelectionPanel panel = new ThreadSelectionPanel(SmartTaskProgressPanel.ID_CONFIGURATION, threadsModel);
        panel.add(new VisibleBehaviour(() -> !started));
        return panel;
    }

    private void initStatus(@NotNull WebMarkupContainer container) {
        BadgePanel badgePanel = new BadgePanel(ID_STATUS_BOX, () -> {

            if (!started) {
                return new Badge(
                        "badge badge-secondary badge-opaque",
                        createStringResource("SmartTaskProgressPanel.status.onHold").getString());
            }

            TaskExecutionProgress progress = getTaskExecutionProgress();

            if (progress == null) {
                return new Badge("", "");
            }

            TaskResultStatus status = progress.getTaskUserFriendlyStatus();
            GuiTaskResultStatus guiStatus = GuiTaskResultStatus.fromTaskResultStatus(status);

            return new Badge(
                    guiStatus.getBadgeState().getCss(),
                    createStringResource(guiStatus.getLabelKey()).getString());
        });

        badgePanel.setOutputMarkupId(true);
        container.add(badgePanel);
    }

    private @NotNull TimerProgressPanel buildTimerComponent() {
        TimerProgressPanel timerProgressPanel = new TimerProgressPanel(
                ID_TIME_LABEL,
                () -> {
                    TaskType task = getTaskObject();
                    return task != null ? task.getLastRunStartTimestamp() : null;
                },
                () -> {
                    TaskType task = getTaskObject();
                    return task != null ? task.getLastRunFinishTimestamp() : null;
                });

        timerProgressPanel.setOutputMarkupId(true);
        return timerProgressPanel;
    }

    private void initProgress(@NotNull WebMarkupContainer container) {
        WebMarkupContainer progressInfoBox = new WebMarkupContainer(ID_PROGRESS_INFO_BOX);
        progressInfoBox.setOutputMarkupId(true);
        progressInfoBox.add(new VisibleBehaviour(this::isProgressVisible));

        Label progressLabel = new Label(ID_PROGRESS_LABEL, this::getProgressText);
        progressLabel.setOutputMarkupId(true);
        progressInfoBox.add(buildTimerComponent());
        progressInfoBox.add(progressLabel);

        container.add(progressInfoBox);

        WebMarkupContainer progressBox = new WebMarkupContainer(ID_PROGRESS_BOX);
        progressBox.setOutputMarkupId(true);
        progressBox.add(new VisibleBehaviour(this::isProgressVisible));
        progressBox.add(createProgressBar());

        container.add(progressBox);
    }

    private @NotNull String getProgressText() {
        TaskExecutionProgress progress = getTaskExecutionProgress();

        if (progress == null) {
            return createStringResource(
                    "SmartTaskProgressPanel.progressLabel",
                    "0").getString();
        }

        String progressLabelValue = progress.getProgressLabel();

        if (progressLabelValue == null || progressLabelValue.isBlank()) {
            progressLabelValue = "0";
        }

        return createStringResource(
                "SmartTaskProgressPanel.progressLabel",
                progressLabelValue).getString();
    }

    private @Nullable TaskExecutionProgress getTaskExecutionProgress() {
        TaskType task = getTaskObject();

        if (task == null) {
            return null;
        }

        TaskInformation info = TaskInformation.createForTask(task, null);
        return TaskExecutionProgress.fromTaskInformation(info, getPageBase());
    }

    @Nullable
    protected TaskType getTaskObject() {
        return taskModel != null ? taskModel.getObject() : null;
    }

    private @NotNull Component createProgressBar() {
        WebMarkupContainer progressBar = new WebMarkupContainer(ID_PROGRESS_BAR);
        progressBar.setOutputMarkupId(true);

        progressBar.add(AttributeModifier.replace("class", () -> {
            TaskExecutionProgress progress = getTaskExecutionProgress();

            if (progress == null) {
                return "progress-bar progress-bar-striped progress-bar-animated";
            }

            OperationResultStatus taskStatus = progress.getTaskStatus();

            return switch (taskStatus) {
                case SUCCESS -> "progress-bar bg-success";
                case FATAL_ERROR -> "progress-bar bg-danger";
                case WARNING, PARTIAL_ERROR -> "progress-bar bg-warning progress-bar-animated";
                default -> "progress-bar progress-bar-striped progress-bar-animated";
            };
        }));

        return progressBar;
    }

    protected Component getProgressBarComponent() {
        return get(createComponentPath(ID_CONTAINER, ID_PROGRESS_BOX, ID_PROGRESS_BAR));
    }

    protected IconWithLabel getIconWithLabel() {
        return (IconWithLabel) get(createComponentPath(ID_CONTAINER, ID_TITLE));
    }

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        footer.setOutputMarkupId(true);

        footer.add(createStopButton());
        footer.add(createStartProcessingButton());
        footer.add(createCloseButton());

        AjaxIconButton navigateToTaskButton = buildViewTaskButton();
        navigateToTaskButton.add(new VisibleBehaviour(this::isTaskActionButtonVisible));
        footer.add(navigateToTaskButton);

        AjaxIconButton showButton = new AjaxIconButton(
                ID_SHOW_RESULT,
                Model.of(""),
                createStringResource("SmartTaskProgressPanel.button.showResults")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onShowResults(target);
            }
        };
        showButton.showTitleAsLabel(true);
        showButton.add(new EnableBehaviour(this::isShowResultsEnable));
        showButton.add(new VisibleBehaviour(this::isTaskActionButtonVisible));
        footer.add(showButton);

        add(footer);
    }

    private @NotNull Component createStopButton() {
        AjaxIconButton button = new AjaxIconButton(
                SmartTaskProgressPanel.ID_STOP,
                Model.of(""),
                getStopButtonLabel()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                stopTask(target);
            }
        };

        button.showTitleAsLabel(true);
        button.add(new VisibleBehaviour(() -> started));
        return button;
    }

    protected void stopTask(AjaxRequestTarget target) {
        TaskType taskObject = getTaskObject();

        if (taskObject == null || taskObject.getOid() == null) {
            return;
        }

        String token = taskObject.getOid();

        Task task = getPageBase().createSimpleTask("SmartTaskProgressPanel.stopTask");
        OperationResult result = task.getResult();

        SmartIntegrationService smartIntegrationService = getPageBase().getSmartIntegrationService();

        try {
            smartIntegrationService.cancelRequest(token, 2000L, task, task.getResult());
        } catch (CommonException e) {
            result.recordFatalError("Couldn't suspend task: " + e.getMessage(), e);
            result.computeStatus();
            getPageBase().showResult(result);
        }

        getPageBase().hideMainPopup(target);
    }

    private @NotNull Component createStartProcessingButton() {
        AjaxIconButton button = new AjaxIconButton(
                SmartTaskProgressPanel.ID_START_PROCESSING,
                Model.of("fa-solid fa-play"),
                createStringResource("SmartTaskProgressPanel.button.run")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onRun(target);
            }
        };

        button.showTitleAsLabel(true);
        button.add(new VisibleBehaviour(() -> !started && taskRunner != null));
        return button;
    }

    private @NotNull Component createCloseButton() {
        AjaxIconButton button = new AjaxIconButton(
                SmartTaskProgressPanel.ID_CANCEL_BUTTON,
                Model.of(""),
                createStringResource("PageBase.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };

        button.showTitleAsLabel(true);
        button.add(new VisibleBehaviour(() -> !started));
        return button;
    }

    private void onRun(AjaxRequestTarget target) {
        IModel<TaskType> model = taskRunner.run(target, threadsModel.getObject());

        if (model == null || model.getObject() == null) {
            target.add(getPageBase().getFeedbackPanel().getParent());
            return;
        }

        taskModel = model;
        started = true;
        isTaskOperationCompleted = false;

        WebMarkupContainer container = (WebMarkupContainer) get(ID_CONTAINER);
        addTimerBehavior(container);

        target.add(this);
        target.add(getFooter());
    }

    private @NotNull AjaxIconButton buildViewTaskButton() {
        AjaxIconButton navigateToTaskButton = new AjaxIconButton(
                ID_NAVIGATE_TO_TASK,
                Model.of(""),
                createStringResource("SmartTaskProgressPanel.button.navigateToTask")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                TaskType modelObject = getTaskObject();

                if (modelObject == null || modelObject.getOid() == null) {
                    return;
                }

                DetailsPageUtil.dispatchToObjectDetailsPage(
                        TaskType.class,
                        modelObject.getOid(),
                        this,
                        false);
            }
        };

        navigateToTaskButton.showTitleAsLabel(true);
        return navigateToTaskButton;
    }

    protected boolean isProgressVisible() {
        return started;
    }

    protected boolean isTaskActionButtonVisible() {
        return started;
    }

    protected boolean showResultAfterCompletion() {
        return false;
    }

    protected IModel<String> getStopButtonLabel() {
        return createStringResource("SmartTaskProgressPanel.button.stop.task");
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    protected boolean isShowResultsEnable() {
        TaskExecutionProgress progress = getTaskExecutionProgress();

        return started
                && progress != null
                && progress.isComplete();
    }

    protected abstract void onShowResults(AjaxRequestTarget target);

    @Override
    protected void onDetach() {
        super.onDetach();

        titleModel.detach();
        subtitleModel.detach();
        threadsModel.detach();

        if (taskModel != null) {
            taskModel.detach();
        }
    }

    @Override
    public int getWidth() {
        return 40;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public IModel<String> getTitle() {
        return Model.of();
    }

    @Override
    public Component getContent() {
        return this;
    }
}
