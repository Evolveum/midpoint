/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.lens;

import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.ChangeExecutor;
import com.evolveum.midpoint.model.ModelCompiletimeConfig;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.lens.projector.Projector;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
@Component
public class Clockwork {
	
	private static final Trace LOGGER = TraceManager.getTrace(Clockwork.class);
	
	@Autowired(required = true)
	Projector projector;
	
	@Autowired(required = true)
	private ChangeExecutor changeExecutor;

    @Autowired(required = false)
    private HookRegistry hookRegistry;

    private LensDebugListener debugListener;
	
	private boolean consistenceChecks = true;
	
	public LensDebugListener getDebugListener() {
		return debugListener;
	}

	public void setDebugListener(LensDebugListener debugListener) {
		this.debugListener = debugListener;
	}

	public HookOperationMode run(LensContext context, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (ModelCompiletimeConfig.CONSISTENCY_CHECKS) {
			context.checkConsistence();
		}
		while (context.getState() != ModelState.FINAL) {
            HookOperationMode mode = click(context, task, result);
            if (mode != HookOperationMode.FOREGROUND) {
                return mode;
            }
		}
        return HookOperationMode.FOREGROUND;
	}
	
	public HookOperationMode click(LensContext context, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		
		if (!context.isFresh()) {
			projector.project(context, "synchronization projection", result);
		}
		
		ModelState state = context.getState();
		
		LensUtil.traceContext(LOGGER, "synchronization", state.toString(), context, true);
		
		switch (state) {
			case INITIAL:
				processInitialToPrimary(context, task, result);
				break;
			case PRIMARY:
				processPrimaryToSecondary(context, task, result);
				break;
			case SECONDARY:
				processSecondary(context, task, result);
				break;
			case FINAL:
				return HookOperationMode.FOREGROUND;
		}		
		
		return invokeHooks(context, task, result);
	}

    /**
     * Invokes hooks, if there are any.
     *
     * @return
     *  - ERROR, if any hook reported error; otherwise returns
     *  - BACKGROUND, if any hook reported switching to background; otherwise
     *  - FOREGROUND (if all hooks reported finishing on foreground)
     */
    private HookOperationMode invokeHooks(LensContext context, Task task, OperationResult result) {

        HookOperationMode resultMode = HookOperationMode.FOREGROUND;
        if (hookRegistry != null) {
            for (ChangeHook hook : hookRegistry.getAllChangeHooks()) {
                HookOperationMode mode = hook.invoke(context, task, result);
                if (mode == HookOperationMode.ERROR) {
                    resultMode = HookOperationMode.ERROR;
                } else if (mode == HookOperationMode.BACKGROUND) {
                    if (resultMode != HookOperationMode.ERROR) {
                        resultMode = HookOperationMode.BACKGROUND;
                    }
                }
            }
        }
        return resultMode;
    }


    private void processInitialToPrimary(LensContext context, Task task, OperationResult result) {
		// Context loaded, nothing to do. Bump state to PRIMARY
		context.setState(ModelState.PRIMARY);
	}
	
	private void processPrimaryToSecondary(LensContext context, Task task, OperationResult result) {
		// Nothing to do now. The context is already recomputed.
		context.setState(ModelState.SECONDARY);
	}
	
	private void processSecondary(LensContext context, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (context.getWave() > context.getMaxWave() + 1) {
			context.setState(ModelState.FINAL);
			return;
		}
		// execute current wave and go to the next wave
		changeExecutor.executeChanges(context, task, result);
		// TODO: attempts
		context.incrementWave();
		// Force recompute for next wave
		context.setFresh(false);
	}

}
