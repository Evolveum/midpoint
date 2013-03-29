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

import java.util.Collection;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.CompiletimeConfig;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.lens.projector.ContextLoader;
import com.evolveum.midpoint.model.lens.projector.Projector;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 * @author semancik
 *
 */
@Component
public class Clockwork {
	
	public static final int MAX_REWIND_ATTEMPTS = 2;
	
	private static final Trace LOGGER = TraceManager.getTrace(Clockwork.class);
	
	@Autowired(required = true)
	private Projector projector;
	
	// This is ugly
	// TODO: cleanup
	@Autowired(required = true)
	private ContextLoader contextLoader;
	
	@Autowired(required = true)
	private ChangeExecutor changeExecutor;

    @Autowired(required = false)
    private HookRegistry hookRegistry;
    
    @Autowired(required = true)
	private AuditService auditService;

    private LensDebugListener debugListener;
	
	private boolean consistenceChecks = true;
	
	public LensDebugListener getDebugListener() {
		return debugListener;
	}

	public void setDebugListener(LensDebugListener debugListener) {
		this.debugListener = debugListener;
	}

	public <F extends ObjectType, P extends ObjectType> HookOperationMode run(LensContext<F,P> context, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, RewindException {
		if (CompiletimeConfig.CONSISTENCY_CHECKS) {
			context.checkConsistence();
		}
		while (context.getState() != ModelState.FINAL) {
            HookOperationMode mode = click(context, task, result);

            if (mode == HookOperationMode.BACKGROUND) {
                result.recordInProgress();
                return mode;
            } else if (mode == HookOperationMode.ERROR) {
                return mode;
            }
		}
		// One last click in FINAL state
        return click(context, task, result);
	}
	
	public <F extends ObjectType, P extends ObjectType> HookOperationMode click(LensContext<F,P> context, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, RewindException {
		
		// DO NOT CHECK CONSISTENCY of the context here. The context may not be fresh and consistent yet. Project will fix
		// that. Check consistency afterwards (and it is also checked inside projector several times).
		
		try {
			
			// We need to determine focus before auditing. Otherwise we will not know user
			// for the accounts (unless there is a specific delta for it).
			// This is ugly, but it is the easiest way now (TODO: cleanup).
			contextLoader.determineFocusContext(context, result);
			
			ModelState state = context.getState();
			if (state == ModelState.INITIAL) {
				if (debugListener != null) {
					debugListener.beforeSync(context);
				}
				// We need to do this BEFORE projection. If we would do that after projection
				// there will be secondary changes that are not part of the request.
				audit(context, AuditEventStage.REQUEST, task, result);
			}
			
			if (!context.isFresh()) {
				context.cleanup();
				projector.project(context, "projection", result);
			}
			
			LensUtil.traceContext(LOGGER, "clockwork", state.toString() + " projection (before processing)", true, context, false);
			if (CompiletimeConfig.CONSISTENCY_CHECKS) {
				try {
					context.checkConsistence();
				} catch (IllegalStateException e) {
					throw new IllegalStateException(e.getMessage()+" in clockwork, state="+state, e);
				}
			}
			
	//		LOGGER.info("CLOCKWORK: {}: {}", state, context);
			
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
					if (debugListener != null) {
						debugListener.afterSync(context);
					}
					return HookOperationMode.FOREGROUND;
			}		
			
			return invokeHooks(context, task, result);
			
		} catch (CommunicationException e) {
			audit(context, AuditEventStage.EXECUTION, task, result);
			throw e;
		} catch (ConfigurationException e) {
			audit(context, AuditEventStage.EXECUTION, task, result);
			throw e;
		} catch (ExpressionEvaluationException e) {
			audit(context, AuditEventStage.EXECUTION, task, result);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			audit(context, AuditEventStage.EXECUTION, task, result);
			throw e;
		} catch (ObjectNotFoundException e) {
			audit(context, AuditEventStage.EXECUTION, task, result);
			throw e;
		} catch (PolicyViolationException e) {
			audit(context, AuditEventStage.EXECUTION, task, result);
			throw e;
		} catch (SchemaException e) {
			audit(context, AuditEventStage.EXECUTION, task, result);
			throw e;
		} catch (SecurityViolationException e) {
			audit(context, AuditEventStage.EXECUTION, task, result);
			throw e;
		} catch (RuntimeException e) {
			audit(context, AuditEventStage.EXECUTION, task, result);
			throw e;
		}
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


    private <F extends ObjectType, P extends ObjectType> void processInitialToPrimary(LensContext<F,P> context, Task task, OperationResult result) {
		// Context loaded, nothing special do. Bump state to PRIMARY.
		context.setState(ModelState.PRIMARY);		
	}
	
	private <F extends ObjectType, P extends ObjectType> void processPrimaryToSecondary(LensContext<F,P> context, Task task, OperationResult result) {
		// Nothing to do now. The context is already recomputed.
		context.setState(ModelState.SECONDARY);
	}
	
	private <F extends ObjectType, P extends ObjectType> void processSecondary(LensContext<F,P> context, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, RewindException, ExpressionEvaluationException {
		if (context.getExecutionWave() > context.getMaxWave() + 1) {
			context.setState(ModelState.FINAL);
			return;
		}
		
		changeExecutor.executeChanges(context, task, result);
		
		audit(context, AuditEventStage.EXECUTION, task, result);
		
		context.incrementExecutionWave();

		// Force recompute for next wave
		context.rot();
		
		LensUtil.traceContext(LOGGER, "clockwork", context.getState() + " change execution", false, context, false);
	}

	private <F extends ObjectType, P extends ObjectType> void audit(LensContext<F,P> context, AuditEventStage stage, Task task, OperationResult result) throws SchemaException {
		
		PrismObject<? extends ObjectType> primaryObject = null;
		ObjectDelta<? extends ObjectType> primaryDelta = null;
		if (context.getFocusContext() != null) {
			primaryObject = context.getFocusContext().getObjectOld();
			primaryDelta = context.getFocusContext().getDelta();
		} else {
			Collection<LensProjectionContext<P>> projectionContexts = context.getProjectionContexts();
			if (projectionContexts == null || projectionContexts.isEmpty()) {
				throw new IllegalStateException("No focus and no projectstions in "+context);
			}
			if (projectionContexts.size() > 1) {
				throw new IllegalStateException("No focus and more than one projection in "+context);
			}
			LensProjectionContext<P> projection = projectionContexts.iterator().next();
			primaryObject = projection.getObjectOld();
			primaryDelta = projection.getDelta();
		}
		
		AuditEventType eventType = null;
		if (primaryDelta == null) {
			eventType = AuditEventType.SYNCHRONIZATION;
		} else if (primaryDelta.isAdd()) {
			eventType = AuditEventType.ADD_OBJECT;
		} else if (primaryDelta.isModify()) {
			eventType = AuditEventType.MODIFY_OBJECT;
		} else if (primaryDelta.isDelete()) {
			eventType = AuditEventType.DELETE_OBJECT;
		} else {
			throw new IllegalStateException("Unknown state of delta "+primaryDelta);
		}
		AuditEventRecord auditRecord = new AuditEventRecord(eventType, stage);
		if (primaryObject != null) {
			auditRecord.setTarget(primaryObject.clone());
		}
		auditRecord.setChannel(context.getChannel());
		if (stage == AuditEventStage.REQUEST) {
			auditRecord.addDeltas(ObjectDeltaOperation.cloneDeltaCollection(context.getAllChanges()));
		} else if (stage == AuditEventStage.EXECUTION) {
			auditRecord.setResult(result);
			auditRecord.setOutcome(result.getComputeStatus());
			Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = context.getExecutedDeltas();
			if ((executedDeltas == null || executedDeltas.isEmpty()) && auditRecord.getOutcome() == OperationResultStatus.SUCCESS) {
				// No deltas, nothing to audit in this wave
				return;
			}
			auditRecord.addDeltas(ObjectDeltaOperation.cloneCollection(executedDeltas));
		} else {
			throw new IllegalStateException("Unknown audit stage "+stage);
		}
		auditService.audit(auditRecord, task);
		// We need to clean up so these deltas will not be audited again in next wave
		context.clearExecutedDeltas();
	}
	
	public static void throwException(Throwable e) throws ObjectAlreadyExistsException, ObjectNotFoundException {
		if (e instanceof ObjectAlreadyExistsException) {
			throw (ObjectAlreadyExistsException)e;
		} else if (e instanceof ObjectNotFoundException) {
			throw (ObjectNotFoundException)e;
		} else if (e instanceof SystemException) {
			throw (SystemException)e;
		} else {
			throw new SystemException("Unexpected exception "+e.getClass()+" "+e.getMessage(), e);
		}
	}
	
}
