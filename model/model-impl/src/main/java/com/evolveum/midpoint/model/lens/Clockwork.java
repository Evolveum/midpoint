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
package com.evolveum.midpoint.model.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.schema.constants.ObjectTypes;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.model.lens.projector.ContextLoader;
import com.evolveum.midpoint.model.lens.projector.Projector;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.HookListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.HookType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ModelHooksType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;

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
    
    @Autowired(required = true)
    private Clock clock;
    
    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService repositoryService;
    
    @Autowired(required = true)
    private ScriptExpressionFactory scriptExpressionFactory;
    
    @Autowired(required = true)
    private PrismContext prismContext;

    private LensDebugListener debugListener;
	
	public LensDebugListener getDebugListener() {
		return debugListener;
	}

	public void setDebugListener(LensDebugListener debugListener) {
		this.debugListener = debugListener;
	}

	public <F extends ObjectType> HookOperationMode run(LensContext<F> context, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (InternalsConfig.consistencyChecks) {
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
	
	public <F extends ObjectType> HookOperationMode click(LensContext<F> context, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		
		// DO NOT CHECK CONSISTENCY of the context here. The context may not be fresh and consistent yet. Project will fix
		// that. Check consistency afterwards (and it is also checked inside projector several times).
		
		if (context.getDebugListener() == null) {
			context.setDebugListener(debugListener);
		}
		
		try {
			
			// We need to determine focus before auditing. Otherwise we will not know user
			// for the accounts (unless there is a specific delta for it).
			// This is ugly, but it is the easiest way now (TODO: cleanup).
			contextLoader.determineFocusContext((LensContext<? extends FocusType>) context, result);
			
			ModelState state = context.getState();
			if (state == ModelState.INITIAL) {
				if (debugListener != null) {
					debugListener.beforeSync(context);
				}
				XMLGregorianCalendar requestTimestamp = clock.currentTimeXMLGregorianCalendar();
				context.getStats().setRequestTimestamp(requestTimestamp);
				// We need to do this BEFORE projection. If we would do that after projection
				// there will be secondary changes that are not part of the request.
				audit(context, AuditEventStage.REQUEST, task, result);
			}
			
			if (!context.isFresh()) {
				context.cleanup();
				projector.project(context, "PROJECTOR ("+state+")", task, result);
			} else {
				LOGGER.trace("Skipping projection because the context is fresh");
			}
			
			LensUtil.traceContext(LOGGER, "CLOCKWORK (" + state + ")", "before processing", true, context, false);
			if (InternalsConfig.consistencyChecks) {
				try {
					context.checkConsistence();
				} catch (IllegalStateException e) {
					throw new IllegalStateException(e.getMessage()+" in clockwork, state="+state, e);
				}
			}
			if (InternalsConfig.encryptionChecks && !ModelExecuteOptions.isNoCrypt(context.getOptions())) {
				context.checkEncrypted();
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
					processFinal(context, task, result);
					if (debugListener != null) {
						debugListener.afterSync(context);
					}
					return HookOperationMode.FOREGROUND;
			}		
			
			return invokeHooks(context, task, result);
			
		} catch (CommunicationException e) {
			processClockworkException(context, e, task, result);
			throw e;
		} catch (ConfigurationException e) {
			processClockworkException(context, e, task, result);
			throw e;
		} catch (ExpressionEvaluationException e) {
			processClockworkException(context, e, task, result);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			processClockworkException(context, e, task, result);
			throw e;
		} catch (ObjectNotFoundException e) {
			processClockworkException(context, e, task, result);
			throw e;
		} catch (PolicyViolationException e) {
			processClockworkException(context, e, task, result);
			throw e;
		} catch (SchemaException e) {
			processClockworkException(context, e, task, result);
			throw e;
		} catch (SecurityViolationException e) {
			processClockworkException(context, e, task, result);
			throw e;
		} catch (RuntimeException e) {
			processClockworkException(context, e, task, result);
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
    private HookOperationMode invokeHooks(LensContext context, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
    	// TODO: following two parts should be merged together in later versions
    	
    	// Execute configured scripting hooks
    	PrismObject<SystemConfigurationType> systemConfiguration = LensUtil.getSystemConfiguration(context, repositoryService, result);
    	// systemConfiguration may be null in some tests
    	if (systemConfiguration != null) {
	    	ModelHooksType modelHooks = systemConfiguration.asObjectable().getModelHooks();
	    	if (modelHooks != null) {
	    		HookListType changeHooks = modelHooks.getChange();
	    		if (changeHooks != null) {
	    			for (HookType hookType: changeHooks.getHook()) {
                        String shortDesc;
                        if (hookType.getName() != null) {
                            shortDesc = "hook '"+hookType.getName()+"'";
                        } else {
                            shortDesc = "scripting hook in system configuration";
                        }
	    				if (hookType.isEnabled() != null && !hookType.isEnabled()) {
	    					// Disabled hook, skip
	    					continue;
	    				}
                        if (hookType.getState() != null) {
                            if (!context.getState().toModelStateType().equals(hookType.getState())) {
                                continue;
                            }
                        }
                        if (hookType.getFocusType() != null) {
                            if (context.getFocusContext() == null) {
                                continue;
                            }
                            QName hookFocusTypeQname = hookType.getFocusType();
                            ObjectTypes hookFocusType = ObjectTypes.getObjectTypeFromTypeQName(hookFocusTypeQname);
                            if (hookFocusType == null) {
                                throw new SchemaException("Unknown focus type QName "+hookFocusTypeQname+" in "+shortDesc);
                            }
                            Class focusClass = context.getFocusClass();
                            Class<? extends ObjectType> hookFocusClass = hookFocusType.getClassDefinition();
                            if (!hookFocusClass.isAssignableFrom(focusClass)) {
                                continue;
                            }
                        }

	    				ScriptExpressionEvaluatorType scriptExpressionEvaluatorType = hookType.getScript();
	    				if (scriptExpressionEvaluatorType == null) {
	    					continue;
	    				}
	    				try {
							evaluateScriptingHook(context, hookType, scriptExpressionEvaluatorType, shortDesc, task, result);
						} catch (ExpressionEvaluationException e) {
							LOGGER.error("Evaluation of {} failed: {}", new Object[]{shortDesc, e.getMessage(), e});
							throw new ExpressionEvaluationException("Evaluation of "+shortDesc+" failed: "+e.getMessage(), e);
						} catch (ObjectNotFoundException e) {
							LOGGER.error("Evaluation of {} failed: {}", new Object[]{shortDesc, e.getMessage(), e});
							throw new ObjectNotFoundException("Evaluation of "+shortDesc+" failed: "+e.getMessage(), e);
						} catch (SchemaException e) {
							LOGGER.error("Evaluation of {} failed: {}", new Object[]{shortDesc, e.getMessage(), e});
							throw new SchemaException("Evaluation of "+shortDesc+" failed: "+e.getMessage(), e);
						}
	    			}
	    		}
	    	}
    	}
    	
    	// Execute registered Java hooks
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


    private void evaluateScriptingHook(LensContext context, HookType hookType,
    		ScriptExpressionEvaluatorType scriptExpressionEvaluatorType, String shortDesc, Task task, OperationResult result) 
    				throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
    	
    	LOGGER.trace("Evaluating {}", shortDesc);
		// TODO: it would be nice to cache this
		// null output definition: this script has no output
		ScriptExpression scriptExpression = scriptExpressionFactory.createScriptExpression(scriptExpressionEvaluatorType, null, shortDesc);
		
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinition(ExpressionConstants.VAR_PRISM_CONTEXT, prismContext);
		variables.addVariableDefinition(ExpressionConstants.VAR_MODEL_CONTEXT, context);
		LensFocusContext focusContext = context.getFocusContext();
		PrismObject focus = null;
		if (focusContext != null) {
			focus = focusContext.getObjectAny();
		}
		variables.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focus);
		
		LensUtil.evaluateScript(scriptExpression, context, variables, shortDesc, task, result);
	}

    private <F extends ObjectType> void processInitialToPrimary(LensContext<F> context, Task task, OperationResult result) {
		// Context loaded, nothing special do. Bump state to PRIMARY.
		context.setState(ModelState.PRIMARY);		
	}
	
	private <F extends ObjectType> void processPrimaryToSecondary(LensContext<F> context, Task task, OperationResult result) {
		// Nothing to do now. The context is already recomputed.
		context.setState(ModelState.SECONDARY);
	}
	
	private <F extends ObjectType> void processSecondary(LensContext<F> context, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		if (context.getExecutionWave() > context.getMaxWave() + 1) {
			context.setState(ModelState.FINAL);
			return;
		}
		
		changeExecutor.executeChanges(context, task, result);
		
		audit(context, AuditEventStage.EXECUTION, task, result);
		
		rotContext(context);
		
		context.incrementExecutionWave();
		
		LensUtil.traceContext(LOGGER, "CLOCKWORK (" + context.getState() + ")", "change execution", false, context, false);
	}
	
	/**
	 * Force recompute for the next wave. Recompute only those contexts that were changed.
	 * This is more inteligent than context.rot()
	 */
	private <F extends ObjectType> void rotContext(LensContext<F> context) throws SchemaException {
		boolean rot = false;
    	for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
    		if (projectionContext.getWave() != context.getExecutionWave()) {
    			LOGGER.trace("Context rot: projection {} NOT rotten because of wrong wave number", projectionContext);
        		continue;
			}
    		ObjectDelta<ShadowType> execDelta = projectionContext.getExecutableDelta();
    		if (isSignificant(execDelta)) {
    			LOGGER.trace("Context rot: projection {} rotten because of delta {}", projectionContext, execDelta);
    			projectionContext.setFresh(false);
    			projectionContext.setFullShadow(false);
    			rot = true;
    			// Propagate to higher-order projections
    			for (LensProjectionContext relCtx: LensUtil.findRelatedContexts(context, projectionContext)) {
    				relCtx.setFresh(false);
    				relCtx.setFullShadow(false);
    			}
	        } else {
	        	LOGGER.trace("Context rot: projection {} NOT rotten because no delta", projectionContext);
	        }
		}
    	LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext != null) {
    		ObjectDelta<F> execDelta = focusContext.getWaveDelta(context.getExecutionWave());
    		if (execDelta != null && !execDelta.isEmpty()) {
    			rot = true;
    		}
    		if (rot) {
	    		// It is OK to refresh focus all the time there was any change. This is cheap.
	    		focusContext.setFresh(false);
    		}
    	}
    	if (rot) {
    		context.setFresh(false);
    	}
	}
	
	private <P extends ObjectType> boolean isSignificant(ObjectDelta<P> delta) {
		if (delta == null || delta.isEmpty()) {
			return false;
		}
		if (delta.isAdd() || delta.isDelete()) {
			return true;
		}
		Collection<? extends ItemDelta<?>> attrDeltas = delta.findItemDeltasSubPath(new ItemPath(ShadowType.F_ATTRIBUTES));
		if (attrDeltas != null && !attrDeltas.isEmpty()) {
			return true;
		}
		return false;
	}
	
	private <F extends ObjectType> void processFinal(LensContext<F> context, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		auditFinalExecution(context, task, result);
		logFinalReadable(context, task, result);
	}
		
	private <F extends ObjectType> void audit(LensContext<F> context, AuditEventStage stage, Task task, OperationResult result) throws SchemaException {
		if (context.isLazyAuditRequest()) {
			if (stage == AuditEventStage.REQUEST) {
				// We skip auditing here, we will do it before execution
			} else if (stage == AuditEventStage.EXECUTION) {
				Collection<ObjectDeltaOperation<? extends ObjectType>> unauditedExecutedDeltas = context.getUnauditedExecutedDeltas();
				if ((unauditedExecutedDeltas == null || unauditedExecutedDeltas.isEmpty())) {
					// No deltas, nothing to audit in this wave
					return;
				}
				if (!context.isRequestAudited()) {
					auditEvent(context, AuditEventStage.REQUEST, context.getStats().getRequestTimestamp(), false, task, result);
				}
				auditEvent(context, stage, null, false, task, result);
			}
		} else {
			auditEvent(context, stage, null, false, task, result);
		}
	}
	
	/**
	 * Make sure that at least one execution is audited if a request was already audited. We don't want
	 * request without execution in the audit logs.
	 */
	private <F extends ObjectType> void auditFinalExecution(LensContext<F> context, Task task, OperationResult result) throws SchemaException {
		if (!context.isRequestAudited()) {
			return;
		}
		if (context.isExecutionAudited()) {
			return;
		}
		auditEvent(context, AuditEventStage.EXECUTION, null, true, task, result);
	}
	
	private <F extends ObjectType> void processClockworkException(LensContext<F> context, Exception e, Task task, OperationResult result) throws SchemaException {
		result.recordFatalError(e);
		auditEvent(context, AuditEventStage.EXECUTION, null, true, task, result);
	}

	private <F extends ObjectType> void auditEvent(LensContext<F> context, AuditEventStage stage, 
			XMLGregorianCalendar timestamp, boolean alwaysAudit, Task task, OperationResult result) throws SchemaException {
		
		PrismObject<? extends ObjectType> primaryObject = null;
		ObjectDelta<? extends ObjectType> primaryDelta = null;
		if (context.getFocusContext() != null) {
			primaryObject = context.getFocusContext().getObjectOld();
			if (primaryObject == null) {
				primaryObject = context.getFocusContext().getObjectNew();
			}
			primaryDelta = context.getFocusContext().getDelta();
		} else {
			Collection<LensProjectionContext> projectionContexts = context.getProjectionContexts();
			if (projectionContexts == null || projectionContexts.isEmpty()) {
				throw new IllegalStateException("No focus and no projectstions in "+context);
			}
			if (projectionContexts.size() > 1) {
				throw new IllegalStateException("No focus and more than one projection in "+context);
			}
			LensProjectionContext projection = projectionContexts.iterator().next();
			primaryObject = projection.getObjectOld();
			if (primaryObject == null) {
				primaryObject = projection.getObjectNew();
			}
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
//		} else {
//			throw new IllegalStateException("No primary object in:\n"+context.dump());
		}
		
		auditRecord.setChannel(context.getChannel());
		
		if (stage == AuditEventStage.REQUEST) {
			auditRecord.addDeltas(ObjectDeltaOperation.cloneDeltaCollection(context.getPrimaryChanges()));
		} else if (stage == AuditEventStage.EXECUTION) {
			auditRecord.setOutcome(result.getComputeStatus());
			Collection<ObjectDeltaOperation<? extends ObjectType>> unauditedExecutedDeltas = context.getUnauditedExecutedDeltas();
			if (!alwaysAudit && (unauditedExecutedDeltas == null || unauditedExecutedDeltas.isEmpty())) {
				// No deltas, nothing to audit in this wave
				return;
			}
			auditRecord.addDeltas(ObjectDeltaOperation.cloneCollection(unauditedExecutedDeltas));
		} else {
			throw new IllegalStateException("Unknown audit stage "+stage);
		}
		
		if (timestamp != null) {
			auditRecord.setTimestamp(XmlTypeConverter.toMillis(timestamp));
		}
		
		addRecordMessage(auditRecord, result);
		
		auditService.audit(auditRecord, task);
		
		if (stage == AuditEventStage.EXECUTION) {
			// We need to clean up so these deltas will not be audited again in next wave
			context.markExecutedDeltasAudited();
			context.setExecutionAudited(true);
		} else if (stage == AuditEventStage.REQUEST) {
			context.setRequestAudited(true);
		} else {
			throw new IllegalStateException("Unknown audit stage "+stage);
		}
	}
	
	/**
	 * Adds a message to the record by pulling the messages from individual delta results.
	 */
	private void addRecordMessage(AuditEventRecord auditRecord, OperationResult result) {
		if (auditRecord.getMessage() != null) {
			return;
		}
		if (!StringUtils.isEmpty(result.getMessage())) {
			String message = result.getMessage();
			auditRecord.setMessage(message);
			return;
		}
		Collection<ObjectDeltaOperation<? extends ObjectType>> deltas = auditRecord.getDeltas();
		if (deltas == null || deltas.isEmpty()) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (ObjectDeltaOperation<? extends ObjectType> delta: deltas) {
			OperationResult executionResult = delta.getExecutionResult();
			if (executionResult != null) {
				String message = executionResult.getMessage();
				if (!StringUtils.isEmpty(message)) {
					if (sb.length() != 0) {
						sb.append("; ");
					}					
					sb.append(message);
				}
			}
		}
		auditRecord.setMessage(sb.toString());
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
	
	/**
	 * Logs the entire operation in a human-readable fashion.
	 */
	private <F extends ObjectType> void logFinalReadable(LensContext<F> context, Task task, OperationResult result) throws SchemaException {
		if (!LOGGER.isDebugEnabled()) {
			return;
		}
		
		// a priori: sync delta
		boolean hasSyncDelta = false;
		for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
			ObjectDelta<ShadowType> syncDelta = projectionContext.getSyncDelta();
			if (syncDelta != null) {
				hasSyncDelta = true;
			}
		}
		
		Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = context.getExecutedDeltas();
		if (!hasSyncDelta && executedDeltas == null || executedDeltas.isEmpty()) {
			// Not worth mentioning
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		String channel = context.getChannel();
		if (channel != null) {
			sb.append("Channel: ").append(channel).append("\n");
		}
		
		
		if (hasSyncDelta) {
			sb.append("Triggered by synchronization delta\n");
			for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
				ObjectDelta<ShadowType> syncDelta = projectionContext.getSyncDelta();
				if (syncDelta != null) {
					sb.append(syncDelta.debugDump(1));
					sb.append("\n");
				}
				DebugUtil.debugDumpLabel(sb, "Situation", 1);
				sb.append(" ");
				sb.append(projectionContext.getSynchronizationSituationDetected());
				sb.append(" -> ");
				sb.append(projectionContext.getSynchronizationSituationResolved());
				sb.append("\n");
			}
		}
		for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
			if (projectionContext.isSyncAbsoluteTrigger()) {
				sb.append("Triggered by absolute state of ").append(projectionContext.getHumanReadableName());
				sb.append(": ");
				sb.append(projectionContext.getSynchronizationSituationDetected());
				sb.append(" -> ");
				sb.append(projectionContext.getSynchronizationSituationResolved());
				sb.append("\n");
			}
		}
		
		// focus primary
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext != null) {
			ObjectDelta<F> focusPrimaryDelta = focusContext.getPrimaryDelta();
			if (focusPrimaryDelta != null) {
				sb.append("Triggered by focus primary delta\n");
				DebugUtil.indentDebugDump(sb, 1);
				sb.append(focusPrimaryDelta.toString());
				sb.append("\n");
			}
		}
		
		// projection primary
		Collection<ObjectDelta<ShadowType>> projPrimaryDeltas = new ArrayList<ObjectDelta<ShadowType>>();
		for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
			ObjectDelta<ShadowType> projPrimaryDelta = projectionContext.getPrimaryDelta();
			if (projPrimaryDelta != null) {
				projPrimaryDeltas.add(projPrimaryDelta);
			}
		}
		if (!projPrimaryDeltas.isEmpty()) {
			sb.append("Triggered by projection primary delta\n");
			for (ObjectDelta<ShadowType> projDelta: projPrimaryDeltas) {
				DebugUtil.indentDebugDump(sb, 1);
				sb.append(projDelta.toString());
				sb.append("\n");
			}
		}
				
		if (focusContext != null) {
			sb.append("Focus: ").append(focusContext.getHumanReadableName()).append("\n");
		}
		if (!context.getProjectionContexts().isEmpty()) {
			sb.append("Projections (").append(context.getProjectionContexts().size()).append("):\n");
			for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
				DebugUtil.indentDebugDump(sb, 1);
				sb.append(projectionContext.getHumanReadableName());
				sb.append(": ");
				sb.append(projectionContext.getSynchronizationPolicyDecision());
				sb.append("\n");
			}
		}
		
		if (executedDeltas == null || executedDeltas.isEmpty()) {
			sb.append("Executed: nothing\n");
		} else {
			sb.append("Executed:\n");
			for (ObjectDeltaOperation<? extends ObjectType> executedDelta: executedDeltas) {
				ObjectDelta<? extends ObjectType> delta = executedDelta.getObjectDelta();
				OperationResult deltaResult = executedDelta.getExecutionResult();
				DebugUtil.indentDebugDump(sb, 1);
				sb.append(delta.toString());
				sb.append(": ");
				sb.append(deltaResult.getStatus());
				sb.append("\n");
			}
		}
		
		LOGGER.debug("\n###[ CLOCKWORK SUMMARY ]######################################\n{}" +
				       "##############################################################",
				sb.toString());
	}
	
}
