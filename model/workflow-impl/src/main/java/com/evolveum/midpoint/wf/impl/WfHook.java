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
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.BaseConfigurationHelper;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;
import org.apache.commons.lang.Validate;
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

    public static final String WORKFLOW_HOOK_URI = "http://midpoint.evolveum.com/model/workflow-hook-1";        // todo

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
    public HookOperationMode invoke(ModelContext context, Task task, OperationResult parentResult) {

        Validate.notNull(context);
        Validate.notNull(task);
        Validate.notNull(parentResult);

        OperationResult result = parentResult.createSubresult(OPERATION_INVOKE);
        result.addParam("taskFromModel", task.toString());
        result.addContext("model state", context.getState());

        WfConfigurationType wfConfigurationType = baseConfigurationHelper.getWorkflowConfiguration(context, result);
        if (wfConfigurationType == null) {      // should not occur, but ... (e.g. when initially loading objects into repository; if done in non-raw mode)
            LOGGER.warn("No system configuration. Workflow approvals are disabled. Proceeding with operation execution as if everything is approved.");
            result.recordSuccess();
            return HookOperationMode.FOREGROUND;
        }
        if (Boolean.FALSE.equals(wfConfigurationType.isModelHookEnabled())) {
            LOGGER.info("Workflow model hook is disabled. Proceeding with operation execution as if everything is approved.");
            result.recordSuccess();
            return HookOperationMode.FOREGROUND;
        }

        logOperationInformation(context);

        try {
            HookOperationMode retval = processModelInvocation(context, wfConfigurationType, task, result);
            result.recordSuccessIfUnknown();
            return retval;
        } catch (RuntimeException e) {
            if (result.isUnknown()) {
                result.recordFatalError("Couldn't process model invocation in workflow module: " + e.getMessage(), e);
            }
            throw e;
        }
    }

    @Override
    public void invokeOnException(ModelContext context, Throwable throwable, Task task, OperationResult result) {
        // do nothing
    }

    private void logOperationInformation(ModelContext context) {

        if (LOGGER.isTraceEnabled()) {

            LensContext lensContext = (LensContext) context;

            LOGGER.trace("=====================================================================");
            LOGGER.trace("WfHook invoked in state " + context.getState() + " (wave " + lensContext.getProjectionWave() + ", max " + lensContext.getMaxWave() + "):");

            ObjectDelta pdelta = context.getFocusContext() != null ? context.getFocusContext().getPrimaryDelta() : null;
            ObjectDelta sdelta = context.getFocusContext() != null ? context.getFocusContext().getSecondaryDelta() : null;

            LOGGER.trace("Primary delta: " + (pdelta == null ? "(null)" : pdelta.debugDump()));
            LOGGER.trace("Secondary delta: " + (sdelta == null ? "(null)" : sdelta.debugDump()));
            LOGGER.trace("Projection contexts: " + context.getProjectionContexts().size());

            for (Object o : context.getProjectionContexts()) {
                ModelProjectionContext mpc = (ModelProjectionContext) o;
                ObjectDelta ppdelta = mpc.getPrimaryDelta();
                ObjectDelta psdelta = mpc.getSecondaryDelta();
                LOGGER.trace(" - Primary delta: " + (ppdelta == null ? "(null)" : ppdelta.debugDump()));
                LOGGER.trace(" - Secondary delta: " + (psdelta == null ? "(null)" : psdelta.debugDump()));
                LOGGER.trace(" - Sync delta:" + (mpc.getSyncDelta() == null ? "(null)" : mpc.getSyncDelta().debugDump()));
            }
        }
    }

    HookOperationMode processModelInvocation(ModelContext<? extends ObjectType> context, WfConfigurationType wfConfigurationType, Task taskFromModel, OperationResult result) {

        try {

            context.reportProgress(new ProgressInformation(WORKFLOWS, ENTERING));

            for (ChangeProcessor changeProcessor : wfConfiguration.getChangeProcessors()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Trying change processor: " + changeProcessor.getClass().getName());
                }
                try {
                    HookOperationMode hookOperationMode = changeProcessor.processModelInvocation(context, wfConfigurationType, taskFromModel, result);
                    if (hookOperationMode != null) {
                        return hookOperationMode;
                    }
                } catch (SchemaException e) {
                    LoggingUtils.logException(LOGGER, "Schema exception while running change processor {}", e, changeProcessor.getClass().getName());   // todo message
                    result.recordFatalError("Schema exception while running change processor " + changeProcessor.getClass(), e);
                    return HookOperationMode.ERROR;
                } catch (RuntimeException e) {
                    LoggingUtils.logException(LOGGER, "Runtime exception while running change processor {}", e, changeProcessor.getClass().getName());   // todo message
                    result.recordFatalError("Runtime exception while running change processor " + changeProcessor.getClass(), e);
                    return HookOperationMode.ERROR;
                }
            }
        } finally {
            if (result.isInProgress()) {
                // a bit of hack: IN_PROGRESS for workflows actually means "success"
                OperationResult r = result.clone();
                r.recordSuccess();
                context.reportProgress(new ProgressInformation(WORKFLOWS, r));
            } else {
                context.reportProgress(new ProgressInformation(WORKFLOWS, result));
            }
        }

        LOGGER.trace("No change processor caught this request, returning the FOREGROUND flag.");
        result.recordSuccess();
        return HookOperationMode.FOREGROUND;
    }

}
