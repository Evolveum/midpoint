/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AsyncWebProcessManager;
import com.evolveum.midpoint.web.application.AsyncWebProcessModel;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.SecurityContextAwareCallable;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.time.Duration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.RESOURCE_OBJECT_OPERATION;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.web.component.progress.ProgressReportActivityDto.ResourceOperationResult;

/**
 * @author mederly
 */
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

    private AjaxSubmitButton abortButton;
    private AjaxSubmitButton backButton;
    private AjaxSubmitButton continueEditingButton;

    private AjaxSelfUpdatingTimerBehavior refreshingBehavior;

    private WebMarkupContainer contentsPanel;
    private StatisticsPanel statisticsPanel;

    private AsyncWebProcessModel<ProgressReporter> reporterModel;

    public ProgressPanel(String id) {
        super(id);

        setOutputMarkupId(true);
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

        hide();
    }

    private void initLayout() {
        Form progressForm = new Form<>(ID_PROGRESS_FORM, true);
        add(progressForm);

        contentsPanel = new WebMarkupContainer(ID_CONTENTS_PANEL);
        contentsPanel.setOutputMarkupId(true);
        progressForm.add(contentsPanel);

        ListView statusItemsListView = new ListView<ProgressReportActivityDto>(ID_ACTIVITIES,
                new IModel<List<ProgressReportActivityDto>>() {

                    @Override
                    public List<ProgressReportActivityDto> getObject() {
                        ProgressReporter reporter = reporterModel.getProcessData();
                        ProgressDto progressDto = reporter.getProgress();

                        return progressDto.getProgressReportActivities();
                    }
                }) {

            @Override
            protected void populateItem(ListItem<ProgressReportActivityDto> item) {
                populateStatusItem(item);
            }
        };
        contentsPanel.add(statusItemsListView);

        statisticsPanel = new StatisticsPanel(ID_STATISTICS, new StatisticsDtoModel());
        contentsPanel.add(statisticsPanel);

        ListView logItemsListView = new ListView(ID_LOG_ITEMS, new IModel<List>() {

            @Override
            public List getObject() {
                ProgressReporter reporter = reporterModel.getProcessData();
                ProgressDto progressDto = reporter.getProgress();

                return progressDto.getLogItems();
            }
        }) {

            @Override
            protected void populateItem(ListItem item) {
                item.add(new Label(ID_LOG_ITEM, item.getModel()));
            }
        };
        contentsPanel.add(logItemsListView);

        Label executionTime = new Label(ID_EXECUTION_TIME, new IModel<String>() {

            @Override
            public String getObject() {
                ProgressReporter reporter = reporterModel.getProcessData();

                if (reporter.getOperationDurationTime() > 0) {
                    return getString("ProgressPanel.ExecutionTimeWhenFinished", reporter.getOperationDurationTime());
                } else if (reporter.getOperationStartTime() > 0) {
                    return getString("ProgressPanel.ExecutionTimeWhenRunning",
                            (System.currentTimeMillis() - reporter.getOperationStartTime()) / 1000);
                } else {
                    return null;
                }
            }
        });
        contentsPanel.add(executionTime);

        initButtons(progressForm);
    }

    private Label createImageLabel(String id, IModel<String> cssClass, IModel<String> title) {
        Label label = new Label(id);
        label.add(AttributeModifier.replace("class", cssClass));
        label.add(AttributeModifier.replace("title", title));           // does not work, currently

        return label;
    }

    private void populateStatusItem(ListItem<ProgressReportActivityDto> item) {
        item.add(new Label(ID_ACTIVITY_DESCRIPTION, new IModel<String>() {

            @Override
            public String getObject() {
                ProgressReportActivityDto si = item.getModelObject();
                if (si.getActivityType() == RESOURCE_OBJECT_OPERATION && si.getResourceShadowDiscriminator() != null) {
                    ResourceShadowDiscriminator rsd = si.getResourceShadowDiscriminator();
                    return createStringResource("ProgressPanel.populateStatusItem.resourceObjectActivity", createStringResource(rsd.getKind()).getString(),
                            rsd.getIntent(),si.getResourceName()).getString();
                } else {
                    return createStringResource(si.getActivityType()).getString();
                }
            }
        }));
        item.add(createImageLabel(ID_ACTIVITY_STATE,
                new IModel<String>() {

                    @Override
                    public String getObject() {
                        OperationResultStatusType statusType = item.getModelObject().getStatus();
                        if (statusType == null) {
                            return null;
                        } else {
                            return OperationResultStatusPresentationProperties.parseOperationalResultStatus(statusType).getIcon() + " fa-lg";
                        }
                    }
                },
                new IModel<String>() {

                    @Override
                    public String getObject() {     // TODO why this does not work???
                        OperationResultStatusType statusType = item.getModelObject().getStatus();
                        if (statusType == null) {
                            return null;
                        } else {
                            return getPageBase().createStringResource(
                                    OperationResultStatusPresentationProperties.parseOperationalResultStatus(statusType).getStatusLabelKey()).getString();
                        }
                    }
                }
        ));
        item.add(new Label(ID_ACTIVITY_COMMENT, new IModel<String>() {

            @Override
            public String getObject() {
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
            }
        }));
    }

    private void configureButton(AjaxSubmitButton btn) {
        btn.setVisible(false);
        btn.setOutputMarkupId(true);
        btn.setOutputMarkupPlaceholderTag(true);
    }

    private void initButtons(final Form progressForm) {
        abortButton = new AjaxSubmitButton(ID_ABORT,
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
        progressForm.add(abortButton);

        backButton = new AjaxSubmitButton(ID_BACK,
                createStringResource("pageAdminFocus.button.back")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                backPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        configureButton(backButton);
        progressForm.add(backButton);

        continueEditingButton = new AjaxSubmitButton(ID_CONTINUE_EDITING,
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
        configureButton(continueEditingButton);
        progressForm.add(continueEditingButton);
    }

    protected void backPerformed(AjaxRequestTarget target) {
        PageBase page = getPageBase();
        page.redirectBack();
    }

    // Note: do not setVisible(false) on the progress panel itself - it will disable AJAX refresh functionality attached to it.
    // Use the following two methods instead.

    public void show() {
        contentsPanel.setVisible(true);
    }

    public void hide() {
        contentsPanel.setVisible(false);
    }

    public void setTask(Task task) {
        if (statisticsPanel != null && statisticsPanel.getModel() instanceof StatisticsDtoModel) {
            ((StatisticsDtoModel) statisticsPanel.getModel()).setTask(task);
        }
    }

    public void invalidateCache() {
        if (statisticsPanel != null && statisticsPanel.getModel() instanceof StatisticsDtoModel) {
            ((StatisticsDtoModel) (statisticsPanel.getModel())).invalidateCache();
        }
    }

    /**
     * Should be called when "save" button is submitted.
     * In future it could encapsulate auxiliary functionality that has to be invoked before starting the operation.
     * Parent page is then responsible for the preparation of the operation and calling the executeChanges method below.
     */
    public void onBeforeSave() {
        //todo implement
    }

    /**
     * Executes changes on behalf of the parent page. By default, changes are executed asynchronously (in
     * a separate thread). However, when set in the midpoint configuration, changes are executed synchronously.
     *
     * @param deltas  Deltas to be executed.
     * @param options Model execution options.
     * @param task    Task in context of which the changes have to be executed.
     * @param result  Operation result.
     */
    public void executeChanges(Collection<ObjectDelta<? extends ObjectType>> deltas, boolean previewOnly,
                               ModelExecuteOptions options, Task task, OperationResult result, AjaxRequestTarget target) {

        PageBase page = getPageBase();

        ProgressReporter reporter = reporterModel.getProcessData();

        if (page instanceof ProgressReportingAwarePage) {
            ProgressReportingAwarePage aware = (ProgressReportingAwarePage) page;
            aware.startProcessing(target, result);
        }

        if (reporter.isAsynchronousExecution()) {
            reporter.setAsyncOperationResult(null);

            clearProgressPanel();
            startRefreshingProgressPanel(target);
            show();

            if (reporter.isAbortEnabled()) {
                showAbortButton(target);
            }
            showBackButton(target);

            setTask(task);

            executeChangesAsync(reporter, deltas, previewOnly, options, task, result);
        } else {
            executeChangesSync(reporter, deltas, previewOnly, options, task, result);
        }

        if (!reporter.isAsynchronousExecution() && page instanceof ProgressReportingAwarePage) {
            ProgressReportingAwarePage aware = (ProgressReportingAwarePage) page;
            aware.finishProcessing(target, reporter.getObjectDeltaOperation(), reporter.isAsynchronousExecution(), result);
        }
    }

    public void executeChangesInBackground(Collection<ObjectDelta<? extends ObjectType>> deltas, boolean previewOnly,
                               ModelExecuteOptions options, Task task, OperationResult result, AjaxRequestTarget target) {
        PageBase page = getPageBase();
        ProgressReporter reporter = reporterModel.getProcessData();
        try {
            TaskManager taskManager = page.getTaskManager();
            MidPointPrincipal user = SecurityUtils.getPrincipalUser();
            if (user == null) {
                throw new RestartResponseException(PageLogin.class);
            } else {
                task.setOwner(user.getFocus().asPrismObject());
            }

            List<ObjectDeltaType> deltasBeans = new ArrayList<>();
            for (ObjectDelta<?> delta : deltas) {
                deltasBeans.add(DeltaConvertor.toObjectDeltaType((ObjectDelta<? extends com.evolveum.prism.xml.ns._public.types_3.ObjectType>) delta));
            }
            PrismPropertyDefinition<ObjectDeltaType> deltasDefinition = page.getPrismContext().getSchemaRegistry()
                    .findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTAS);
            PrismProperty<ObjectDeltaType> deltasProperty = deltasDefinition.instantiate();
            deltasProperty.setRealValues(deltasBeans.toArray(new ObjectDeltaType[0]));
            task.addExtensionProperty(deltasProperty);
            if (options != null) {
                PrismPropertyDefinition<ModelExecuteOptionsType> optionsDefinition = page.getPrismContext().getSchemaRegistry()
                        .findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_EXECUTE_OPTIONS);
                PrismProperty<ModelExecuteOptionsType> optionsProperty = optionsDefinition.instantiate();
                optionsProperty.setRealValue(options.toModelExecutionOptionsType());
                task.addExtensionProperty(optionsProperty);
            }
            task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);
            task.setHandlerUri(ModelPublicConstants.EXECUTE_DELTAS_TASK_HANDLER_URI);
            task.setName("Execute changes");
            task.setInitialExecutionStatus(TaskExecutionStatus.RUNNABLE);

            PrismObject<TaskType> taskType = task.getUpdatedTaskObject();
            AssignmentType archetypeAssignment = new AssignmentType();
            archetypeAssignment.setTargetRef(ObjectTypeUtil.createObjectRef(SystemObjectsType.ARCHETYPE_UTILITY_TASK.value(),
                    ObjectTypes.ARCHETYPE));
            taskType.asObjectable().getAssignment().add(archetypeAssignment);
            taskType.asObjectable().getArchetypeRef().add(ObjectTypeUtil.createObjectRef(SystemObjectsType.ARCHETYPE_UTILITY_TASK.value(),
                    ObjectTypes.ARCHETYPE));

            taskManager.switchToBackground(task, result);
            result.setBackgroundTaskOid(task.getOid());
        } catch (Exception e) {
            result.recordFatalError(e);
        } finally {
            result.computeStatusIfUnknown();
        }
        if (page instanceof ProgressReportingAwarePage) {
            ProgressReportingAwarePage aware = (ProgressReportingAwarePage) page;
            aware.finishProcessing(target, reporter.getObjectDeltaOperation(), reporter.isAsynchronousExecution(), result);
        }
    }

    public void clearProgressPanel() {
        ProgressReporter reporter = reporterModel.getProcessData();
        reporter.getProgress().clear();
    }

    public boolean isAllSuccess() {
        ProgressReporter reporter = reporterModel.getProcessData();
        return reporter.getProgress().allSuccess();
    }

    public ModelContext<? extends ObjectType> getPreviewResult() {
        ProgressReporter reporter = reporterModel.getProcessData();
        return reporter.getPreviewResult();
    }

    public void hideAbortButton(AjaxRequestTarget target) {
        abortButton.setVisible(false);
        target.add(abortButton);
    }

    public void showAbortButton(AjaxRequestTarget target) {
        abortButton.setVisible(true);
        target.add(abortButton);
    }

    public void hideBackButton(AjaxRequestTarget target) {
        backButton.setVisible(false);
        target.add(backButton);
    }

    public void hideContinueEditingButton(AjaxRequestTarget target) {
        continueEditingButton.setVisible(false);
        target.add(continueEditingButton);
    }

    public void showBackButton(AjaxRequestTarget target) {
        backButton.setVisible(true);
        target.add(backButton);
    }

    public void showContinueEditingButton(AjaxRequestTarget target) {
        continueEditingButton.setVisible(true);
        target.add(continueEditingButton);
    }

    // mess

    private void startRefreshingProgressPanel(AjaxRequestTarget target) {
        if (refreshingBehavior != null) {
            return;
        }

        ProgressReporter reporter = reporterModel.getProcessData();
        int refreshInterval = reporter.getRefreshInterval();

        refreshingBehavior = new AjaxSelfUpdatingTimerBehavior(Duration.milliseconds(refreshInterval)) {

            @Override
            protected void onPostProcessTarget(AjaxRequestTarget target) {
                invalidateCache();

                ProgressReporter reporter = reporterModel.getProcessData();

                OperationResult asyncOperationResult = reporter.getAsyncOperationResult();
                if (asyncOperationResult != null) {         // by checking this we know that async operation has been finished
                    asyncOperationResult.recomputeStatus(); // because we set it to in-progress

                    stopRefreshingProgressPanel(target);

                    PageBase page = getPageBase();
                    if (reporter.isAbortRequested()) {
                        page.showResult(asyncOperationResult);
                        target.add(page.getFeedbackPanel());
                        return;
                    }

                    if (page instanceof ProgressReportingAwarePage) {
                        ProgressReportingAwarePage aware = (ProgressReportingAwarePage) page;
                        aware.finishProcessing(target, reporter.getObjectDeltaOperation() , true, asyncOperationResult);
                    }

                    reporter.setAsyncOperationResult(null);
                }
            }

            @Override
            public boolean isEnabled(Component component) {
                return component != null;
            }
        };

        add(refreshingBehavior);

        target.add(this);
    }

    private void stopRefreshingProgressPanel(AjaxRequestTarget target) {
        if (refreshingBehavior != null) {
            refreshingBehavior.stop(target);
            // We cannot remove the behavior, as it would cause NPE because of component == null (since wicket 7.5)
            //progressPanel.remove(refreshingBehavior);
            refreshingBehavior = null;              // causes re-adding this behavior when re-saving changes
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

        hideAbortButton(target);
    }

    private void executeChangesSync(ProgressReporter reporter, Collection<ObjectDelta<? extends ObjectType>> deltas,
                                    boolean previewOnly, ModelExecuteOptions options, Task task, OperationResult result) {

        try {
            MidPointApplication application = MidPointApplication.get();

            if (previewOnly) {
                ModelInteractionService service = application.getModelInteractionService();
                ModelContext previewResult = service.previewChanges(deltas, options, task, result);
                reporter.setPreviewResult(previewResult);
            } else {
                ModelService service = application.getModel();
                Collection<ObjectDeltaOperation<? extends ObjectType>>executedDeltas = service.executeChanges(deltas, options, task, result);
                reporter.setObjectDeltaOperation(executedDeltas);
            }
            result.computeStatusIfUnknown();
        } catch (CommonException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error executing changes", e);
            if (!result.isFatalError()) {       // just to be sure the exception is recorded into the result
                result.recordFatalError(e.getMessage(), e);
            }
        }
    }

    private void executeChangesAsync(ProgressReporter reporter, Collection<ObjectDelta<? extends ObjectType>> deltas,
                                     boolean previewOnly, ModelExecuteOptions options, Task task, OperationResult result) {

        MidPointApplication application = MidPointApplication.get();

        final ModelInteractionService modelInteraction = application.getModelInteractionService();
        final ModelService model = application.getModel();

        final SecurityContextManager secManager = application.getSecurityContextManager();

        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final HttpConnectionInformation connInfo = SecurityUtil.getCurrentConnectionInformation();

        Callable<Void> execution = new SecurityContextAwareCallable<Void>(secManager, auth, connInfo) {

            @Override
            public Void callWithContextPrepared() throws Exception {
                try {
                    LOGGER.debug("Execution start");

                    reporter.recordExecutionStart();

                    if (previewOnly) {
                        ModelContext previewResult = modelInteraction
                                .previewChanges(deltas, options, task, Collections.singleton(reporter), result);
                        reporter.setPreviewResult(previewResult);
                    } else if (deltas != null && deltas.size() > 0){
                        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = model.executeChanges(deltas, options, task, Collections.singleton(reporter), result);
                        reporter.setObjectDeltaOperation(executedDeltas);
                    }
                } catch (CommonException | RuntimeException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Error executing changes", e);
                    if (!result.isFatalError()) {       // just to be sure the exception is recorded into the result
                        result.recordFatalError(e.getMessage(), e);
                    }
                } finally {
                    LOGGER.debug("Execution finish {}", result);
                }
                reporter.recordExecutionStop();
                reporter.setAsyncOperationResult(result);          // signals that the operation has finished

                return null;
            }
        };

        result.recordInProgress(); // to disable showing not-final results (why does it work? and why is the result shown otherwise?)

        AsyncWebProcessManager manager = application.getAsyncWebProcessManager();
        manager.submit(reporterModel.getId(), execution);
    }
}
