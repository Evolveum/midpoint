/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.hook;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.WORKFLOWS;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.ENTERING;

import java.util.List;
import jakarta.annotation.PostConstruct;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.impl.lens.ClockworkMedic;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.ApprovalsManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.ConfigurationHelper;
import com.evolveum.midpoint.wf.impl.processors.ModelInvocationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaExecutionInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;

/**
 * Provides an interface between the model and the workflow engine:
 * catches hook calls and delegates them to change processors.
 */
@Component
public class WfHook implements ChangeHook {

    private static final Trace LOGGER = TraceManager.getTrace(WfHook.class);

    private static final String WORKFLOW_HOOK_URI = "http://midpoint.evolveum.com/model/workflow-hook-1";        // todo

    @Autowired private PrismContext prismContext;
    @Autowired private WfConfiguration wfConfiguration;
    @Autowired private ConfigurationHelper configurationHelper;
    @Autowired private HookRegistry hookRegistry;
    @Autowired private ApprovalsManager approvalsManager;
    @Autowired private ClockworkMedic medic;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    private static final String DOT_CLASS = WfHook.class.getName() + ".";
    private static final String OP_INVOKE = DOT_CLASS + "invoke";

    @PostConstruct
    public void init() {
        if (wfConfiguration.isEnabled()) {
            hookRegistry.registerChangeHook(WfHook.WORKFLOW_HOOK_URI, this);
            LOGGER.info("Workflow change hook was registered.");
        } else {
            LOGGER.info("Workflow change hook is not registered, because workflows are disabled.");
        }
    }

    @Override
    public <O extends ObjectType> HookOperationMode invoke(
            @NotNull ModelContext<O> context, @NotNull Task task, @NotNull OperationResult parentResult) {
        // Generally this cannot be minor as we need the "task switched to background" flag.
        // But if the hook does nothing (returns FOREGROUND flag), we mark the result
        // as minor afterwards.
        OperationResult result = parentResult.createSubresult(OP_INVOKE);
        result.addParam("task", task.toString());
        result.addArbitraryObjectAsContext("model state", context.getState());
        try {
            if (context.isPreview() || context.isSimulation()) {
                result.recordNotApplicable("preview/simulation mode");
                return HookOperationMode.FOREGROUND;
            }

            WfConfigurationType wfConfigurationType = configurationHelper.getWorkflowConfiguration(context, result);
            if (wfConfigurationType != null && Boolean.FALSE.equals(wfConfigurationType.isModelHookEnabled())) {
                LOGGER.info("Workflow model hook is disabled. Proceeding with operation execution as if everything is approved.");
                result.recordSuccess();
                return HookOperationMode.FOREGROUND;
            }

            if (context.getPartialProcessingOptions().getApprovals() == PartialProcessingTypeType.SKIP) {
                LOGGER.debug("Skipping workflow processing because of the partial processing option set to SKIP");
                result.recordSuccess();
                return HookOperationMode.FOREGROUND;
            }

            // It would be also possible to set 'skip' partial processing option for initial import; however,
            // e.g. for tests, initialization is scattered through many places, so that would be too much work.
            if (SchemaConstants.CHANNEL_INIT_URI.equals(context.getChannel())) {
                LOGGER.debug("Skipping workflow processing because the channel is '" + SchemaConstants.CHANNEL_INIT_URI + "'.");
                result.recordSuccess();
                return HookOperationMode.FOREGROUND;
            }

            logOperationInformation(context);

            HookOperationMode retval = processModelInvocation(context, wfConfigurationType, task, result);
            result.computeStatus();
            if (retval == HookOperationMode.FOREGROUND) {
                result.setMinor();
            }
            return retval;
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't process model invocation in workflow module: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void invokeOnException(@NotNull ModelContext<?> context, @NotNull Throwable throwable, @NotNull Task task, @NotNull OperationResult result) {
        // do nothing
    }

    @Override
    public void invokePreview(@NotNull ModelContext<? extends ObjectType> context, Task task, OperationResult result) {
        if (context.getPartialProcessingOptions().getApprovals() != PartialProcessingTypeType.PROCESS) {
            return;
        }
        try {
            List<ApprovalSchemaExecutionInformationType> preview =
                    approvalsManager.getApprovalSchemaPreview(context, task, result);
            ((LensContext<?>) context).addHookPreviewResults(WORKFLOW_HOOK_URI, preview);
        } catch (CommonException e) {
            // already recorded in the operation result, so no more processing is necessary
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't preview approvals", e);
        }
    }

    private void logOperationInformation(ModelContext<?> context) {
        if (LOGGER.isTraceEnabled()) {
            LensContext<?> lensContext = (LensContext<?>) context;
            medic.traceContext(LOGGER, "WORKFLOW (" + context.getState() + ")", "workflow processing", true, lensContext, false);
        }
    }

    private HookOperationMode processModelInvocation(@NotNull ModelContext<? extends ObjectType> modelContext,
            WfConfigurationType wfConfigurationType, @NotNull Task opTask, @NotNull OperationResult result) {
        try {
            modelContext.reportProgress(new ProgressInformation(WORKFLOWS, ENTERING));
            ModelInvocationContext<?> ctx =
                    new ModelInvocationContext<>(modelContext, wfConfigurationType, repositoryService, opTask);
            for (ChangeProcessor changeProcessor : wfConfiguration.getChangeProcessors()) {
                LOGGER.trace("Trying change processor: {}", changeProcessor.getClass().getName());
                try {
                    HookOperationMode hookOperationMode = changeProcessor.processModelInvocation(ctx, result);
                    if (hookOperationMode != null) {
                        return hookOperationMode;
                    }
                } catch (Exception e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Exception while running change processor {}: {}", e,
                            changeProcessor.getClass().getName(), e.getMessage());
                    result.recordFatalError("Exception while running change processor "
                            + changeProcessor.getClass().getSimpleName() + ": " + e.getMessage(), e);
                    return HookOperationMode.ERROR;
                }
            }
        } finally {
            if (result.isInProgress()) {
                // a bit of hack: IN_PROGRESS for workflows actually means "success"
                OperationResult r = result.clone();
                r.recordSuccess();
                modelContext.reportProgress(new ProgressInformation(WORKFLOWS, r));
            } else {
                modelContext.reportProgress(new ProgressInformation(WORKFLOWS, result));
            }
        }
        LOGGER.trace("No change processor caught this request, returning the FOREGROUND flag.");
        return HookOperationMode.FOREGROUND;
    }
}
