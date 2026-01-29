/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.component;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationPage;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.page.PageSimulationResult;
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
import com.evolveum.midpoint.web.page.admin.server.TaskExecutionProgress;
import com.evolveum.midpoint.web.page.admin.server.dto.GuiTaskResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Duration;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.formatElapsedTime;
import static com.evolveum.midpoint.gui.impl.page.admin.simulation.wizard.ResourceSimulationTaskWizardPanel.getSimulationResultReference;

/**
 * Popup panel that displays progress of a running simulation task.
 *
 * <p>Shows a title/subtitle, current task status, elapsed time, and progress information.
 * The panel periodically refreshes itself and updates footer buttons (stop / show results).
 * When the task is finished, the auto-refresh is stopped and the "Show results" action becomes enabled.</p>
 *
 * <p>The "Stop simulation" action requests cancellation of the underlying task via {@link SmartIntegrationService}.
 * The "See results" action navigates to {@link PageSimulationResult} using the simulation result reference
 * obtained from the task.</p>
 */
public class SimulationProgressPanel extends BasePanel<TaskType> implements Popupable {

    private static final String ID_CONTAINER = "container";

    private static final String ID_TITLE = "title";
    private static final String ID_SUBTITLE = "subtitle";

    private static final String ID_STATUS_BOX = "statusBox";
    private static final String ID_TIME_LABEL = "timeLabel";
    private static final String ID_PROGRESS_LABEL = "progressLabel";
    private static final String ID_PROGRESS_BAR = "progressBar";

    private static final String ID_BUTTONS = "buttons";
    private static final String ID_STOP = "stop";
    private static final String ID_SHOW_RESULT = "showResult";

    private static final Duration REFRESH_INTERVAL = Duration.ofSeconds(2);

    private final IModel<String> titleModel;
    private final IModel<String> subtitleModel;

    private Fragment footer;

    public SimulationProgressPanel(
            String id,
            IModel<String> titleModel,
            IModel<String> subtitleModel,
            IModel<TaskType> taskModel) {
        super(id, taskModel);
        this.titleModel = titleModel;
        this.subtitleModel = subtitleModel;
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        WebMarkupContainer container = initContainer();
        initHeader(container);
        initStatus(container);
        initProgress(container);

        initFooter();
    }

    private @NotNull WebMarkupContainer initContainer() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        container.add(new AjaxSelfUpdatingTimerBehavior(REFRESH_INTERVAL) {
            @Override
            protected void onPostProcessTarget(AjaxRequestTarget target) {
                super.onPostProcessTarget(target);
                if (getTaskExecutionProgress().isComplete()
                        || getTaskExecutionProgress().getTaskStatus() != OperationResultStatus.IN_PROGRESS) {
                    stop(target);
                }
                target.add(getFooter());
            }
        });
        add(container);
        return container;
    }

    private void initHeader(@NotNull WebMarkupContainer container) {
        container.add(new Label(ID_TITLE, titleModel));
        container.add(new Label(ID_SUBTITLE, subtitleModel));
    }

    private void initStatus(@NotNull WebMarkupContainer container) {
        BadgePanel badgePanel = new BadgePanel(ID_STATUS_BOX, () -> {
            TaskResultStatus status = getTaskExecutionProgress().getTaskUserFriendlyStatus();
            GuiTaskResultStatus guiStatus = GuiTaskResultStatus.fromTaskResultStatus(status);
            return new Badge(guiStatus.getBadgeState().getCss(), createStringResource(guiStatus.getLabelKey()).getString());
        });
        badgePanel.setOutputMarkupId(true);
        container.add(badgePanel);

        Label timeLabel = new Label(ID_TIME_LABEL, this::getElapsedTimeText);
        timeLabel.setOutputMarkupId(true);
        container.add(timeLabel);
    }

    private void initProgress(@NotNull WebMarkupContainer container) {
        Label progressLabel = new Label(ID_PROGRESS_LABEL, this::getProgressText);
        progressLabel.setOutputMarkupId(true);
        container.add(progressLabel);

        container.add(createProgressBar());
    }

    private @NotNull String getElapsedTimeText() {
        TaskType task = getModelObject();
        if (task == null) {
            return "";
        }

        XMLGregorianCalendar startTs = task.getLastRunStartTimestamp();
        if (startTs == null) {
            return "";
        }

        long startMillis = startTs.toGregorianCalendar().getTimeInMillis();

        XMLGregorianCalendar finishTs = task.getLastRunFinishTimestamp();
        long endMillis = finishTs != null
                ? finishTs.toGregorianCalendar().getTimeInMillis()
                : System.currentTimeMillis();

        return formatElapsedTime(startMillis, endMillis,
                createStringResource("SimulationProgressPanel.runningFor").getString());
    }

    private @NotNull String getProgressText() {
        String progressLabelValue = getTaskExecutionProgress().getProgressLabel();
        if (progressLabelValue == null || progressLabelValue.isBlank()) {
            progressLabelValue = "0";
        }

        return createStringResource(
                "SimulationProgressPanel.progressLabel",
                progressLabelValue).getString();
    }

    private @NotNull TaskExecutionProgress getTaskExecutionProgress() {
        TaskInformation info = TaskInformation.createForTask(getModelObject(), null);
        return TaskExecutionProgress.fromTaskInformation(info, getPageBase());
    }

    private @NotNull Component createProgressBar() {
        WebMarkupContainer progressBar = new WebMarkupContainer(SimulationProgressPanel.ID_PROGRESS_BAR);
        progressBar.setOutputMarkupId(true);
        return progressBar;
    }

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        footer.setOutputMarkupId(true);

        AjaxIconButton stopButton = new AjaxIconButton(ID_STOP, Model.of(""),
                createStringResource("SimulationProgressPanel.button.stopSimulation")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onStop(target);
            }
        };
        stopButton.showTitleAsLabel(true);
        footer.add(stopButton);

        AjaxIconButton showButton = new AjaxIconButton(
                ID_SHOW_RESULT,
                Model.of(""),
                createStringResource("SimulationProgressPanel.button.showResults")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onShowResults();
            }
        };
        showButton.showTitleAsLabel(true);
        showButton.add(new EnableBehaviour(this::isShowResultsEnable));
        footer.add(showButton);

        add(footer);
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    protected boolean isShowResultsEnable() {
        return getTaskExecutionProgress().isComplete();
    }

    protected void onStop(AjaxRequestTarget target) {
        String token = getModelObject().getOid();
        Task task = getPageBase().createSimpleTask("SimulationProgressPanel.onStop");
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

    protected void onShowResults() {
        ObjectReferenceType simulationResultReference = getSimulationResultReference(getModelObject());
        PageParameters params = new PageParameters();
        if (simulationResultReference != null) {
            params.set(SimulationPage.PAGE_PARAMETER_RESULT_OID, simulationResultReference.getOid());
            getPageBase().navigateToNext(PageSimulationResult.class, params);
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
