/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.BaseConfigurationHelper;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.WORKFLOWS;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.ENTERING;

/**
 * Provides an interface between the model and the workflow engine:
 * catches hook calls and delegates them to change processors.
 *
 * @author mederly
 */
@Component
public class WfHook implements ChangeHook {

    private static final Trace LOGGER = TraceManager.getTrace(WfHook.class);

    private static final String WORKFLOW_HOOK_URI = "http://midpoint.evolveum.com/model/workflow-hook-1";        // todo

    @Autowired
    private WfConfiguration wfConfiguration;

    @Autowired
    private BaseConfigurationHelper baseConfigurationHelper;

    @Autowired
    private HookRegistry hookRegistry;

    private static final String DOT_CLASS = WfHook.class.getName() + ".";
    private static final String OPERATION_INVOKE = DOT_CLASS + "invoke";

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
    public <O extends ObjectType> HookOperationMode invoke(@NotNull ModelContext<O> context, @NotNull Task taskFromModel,
            @NotNull OperationResult parentResult) {

        Validate.notNull(context);
        Validate.notNull(taskFromModel);
        Validate.notNull(parentResult);

	    // Generally this cannot be minor as we need the "task switched to background" flag.
	    // But if the hook does nothing (returns FOREGROUND flag), we mark the result
	    // as minor afterwards.
        OperationResult result = parentResult.createSubresult(OPERATION_INVOKE);
        result.addParam("taskFromModel", taskFromModel.toString());
        result.addContext("model state", context.getState());
        try {
            WfConfigurationType wfConfigurationType = baseConfigurationHelper.getWorkflowConfiguration(context, result);
            // TODO consider this if it's secure enough
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
            if (SchemaConstants.CHANNEL_GUI_INIT_URI.equals(context.getChannel())) {
                LOGGER.debug("Skipping workflow processing because the channel is '" + SchemaConstants.CHANNEL_GUI_INIT_URI + "'.");
                result.recordSuccess();
                return HookOperationMode.FOREGROUND;
            }

            logOperationInformation(context);

            HookOperationMode retval = processModelInvocation(context, wfConfigurationType, taskFromModel, result);
            result.computeStatus();
            if (retval == HookOperationMode.FOREGROUND) {
	            result.setMinor(true);
            }
            return retval;
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't process model invocation in workflow module: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void invokeOnException(@NotNull ModelContext context, @NotNull Throwable throwable, @NotNull Task task, @NotNull OperationResult result) {
        // do nothing
    }

    private void logOperationInformation(ModelContext context) {

        if (LOGGER.isTraceEnabled()) {
        	@SuppressWarnings({"unchecked", "raw"})
            LensContext<?> lensContext = (LensContext<?>) context;
			try {
				LensUtil.traceContext(LOGGER, "WORKFLOW (" + context.getState() + ")", "workflow processing", true, lensContext, false);
			} catch (SchemaException e) {
				throw new IllegalStateException("SchemaException when tracing model context: " + e.getMessage(), e);
			}
        }
    }

    private HookOperationMode processModelInvocation(@NotNull ModelContext<? extends ObjectType> modelContext,
            WfConfigurationType wfConfigurationType, @NotNull Task taskFromModel, @NotNull OperationResult result) {

        try {

            modelContext.reportProgress(new ProgressInformation(WORKFLOWS, ENTERING));

            for (ChangeProcessor changeProcessor : wfConfiguration.getChangeProcessors()) {
                LOGGER.trace("Trying change processor: {}", changeProcessor.getClass().getName());
                try {
                    HookOperationMode hookOperationMode = changeProcessor.processModelInvocation(modelContext, wfConfigurationType, taskFromModel, result);
                    if (hookOperationMode != null) {
                        return hookOperationMode;
                    }
                } catch (ObjectNotFoundException|SchemaException|RuntimeException|ExpressionEvaluationException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Exception while running change processor {}", e, changeProcessor.getClass().getName());
                    result.recordFatalError("Exception while running change processor " + changeProcessor.getClass(), e);
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
