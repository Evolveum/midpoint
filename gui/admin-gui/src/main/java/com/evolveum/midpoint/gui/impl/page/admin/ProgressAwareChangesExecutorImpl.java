/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;

import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.NotLoggedInException;
import com.evolveum.midpoint.web.application.AsyncWebProcessManager;
import com.evolveum.midpoint.web.application.AsyncWebProcessModel;
import com.evolveum.midpoint.web.component.SecurityContextAwareCallable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.impl.page.admin.component.ProgressPanel;
import com.evolveum.midpoint.gui.impl.page.admin.component.ProgressReportingAwarePage;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.progress.ProgressReporter;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.security.MidPointApplication;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.restartOnLoginPageException;
import static com.evolveum.midpoint.model.api.ModelExecuteOptions.toModelExecutionOptionsBean;
import static com.evolveum.midpoint.schema.util.task.work.SpecificWorkDefinitionUtil.createExplicitChangeExecutionDef;

public class ProgressAwareChangesExecutorImpl implements ObjectChangeExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(ProgressAwareChangesExecutorImpl.class);

    private final ExecuteChangeOptionsDto executeOptions;
    private final ProgressReportingAwarePage progressAwarePage;

    public ProgressAwareChangesExecutorImpl(ExecuteChangeOptionsDto options, ProgressReportingAwarePage progressAwarePage) {
        this.executeOptions = options;
        this.progressAwarePage = progressAwarePage;
    }

    public Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges(Collection<ObjectDelta<? extends ObjectType>> deltas, boolean previewOnly, Task task, OperationResult result, AjaxRequestTarget target) {
        ModelExecuteOptions options = createOptions(executeOptions, previewOnly);
        LOGGER.debug("Using execute options {}.", options);
        if (executeOptions.isSaveInBackground() && !previewOnly) {
            return executeChangesInBackground(deltas, options, task, result, target);
        }

        return executeChanges(deltas, previewOnly, options, task, result, target);
    }

    private ModelExecuteOptions createOptions(ExecuteChangeOptionsDto executeChangeOptionsDto, boolean previewOnly) {
        ModelExecuteOptions options = executeChangeOptionsDto.createOptions(PrismContext.get());
        if (previewOnly) {
            options.getOrCreatePartialProcessing().setApprovals(PartialProcessingTypeType.PROCESS);
        }
        return options;
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
    public Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges(Collection<ObjectDelta<? extends ObjectType>> deltas, boolean previewOnly,
            ModelExecuteOptions options, Task task, OperationResult result, AjaxRequestTarget target) {

        ProgressPanel progressPanel = progressAwarePage.startAndGetProgressPanel(target, result);

        ProgressReporter reporter = progressPanel.getReporterModel().getProcessData();
        if (reporter.isAsynchronousExecution()) {
            reporter.setAsyncOperationResult(null);

            progressPanel.setTask(task);

            executeChangesAsync(progressPanel, deltas, previewOnly, options, task, result);
        } else {
            executeChangesSync(reporter, deltas, previewOnly, options, task, result);
        }

        if (!reporter.isAsynchronousExecution()) {
            progressAwarePage.finishProcessing(target, reporter.isAsynchronousExecution(), result);
        }

        return reporter.getObjectDeltaOperation();
    }

    private void executeChangesAsync(ProgressPanel progressPanel, Collection<ObjectDelta<? extends ObjectType>> deltas,
            boolean previewOnly, ModelExecuteOptions options, Task task, OperationResult result) {

        MidPointApplication application = MidPointApplication.get();

        final ModelInteractionService modelInteraction = application.getModelInteractionService();
        final ModelService model = application.getModel();

        final SecurityContextManager secManager = application.getSecurityContextManager();

        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final HttpConnectionInformation connInfo = SecurityUtil.getCurrentConnectionInformation();

        AsyncWebProcessModel<ProgressReporter> reporterModel = progressPanel.getReporterModel();

        Callable<Void> execution = new SecurityContextAwareCallable<>(secManager, auth, connInfo) {

            @Override
            public Void callWithContextPrepared() {
                ProgressReporter reporter = reporterModel.getProcessData();
                try {
                    LOGGER.debug("Execution start");

                    reporter.recordExecutionStart();

                    if (previewOnly) {
                        ModelContext previewResult = modelInteraction
                                .previewChanges(deltas, options, task, Collections.singleton(reporter), result);
                        reporter.setPreviewResult(previewResult);
                    } else if (deltas != null && deltas.size() > 0) {
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

        result.setInProgress(); // to disable showing not-final results (why does it work? and why is the result shown otherwise?)

        AsyncWebProcessManager manager = application.getAsyncWebProcessManager();
        manager.submit(reporterModel.getId(), execution);

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

    private Collection<ObjectDeltaOperation<? extends ObjectType>> executeChangesInBackground(
            Collection<ObjectDelta<? extends ObjectType>> deltas,
            ModelExecuteOptions options, Task task, OperationResult result, AjaxRequestTarget target) {

        try {

            progressAwarePage.getModelInteractionService().submit(
                    createExplicitChangeExecutionDef(deltas, toModelExecutionOptionsBean(options)),
                    ActivitySubmissionOptions.create()
                            .withTaskTemplate(new TaskType()
                                    .name("Execute changes")
                                    .channel(SchemaConstants.CHANNEL_USER_URI)),
                    task, result);

        } catch (NotLoggedInException e) {
            throw restartOnLoginPageException();
        } catch (Exception e) {
            result.recordFatalError(e);
        } finally {
            result.computeStatusIfUnknown();
        }

        progressAwarePage.finishProcessing(target,true, result);
        return null;
    }
}
