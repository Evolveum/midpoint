/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.page.admin.users.DefaultGuiProgressListener;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

/**
 * Puts together all objects necessary for managing progress reporting and abort functionality.
 * Provides a facade so that this functionality can be easily used from within relevant wicket pages
 * (edit user, org, role, ...).
 *
 * An instance of this class has to be created for each progress reporting case - e.g. at least
 * one for each instance of relevant user/org/role page.
 *
 * @author mederly
 */
public class ProgressReporter implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(ProgressReporter.class);

    // links to wicket artefacts on parent page
    private AjaxSubmitButton abortButton;
    private AjaxSubmitButton backButton;
    private AjaxSubmitButton continueEditingButton;
    private ProgressReportingAwarePage parentPage;
    private ProgressPanel progressPanel;
    private AjaxSelfUpdatingTimerBehavior refreshingBehavior = null;             // behavior is attached to the progress panel

    // items related to asynchronously executed operation
    private OperationResult asyncOperationResult;           // Operation result got from the asynchronous operation (null if async op not yet finished)
    private transient Thread asyncExecutionThread;          // Thread in which async op is executing
    private DefaultGuiProgressListener progressListener;    // Listener created to receive events from the model
                                                // TODO generalize to allow more kinds of GUI progress listeners

    // configuration properties
    private int refreshInterval;
    private boolean asynchronousExecution;
    private boolean abortEnabled;
	private ModelContext<? extends ObjectType> previewResult;		// Temporary - TODO rethink this...

	public ProgressPanel getProgressPanel() {
		return progressPanel;
	}

	/**
     * Creates and initializes a progress reporter instance. Should be called during initialization
     * of respective wicket page.
     *
     * @param parentPage The parent page (user, org, role, ...)
     * @param id Wicket ID of the progress panel
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

    // ===================== Dealing with the SAVE button =======================

    /**
     * Should be called when "save" button is submitted.
     * In future it could encapsulate auxiliary functionality that has to be invoked before starting the operation.
     * Parent page is then responsible for the preparation of the operation and calling the executeChanges method below.
     */
    public void onSaveSubmit() {
    }

    /**
     * Executes changes on behalf of the parent page. By default, changes are executed asynchronously (in
     * a separate thread). However, when set in the midpoint configuration, changes are executed synchronously.
     *
     * @param deltas Deltas to be executed.
     * @param options Model execution options.
     * @param task Task in context of which the changes have to be executed.
     * @param result Operation result.
     * @param target AjaxRequestTarget into which any synchronous changes are signalized.
     */
    public void executeChanges(final Collection<ObjectDelta<? extends ObjectType>> deltas,
			final boolean previewOnly, final ModelExecuteOptions options, final Task task, final OperationResult result, AjaxRequestTarget target) {
        parentPage.startProcessing(target, result);
    	ModelService modelService = parentPage.getModelService();
		ModelInteractionService modelInteractionService = parentPage.getModelInteractionService();
        if (asynchronousExecution) {
            executeChangesAsync(deltas, previewOnly, options, task, result, target, modelService, modelInteractionService);
        } else {
            executeChangesSync(deltas, previewOnly, options, task, result, target, modelService, modelInteractionService);
        }
    }

    private void executeChangesSync(Collection<ObjectDelta<? extends ObjectType>> deltas, boolean previewOnly, ModelExecuteOptions options, Task task,
			OperationResult result, AjaxRequestTarget target, ModelService modelService, ModelInteractionService modelInteractionService) {
        try {
			if (previewOnly) {
				previewResult = modelInteractionService.previewChanges(deltas, options, task, result);
			} else {
				modelService.executeChanges(deltas, options, task, result);
			}
            result.computeStatusIfUnknown();
        } catch (CommunicationException |ObjectAlreadyExistsException |ExpressionEvaluationException |
                PolicyViolationException |SchemaException |SecurityViolationException |
                ConfigurationException |ObjectNotFoundException |RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error executing changes", e);
            if (!result.isFatalError()) {       // just to be sure the exception is recorded into the result
                result.recordFatalError(e.getMessage(), e);
            }
        }
        parentPage.finishProcessing(target, result, false);
    }

    private void executeChangesAsync(final Collection<ObjectDelta<? extends ObjectType>> deltas, final boolean previewOnly,
			final ModelExecuteOptions options, final Task task, final OperationResult result, AjaxRequestTarget target,
			final ModelService modelService, final ModelInteractionService modelInteractionService) {
		final SecurityEnforcer enforcer = parentPage.getSecurityEnforcer();
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		asyncOperationResult = null;

		clearProgressPanel();
		startRefreshingProgressPanel(target);
		showProgressPanel();

		progressPanel.setTask(task);
		progressListener = new DefaultGuiProgressListener(parentPage, progressPanel.getModelObject());
		final HttpConnectionInformation connInfo = SecurityUtil.getCurrentConnectionInformation();
		Runnable execution = () -> {
			try {
				enforcer.storeConnectionInformation(connInfo);
				enforcer.setupPreAuthenticatedSecurityContext(authentication);
				progressPanel.recordExecutionStart();
				if (previewOnly) {
					previewResult = modelInteractionService
							.previewChanges(deltas, options, task, Collections.singleton(progressListener), result);
				} else {
					modelService.executeChanges(deltas, options, task, Collections.singleton(progressListener), result);
				}
			} catch (CommunicationException | ObjectAlreadyExistsException | ExpressionEvaluationException |
					PolicyViolationException | SchemaException | SecurityViolationException |
					ConfigurationException | ObjectNotFoundException | RuntimeException e) {
				LoggingUtils.logUnexpectedException(LOGGER, "Error executing changes", e);
				if (!result.isFatalError()) {       // just to be sure the exception is recorded into the result
					result.recordFatalError(e.getMessage(), e);
				}
			}
			progressPanel.recordExecutionStop();
			asyncOperationResult = result;          // signals that the operation has finished
		};

        if (abortEnabled) {
            showAbortButton(target);
        }
        showBackButton(target);

        result.recordInProgress();              // to disable showing not-final results (why does it work? and why is the result shown otherwise?)

        asyncExecutionThread = new Thread(execution);
        asyncExecutionThread.start();
    }

    private void startRefreshingProgressPanel(AjaxRequestTarget target) {
        if (refreshingBehavior == null) {       // i.e. refreshing behavior has not been set yet
            refreshingBehavior = new AjaxSelfUpdatingTimerBehavior(Duration.milliseconds(refreshInterval)) {

                @Override
                protected void onPostProcessTarget(AjaxRequestTarget target) {
                    super.onPostProcessTarget(target);
                    if (progressPanel != null) {
                        progressPanel.invalidateCache();
                    }
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

    // =================== Dealing with the "Abort" button ========================

    public void registerAbortButton(AjaxSubmitButton abortButton) {
        abortButton.setOutputMarkupId(true);
        abortButton.setOutputMarkupPlaceholderTag(true);
        abortButton.setVisible(false);
        this.abortButton = abortButton;
    }

    public void registerBackButton(AjaxSubmitButton backButton) {
        backButton.setOutputMarkupId(true);
        backButton.setOutputMarkupPlaceholderTag(true);
        backButton.setVisible(false);
        this.backButton = backButton;
    }

    public void registerContinueEditingButton(AjaxSubmitButton continueEditingButton) {
		continueEditingButton.setOutputMarkupId(true);
		continueEditingButton.setOutputMarkupPlaceholderTag(true);
		continueEditingButton.setVisible(false);
        this.continueEditingButton = continueEditingButton;
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
        hideAbortButton(target);
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


    // ================= Other methods =================

    public boolean isAllSuccess() {
        return progressPanel.getModelObject().allSuccess();
    }

    public void showProgressPanel() {
        if (progressPanel != null) {
            progressPanel.show();
        }
    }

    public void hideProgressPanel() {
        if (progressPanel != null) {
            progressPanel.hide();
        }
    }

    public void clearProgressPanel() {
        progressPanel.getModelObject().clear();
    }

	public ModelContext<? extends ObjectType> getPreviewResult() {
		return previewResult;
	}
}
