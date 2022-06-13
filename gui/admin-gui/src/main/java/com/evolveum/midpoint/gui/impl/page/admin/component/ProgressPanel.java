/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AsyncWebProcessModel;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.progress.*;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.List;
import java.util.concurrent.Future;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.RESOURCE_OBJECT_OPERATION;
import static com.evolveum.midpoint.web.component.progress.ProgressReportActivityDto.ResourceOperationResult;

public class ProgressPanel extends BasePanel {

    private static final Trace LOGGER = TraceManager.getTrace(ProgressPanel.class);

    private static final String ID_CONTENTS_PANEL = "contents";
    private static final String ID_ACTIVITIES = "progressReportActivities";
    private static final String ID_ACTIVITY_DESCRIPTION = "description";
    private static final String ID_ACTIVITY_STATE = "status";
    private static final String ID_ACTIVITY_COMMENT = "comment";
    private static final String ID_STATISTICS = "statistics";
    private static final String ID_LOG_ITEMS = "logItems";
    private static final String ID_LOG_ITEM = "logItem";
    private static final String ID_EXECUTION_TIME = "executionTime";
    private static final String ID_PROGRESS_FORM = "progressForm";
    private static final String ID_BACK = "back";
    private static final String ID_ABORT = "abort";
    private static final String ID_CONTINUE_EDITING = "continueEditing";

    private AjaxSelfUpdatingTimerBehavior refreshingBehavior;

    private StatisticsPanel statisticsPanel;

    private AsyncWebProcessModel<ProgressReporter> reporterModel;
    private final ExecuteChangeOptionsDto executeOptions;

    private final ProgressReportingAwarePage progressAwarePage;


    public ProgressPanel(String id, ExecuteChangeOptionsDto options, ProgressReportingAwarePage progressAwarePage) {
        super(id);

        this.progressAwarePage = progressAwarePage;
        this.executeOptions = options;
        setOutputMarkupId(true);
    }

    public AsyncWebProcessModel<ProgressReporter> getReporterModel() {
        return reporterModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        PageBase page = getPageBase();

        WebApplicationConfiguration config = page.getWebApplicationConfiguration();

        ProgressReporter reporter = new ProgressReporter(MidPointApplication.get());
        reporter.setRefreshInterval(config.getProgressRefreshInterval());
        reporter.setAsynchronousExecution(config.isProgressReportingEnabled());
        reporter.setAbortEnabled(config.isAbortEnabled());

        reporterModel = new AsyncWebProcessModel<>(reporter);

        initLayout();
        addRefreshingProgressPanel();
    }

    private void initLayout() {
        MidpointForm progressForm = new MidpointForm<>(ID_PROGRESS_FORM, true);
        add(progressForm);

        WebMarkupContainer contentsPanel = new WebMarkupContainer(ID_CONTENTS_PANEL);
        contentsPanel.setOutputMarkupId(true);
        progressForm.add(contentsPanel);

        ListView<ProgressReportActivityDto> statusItemsListView = new ListView<>(ID_ACTIVITIES,
                (IModel<List<ProgressReportActivityDto>>) () -> {
                    ProgressReporter reporter = reporterModel.getProcessData();
                    ProgressDto progressDto = reporter.getProgress();

                    return progressDto.getProgressReportActivities();
                }) {

            @Override
            protected void populateItem(ListItem<ProgressReportActivityDto> item) {
                populateStatusItem(item);
            }
        };
        contentsPanel.add(statusItemsListView);

        StatisticsDtoModel statisticsModel = new StatisticsDtoModel();
        statisticsPanel = new StatisticsPanel(ID_STATISTICS, statisticsModel);
        contentsPanel.add(statisticsPanel);

        ListView<String> logItemsListView = new ListView<>(ID_LOG_ITEMS, (IModel<List<String>>) () -> {
            ProgressReporter reporter = reporterModel.getProcessData();
            ProgressDto progressDto = reporter.getProgress();

            return progressDto.getLogItems();
        }) {

            @Override
            protected void populateItem(ListItem item) {
                item.add(new Label(ID_LOG_ITEM, item.getModel()));
            }
        };
        contentsPanel.add(logItemsListView);

        Label executionTime = new Label(ID_EXECUTION_TIME, (IModel<String>) () -> {
            ProgressReporter reporter = reporterModel.getProcessData();

            if (reporter.getOperationDurationTime() > 0) {
                return getString("ProgressPanel.ExecutionTimeWhenFinished", reporter.getOperationDurationTime());
            } else if (reporter.getOperationStartTime() > 0) {
                return getString("ProgressPanel.ExecutionTimeWhenRunning",
                        (System.currentTimeMillis() - reporter.getOperationStartTime()) / 1000);
            } else {
                return null;
            }
        });
        contentsPanel.add(executionTime);

        initButtons(progressForm);
    }

