/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * Provides an interface between the model and the workflow engine:
 * catches hook calls and determines which ones require workflow invocation
 * (actually, more-or-less just delegates to wfCore)
 *
 * @author mederly
 */
public class WfHook implements ChangeHook {

    public static final String WORKFLOW_HOOK_URI = "http://midpoint.evolveum.com/model/workflow-hook-1";

    private static final Trace LOGGER = TraceManager.getTrace(WfHook.class);

    public WfCore wfCore;

    private static final String DOT_CLASS = WfHook.class.getName() + ".";
    private static final String OPERATION_INVOKE = DOT_CLASS + "invoke";

    public WfHook(WorkflowManager workflowManager, WfCore wfCore) {
        this.wfCore = wfCore;
    }

    public void register(HookRegistry hookRegistry) {
        LOGGER.trace("Registering workflow hook");
        hookRegistry.registerChangeHook(WfHook.WORKFLOW_HOOK_URI, this);
    }

    @Override
    public HookOperationMode invoke(ModelContext context, Task task, OperationResult parentResult) {

        Validate.notNull(context);
        Validate.notNull(task);
        Validate.notNull(parentResult);

        if (context.getFocusContext() == null) {        // probably not a user-related event
            return HookOperationMode.FOREGROUND;
        }

        OperationResult result = parentResult.createSubresult(OPERATION_INVOKE);
        result.addParam("task", task.toString());

        logOperationInformation(context);

        HookOperationMode retval = wfCore.startProcessesIfNeeded(context, task, result);
        result.recordSuccessIfUnknown();
        return retval;
    }

    private void logOperationInformation(ModelContext context) {

        if (LOGGER.isTraceEnabled()) {

            LensContext lensContext = (LensContext) context;

            LOGGER.trace("=====================================================================");
            LOGGER.trace("WfHook invoked in state " + context.getState() + " (wave " + lensContext.getProjectionWave() + ", max " + lensContext.getMaxWave() + "):");

            ObjectDelta pdelta = context.getFocusContext().getPrimaryDelta();
            ObjectDelta sdelta = context.getFocusContext().getSecondaryDelta();

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

}
