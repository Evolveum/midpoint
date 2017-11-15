/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import org.apache.commons.lang.ObjectUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;

import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.RESOURCE_OBJECT_OPERATION;
import static com.evolveum.midpoint.web.component.progress.ProgressReportActivityDto.ResourceOperationResult;

/**
 * @author mederly
 */
public class ProgressPanel extends BasePanel<ProgressDto> {

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

    private WebMarkupContainer contentsPanel;
    private StatisticsPanel statisticsPanel;

    private IModel<ProgressReporter> reporterModel;

    //todo move to progress reporter bean probably
    private long operationStartTime;            // if 0, operation hasn't start yet
    private long operationDurationTime;         // if >0, operation has finished

    public ProgressPanel(String id) {
        super(id);

        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        PageBase page = getPageBase();
        reporterModel = new ProgressReporterModel(page);

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
                new AbstractReadOnlyModel<List<ProgressReportActivityDto>>() {

                    @Override
                    public List<ProgressReportActivityDto> getObject() {
                        ProgressDto progressDto = ProgressPanel.this.getModelObject();
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

        ListView logItemsListView = new ListView(ID_LOG_ITEMS, new AbstractReadOnlyModel<List>() {

            @Override
            public List getObject() {
                ProgressDto progressDto = ProgressPanel.this.getModelObject();
                return progressDto.getLogItems();
            }
        }) {

            @Override
            protected void populateItem(ListItem item) {
                item.add(new Label(ID_LOG_ITEM, item.getModel()));
            }
        };
        contentsPanel.add(logItemsListView);

        Label executionTime = new Label(ID_EXECUTION_TIME, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if (operationDurationTime > 0) {
                    return getString("ProgressPanel.ExecutionTimeWhenFinished", operationDurationTime);
                } else if (operationStartTime > 0) {
                    return getString("ProgressPanel.ExecutionTimeWhenRunning",
                            (System.currentTimeMillis() - operationStartTime) / 1000);
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
        item.add(new Label(ID_ACTIVITY_DESCRIPTION, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ProgressReportActivityDto si = item.getModelObject();
                if (si.getActivityType() == RESOURCE_OBJECT_OPERATION && si.getResourceShadowDiscriminator() != null) {
                    ResourceShadowDiscriminator rsd = si.getResourceShadowDiscriminator();
                    return createStringResource(rsd.getKind()).getString()
                            + " (" + rsd.getIntent() + ") on " + si.getResourceName();             // TODO correct i18n
                } else {
                    return createStringResource(si.getActivityType()).getString();
                }
            }
        }));
        item.add(createImageLabel(ID_ACTIVITY_STATE,
                new AbstractReadOnlyModel<String>() {

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
                new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {     // TODO why this does not work???
                        OperationResultStatusType statusType = item.getModelObject().getStatus();       // TODO i18n
                        if (statusType == null) {
                            return null;
                        } else {
                            return statusType.toString();
                        }
                    }
                }
        ));
        item.add(new Label(ID_ACTIVITY_COMMENT, new AbstractReadOnlyModel<String>() {

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
            protected void onSubmit(AjaxRequestTarget target,
                                    org.apache.wicket.markup.html.form.Form<?> form) {
                progressReporter.onAbortSubmit(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target,
                                   org.apache.wicket.markup.html.form.Form<?> form) {
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        configureButton(abortButton);
        progressForm.add(abortButton);

        backButton = new AjaxSubmitButton(ID_BACK,
                createStringResource("pageAdminFocus.button.back")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                backPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target,
                                   org.apache.wicket.markup.html.form.Form<?> form) {
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        configureButton(backButton);
        progressForm.add(backButton);

        continueEditingButton = new AjaxSubmitButton(ID_CONTINUE_EDITING,
                createStringResource("pageAdminFocus.button.continueEditing")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                ProgressReportingAwarePage page = (ProgressReportingAwarePage) getPage();
                page.continueEditing(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target,
                                   org.apache.wicket.markup.html.form.Form<?> form) {
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

    public void recordExecutionStart() {
        operationDurationTime = 0;
        operationStartTime = System.currentTimeMillis();
    }

    public void recordExecutionStop() {
        operationDurationTime = System.currentTimeMillis() - operationStartTime;
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

    public void executeChanges(Collection<ObjectDelta<? extends ObjectType>> deltas, boolean previewOnly,
                               ModelExecuteOptions options, Task task, OperationResult result, AjaxRequestTarget target) {
        //todo implement
    }

    public void clearProgressPanel() {
        getModelObject().clear();
    }

    public boolean isAllSuccess() {
        return getModelObject().allSuccess();
    }

    public ModelContext<? extends ObjectType> getPreviewResult() {
        // todo implement
        return null;
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
        if (refreshingBehavior == null) {       // i.e. refreshing behavior has not been set yet
            LOGGER.info("Creating refreshing behavior");
            LOGGER.debug("Progress reporter {}, panel {}", ProgressReporter.this, ObjectUtils.identityToString(progressPanel));
            refreshingBehavior = new AjaxSelfUpdatingTimerBehavior(Duration.milliseconds(refreshInterval)) {

                @Override
                protected void onPostProcessTarget(AjaxRequestTarget target) {
                    if (progressPanel != null) {
                        progressPanel.invalidateCache();
                    }

                    LOGGER.debug("onPostProcessTarget {} {}", ProgressReporter.this, asyncOperationResult);
                    LOGGER.debug("Progress reporter {}, panel {}", ProgressReporter.this, ObjectUtils.identityToString(progressPanel));

                    if (asyncOperationResult != null) {         // by checking this we know that async operation has been finished
                        asyncOperationResult.recomputeStatus(); // because we set it to in-progress

                        stopRefreshingProgressPanel(target);

                        parentPage.finishProcessing(target, asyncOperationResult, true);
                        asyncOperationResult = null;
                    }
                }

                @Override
                public boolean isEnabled(Component component) {
                    return component != null;
                }
            };
            progressPanel.add(refreshingBehavior);
            target.add(progressPanel);
        }
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
    public void onAbortSubmit(AjaxRequestTarget target) {
        if (progressListener == null) {
            LOGGER.error("No progressListener (abortButton.onSubmit)");
            return;         // should not occur
        }
        progressListener.setAbortRequested(true);
        if (asyncExecutionThread != null) {
            if (asyncExecutionThread.isAlive()) {
                progressPanel.getModelObject().log("Abort requested, please wait...");      // todo i18n
                asyncExecutionThread.interrupt();
            } else {
                progressPanel.getModelObject().log("Abort requested, but the execution seems to be already finished."); // todo i18n
            }
        } else {
            progressPanel.getModelObject().log("Abort requested, please wait... (note: couldn't interrupt the thread)"); // todo i18n
        }
        progressPanel.hideAbortButton(target);
    }

    /**
     * Creates and initializes a progress reporter instance. Should be called during initialization
     * of respective wicket page.
     *
     * @param parentPage The parent page (user, org, role, ...)
     * @param id         Wicket ID of the progress panel
     * @return Progress reporter instance
     */
    public static ProgressReporter create(String id, ProgressReportingAwarePage parentPage) {
        ProgressReporter reporter = new ProgressReporter();
        reporter.progressPanel = new ProgressPanel(id, new Model<>(new ProgressDto()), reporter, parentPage);
        reporter.progressPanel.setOutputMarkupId(true);
        reporter.progressPanel.hide();

        WebApplicationConfiguration config = parentPage.getWebApplicationConfiguration();
        reporter.refreshInterval = config.getProgressRefreshInterval();
        reporter.asynchronousExecution = config.isProgressReportingEnabled();
        reporter.abortEnabled = config.isAbortEnabled();

        reporter.parentPage = parentPage;

        return reporter;
    }

    private static class ProgressReporterModel implements IModel<ProgressReporter> {

        private PageBase page;

        private ProgressReporter reporter;
        private String id;

        public ProgressReporterModel(PageBase page) {
            this.page = page;
        }

        @Override
        public ProgressReporter getObject() {
            if (reporter != null) {
                return reporter;
            }

            ProgressReporterManager manager = page.getProgressReporterManager();
            if (id != null) {
                reporter = manager.getReporter(id);

                return reporter;
            }

            reporter = manager.createReporter();
            id = reporter.getId();

            return reporter;
        }

        @Override
        public void setObject(ProgressReporter object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void detach() {
            reporter = null;
        }
    }
}