    private Label createImageLabel(IModel<String> cssClass, IModel<String> title) {
        Label label = new Label(ProgressPanel.ID_ACTIVITY_STATE);
        label.add(AttributeModifier.replace("class", cssClass));
        label.add(AttributeModifier.replace("title", title));           // does not work, currently

        return label;
    }

    private void populateStatusItem(ListItem<ProgressReportActivityDto> item) {
        item.add(new Label(ID_ACTIVITY_DESCRIPTION, (IModel<String>) () -> {
            ProgressReportActivityDto si = item.getModelObject();
            ProjectionContextKey key = si.getProjectionContextKey();
            if (si.getActivityType() == RESOURCE_OBJECT_OPERATION && key != null) {
                return createStringResource("ProgressPanel.populateStatusItem.resourceObjectActivity",
                        createStringResource(key.getKind()).getString(),
                        key.getIntent(),
                        si.getResourceName()).getString();
            } else {
                return createStringResource(si.getActivityType()).getString();
            }
        }));
        item.add(createImageLabel(
                (IModel<String>) () -> {
                    OperationResultStatusType statusType = item.getModelObject().getStatus();
                    if (statusType == null) {
                        return null;
                    } else {
                        return OperationResultStatusPresentationProperties.parseOperationalResultStatus(statusType).getIcon() + " fa-lg";
                    }
                },
                (IModel<String>) () -> {     // TODO why this does not work???
                    OperationResultStatusType statusType = item.getModelObject().getStatus();
                    if (statusType == null) {
                        return null;
                    } else {
                        return getPageBase().createStringResource(
                                OperationResultStatusPresentationProperties.parseOperationalResultStatus(statusType).getStatusLabelKey()).getString();
                    }
                }
        ));
        item.add(new Label(ID_ACTIVITY_COMMENT, (IModel<String>) () -> {
            ProgressReportActivityDto si = item.getModelObject();
            if (si.getResourceName() != null || si.getResourceOperationResultList() != null) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                if (si.getResourceOperationResultList() != null) {
                    for (ResourceOperationResult ror : si.getResourceOperationResultList()) {
                        if (!first) {
                            sb.append(", ");
                        } else {
                            first = false;
                        }
                        sb.append(createStringResource("ChangeType." + ror.getChangeType()).getString());
                        sb.append(":");
                        sb.append(createStringResource(ror.getResultStatus()).getString());
                    }
                }
                if (si.getResourceObjectName() != null) {
                    if (!first) {
                        sb.append(" -> ");
                    }
                    sb.append(si.getResourceObjectName());
                }
                return sb.toString();
            } else {
                return null;
            }
        }));
    }

    private void configureButton(AjaxSubmitButton btn) {
        btn.setOutputMarkupId(true);
        btn.setOutputMarkupPlaceholderTag(true);
    }

    public void setTask(Task task) {
        if (statisticsPanel != null && statisticsPanel.getModel() instanceof StatisticsDtoModel) {
            ((StatisticsDtoModel) statisticsPanel.getModel()).setTask(task);
        }
    }

    private Boolean abortVisible;
    public void manageButtons(AjaxRequestTarget target, boolean returningFromAsync, boolean canContinueEditing) {
        if (returningFromAsync) {
            showButton(target, ID_BACK);
            abortVisible = false;
//            hideButton(target, ID_ABORT);
            hideButton(target, ID_CONTINUE_EDITING);
        }

        if (canContinueEditing) {
            hideButton(target, ID_BACK);
            showButton(target, ID_CONTINUE_EDITING);
        }
    }

    private void initButtons(final MidpointForm progressForm) {
        AjaxSubmitButton abortButton = new AjaxSubmitButton(ID_ABORT,
                createStringResource("pageAdminFocus.button.abort")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                abortPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        configureButton(abortButton);
        abortButton.add(new VisibleBehaviour(() -> isAbortVisible()));
        progressForm.add(abortButton);

        AjaxSubmitButton backButton = new AjaxSubmitButton(ID_BACK,
                createStringResource("pageAdminFocus.button.back")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                backPerformed();
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        backButton.setVisible(false);
        configureButton(backButton);
        progressForm.add(backButton);

        AjaxSubmitButton continueEditingButton = new AjaxSubmitButton(ID_CONTINUE_EDITING,
                createStringResource("pageAdminFocus.button.continueEditing")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                ProgressReportingAwarePage page = (ProgressReportingAwarePage) getPage();
                page.continueEditing(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        continueEditingButton.setVisible(false);
        configureButton(continueEditingButton);
        progressForm.add(continueEditingButton);
    }

    private boolean isAbortVisible() {
        if (BooleanUtils.isFalse(abortVisible)) {
            return false;
        }
        return reporterModel.getProcessData().isAbortEnabled() && !reporterModel.getProcessData().isAbortRequested();
    }

    protected void backPerformed() {
        PageBase page = getPageBase();
        page.redirectBack();
    }

    /**
     * Should be called when "save" button is submitted.
     * In future it could encapsulate auxiliary functionality that has to be invoked before starting the operation.
     * Parent page is then responsible for the preparation of the operation and calling the executeChanges method below.
     */
    public void onBeforeSave() {
        //todo implement
    }

    public boolean isAllSuccess() {
        ProgressReporter reporter = reporterModel.getProcessData();
        return reporter.getProgress().allSuccess();
    }

    public ModelContext<? extends ObjectType> getPreviewResult() {
        ProgressReporter reporter = reporterModel.getProcessData();
        return reporter.getPreviewResult();
    }

    private void showButton(AjaxRequestTarget target, String buttonId) {
        setButtonVisibility(target, buttonId, true);
    }

    private void hideButton(AjaxRequestTarget target, String buttonId) {
        setButtonVisibility(target, buttonId, false);
    }

    private void setButtonVisibility(AjaxRequestTarget target, String buttonId, boolean visible) {
        AjaxSubmitButton backButton = getButton(buttonId);
        backButton.setVisible(visible);
        target.add(backButton);
    }

    private AjaxSubmitButton getButton(String buttonId) {
        return (AjaxSubmitButton) get(createComponentPath(ID_PROGRESS_FORM, buttonId));
    }

    public boolean isKeepDisplayingResults() {
        return executeOptions.isKeepDisplayingResults();
    }

    public ExecuteChangeOptionsDto getExecuteOptions() {
        return executeOptions;
    }
    // mess

    public void addRefreshingProgressPanel() {
        if (refreshingBehavior != null) {
            return;
        }

        ProgressReporter reporter = reporterModel.getProcessData();
        int refreshInterval = reporter.getRefreshInterval();

        refreshingBehavior = new AjaxSelfUpdatingTimerBehavior(java.time.Duration.ofMillis(refreshInterval)) {

            @Override
            protected void onPostProcessTarget(AjaxRequestTarget target) {
                invalidateCache();

                ProgressReporter reporter = reporterModel.getProcessData();

                OperationResult asyncOperationResult = reporter.getAsyncOperationResult();
                if (asyncOperationResult != null) {         // by checking this we know that async operation has been finished
                    asyncOperationResult.recomputeStatus(); // because we set it to in-progress

                    PageBase page = getPageBase();
                    if (reporter.isAbortRequested()) {
                        page.showResult(asyncOperationResult);
                        target.add(page.getFeedbackPanel());
                        return;
                    }

                    progressAwarePage.finishProcessing(target, true, asyncOperationResult);

                    stopRefreshingProgressPanel(target);
                    reporter.setAsyncOperationResult(null);
                }
            }

            @Override
            public boolean isEnabled(Component component) {
                return component != null;
            }
        };

        add(refreshingBehavior);
    }

    private void stopRefreshingProgressPanel(AjaxRequestTarget target) {
        if (refreshingBehavior != null) {
            refreshingBehavior.stop(target);
            // We cannot remove the behavior, as it would cause NPE because of component == null (since wicket 7.5)
            //progressPanel.remove(refreshingBehavior);
            refreshingBehavior = null;              // causes re-adding this behavior when re-saving changes
        }
    }

    public void invalidateCache() {
        if (statisticsPanel != null && statisticsPanel.getModel() instanceof StatisticsDtoModel) {
            ((StatisticsDtoModel) (statisticsPanel.getModel())).invalidateCache();
        }
    }

    /**
     * You have to call this method when Abort button is pressed
     */
    public void abortPerformed(AjaxRequestTarget target) {
        ProgressReporter reporter = reporterModel.getProcessData();

        if (reporter == null) {
            LOGGER.error("No reporter/progressListener (abortButton.onSubmit)");
            return;         // should not occur
        }

        reporter.setAbortRequested(true);

        Future future = reporterModel.getObject().getFuture();
        if (future != null) {
            if (!future.isDone()) {
                reporter.getProgress().log(getString("ProgressPanel.abortRequested"));
                future.cancel(true);
            } else {
                reporter.getProgress().log(getString("ProgressPanel.abortRequestedFinished"));
            }
        } else {
            reporter.getProgress().log("ProgressPanel.abortRequestedNoInterrupt");
        }

        hideButton(target, ID_ABORT);
    }

}
