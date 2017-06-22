/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.expression.evaluator.caching.AssociationSearchExpressionEvaluatorCache;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.controller.ModelUtils;
import com.evolveum.midpoint.model.impl.lens.projector.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.FocusConstraintsChecker;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.model.impl.sync.RecomputeTaskHandler;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceOperationListener;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.security.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskBinding;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.AuthorizationException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author semancik
 *
 */
@Component
public class Clockwork {
	
	private static final int DEFAULT_NUMBER_OF_RESULTS_TO_KEEP = 5;

	private static final Trace LOGGER = TraceManager.getTrace(Clockwork.class);
	
	@Autowired(required=true)
	private Projector projector;
	
	// This is ugly
	// TODO: cleanup
	@Autowired
	private ContextLoader contextLoader;
	
	@Autowired(required=true)
	private ChangeExecutor changeExecutor;

    @Autowired(required = false)
    private HookRegistry hookRegistry;
    
    @Autowired
	private AuditService auditService;
    
    @Autowired
    private SecurityEnforcer securityEnforcer;
    
    @Autowired
    private Clock clock;
    
    @Autowired
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService repositoryService;
    
    @Autowired
	private ModelObjectResolver objectResolver;
    
    @Autowired
	private SystemObjectCache systemObjectCache;

	@Autowired
	private transient ProvisioningService provisioningService;

	@Autowired
	private transient ChangeNotificationDispatcher changeNotificationDispatcher;

    @Autowired
    private ScriptExpressionFactory scriptExpressionFactory;
    
    @Autowired(required=true)
    private PersonaProcessor personaProcessor;
    
    @Autowired
    private PrismContext prismContext;

    @Autowired
    private TaskManager taskManager;
    
    @Autowired
    private OperationalDataManager metadataManager;

    private LensDebugListener debugListener;
	
	public LensDebugListener getDebugListener() {
		return debugListener;
	}

	public void setDebugListener(LensDebugListener debugListener) {
		this.debugListener = debugListener;
	}

	private static final int DEFAULT_MAX_CLICKS = 200;

	public <F extends ObjectType> HookOperationMode run(LensContext<F> context, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		LOGGER.trace("Running clockwork for context {}", context);
		if (InternalsConfig.consistencyChecks) {
			context.checkConsistence();
		}

		int clicked = 0;

		try {
			FocusConstraintsChecker.enterCache();
			enterAssociationSearchExpressionEvaluatorCache();
			provisioningService.enterConstraintsCheckerCache();

			while (context.getState() != ModelState.FINAL) {

				// TODO implement in model context (as transient or even non-transient attribute) to allow for checking in more complex scenarios
				int maxClicks = getMaxClicks(context, result);
				if (clicked >= maxClicks) {
					throw new IllegalStateException("Model operation took too many clicks (limit is " + maxClicks + "). Is there a cycle?");
				}
				clicked++;

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
		} finally {
			FocusConstraintsChecker.exitCache();
			exitAssociationSearchExpressionEvaluatorCache();
			provisioningService.exitConstraintsCheckerCache();
		}
	}

	private void enterAssociationSearchExpressionEvaluatorCache() {
		AssociationSearchExpressionEvaluatorCache cache = AssociationSearchExpressionEvaluatorCache.enterCache();
		AssociationSearchExpressionCacheInvalidator invalidator = new AssociationSearchExpressionCacheInvalidator(cache);
		cache.setClientContextInformation(invalidator);
		changeNotificationDispatcher.registerNotificationListener((ResourceObjectChangeListener) invalidator);
		changeNotificationDispatcher.registerNotificationListener((ResourceOperationListener) invalidator);
	}

	private void exitAssociationSearchExpressionEvaluatorCache() {
		AssociationSearchExpressionEvaluatorCache cache = AssociationSearchExpressionEvaluatorCache.exitCache();
		if (cache == null) {
			return;			// shouldn't occur
		}
		Object invalidator = cache.getClientContextInformation();
		if (invalidator == null || !(invalidator instanceof AssociationSearchExpressionCacheInvalidator)) {
			return;			// shouldn't occur either
		}
		changeNotificationDispatcher.unregisterNotificationListener((ResourceObjectChangeListener) invalidator);
		changeNotificationDispatcher.unregisterNotificationListener((ResourceOperationListener) invalidator);
	}

	private <F extends ObjectType> int getMaxClicks(LensContext<F> context, OperationResult result) throws SchemaException, ObjectNotFoundException {
		PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
		Integer maxClicks = SystemConfigurationTypeUtil.getMaxModelClicks(systemConfiguration);
		if (maxClicks == null) {
			return DEFAULT_MAX_CLICKS;
		} else {
			return maxClicks;
		}
	}

	public <F extends ObjectType> HookOperationMode click(LensContext<F> context, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		
		// DO NOT CHECK CONSISTENCY of the context here. The context may not be fresh and consistent yet. Project will fix
		// that. Check consistency afterwards (and it is also checked inside projector several times).
		
		if (context.getDebugListener() == null) {
			context.setDebugListener(debugListener);
		}
		
		try {
			
			XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
			
			// We need to determine focus before auditing. Otherwise we will not know user
			// for the accounts (unless there is a specific delta for it).
			// This is ugly, but it is the easiest way now (TODO: cleanup).
			contextLoader.determineFocusContext((LensContext<? extends FocusType>) context, result);
			
			ModelState state = context.getState();
			if (state == ModelState.INITIAL) {
				if (debugListener != null) {
					debugListener.beforeSync(context);
				}
				metadataManager.applyRequestMetadata(context, now, task, result);
				context.getStats().setRequestTimestamp(now);
				// We need to do this BEFORE projection. If we would do that after projection
				// there will be secondary changes that are not part of the request.
				audit(context, AuditEventStage.REQUEST, task, result);
			}

			boolean recompute = false;
			if (!context.isFresh()) {
				LOGGER.trace("Context is not fresh -- forcing cleanup and recomputation");
				recompute = true;
			} else if (context.getExecutionWave() > context.getProjectionWave()) {		// should not occur
				LOGGER.warn("Execution wave is greater than projection wave -- forcing cleanup and recomputation");
				recompute = true;
			}

			if (recompute) {
				context.cleanup();
				projector.project(context, "PROJECTOR ("+state+")", task, result);
			} else if (context.getExecutionWave() == context.getProjectionWave()) {
				LOGGER.trace("Running projector for current execution wave");
				projector.resume(context, "PROJECTOR ("+state+")", task, result);
			} else {
				LOGGER.trace("Skipping projection because the context is fresh and projection for current wave has already run");
			}
			
			if (!context.isRequestAuthorized()) {
				authorizeContextRequest(context, task, result);
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
					HookOperationMode mode = processFinal(context, task, result);
					if (debugListener != null) {
						debugListener.afterSync(context);
					}
					return mode;
			}		
			result.recomputeStatus();
			result.cleanupResult();
			return invokeHooks(context, task, result);
			
		} catch (CommunicationException | ConfigurationException | ExpressionEvaluationException | ObjectNotFoundException |
				PolicyViolationException | SchemaException | SecurityViolationException | RuntimeException |
				ObjectAlreadyExistsException e) {
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
    private HookOperationMode invokeHooks(LensContext context, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
    	// TODO: following two parts should be merged together in later versions
    	
    	// Execute configured scripting hooks
    	PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
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
							LOGGER.error("Evaluation of {} failed: {}", shortDesc, e.getMessage(), e);
							throw new ExpressionEvaluationException("Evaluation of "+shortDesc+" failed: "+e.getMessage(), e);
						} catch (ObjectNotFoundException e) {
							LOGGER.error("Evaluation of {} failed: {}", shortDesc, e.getMessage(), e);
							throw new ObjectNotFoundException("Evaluation of "+shortDesc+" failed: "+e.getMessage(), e);
						} catch (SchemaException e) {
							LOGGER.error("Evaluation of {} failed: {}", shortDesc, e.getMessage(), e);
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
		
		Utils.evaluateScript(scriptExpression, context, variables, false, shortDesc, task, result);
		LOGGER.trace("Finished evaluation of {}", shortDesc);
	}

    private <F extends ObjectType> void processInitialToPrimary(LensContext<F> context, Task task, OperationResult result) {
		// Context loaded, nothing special do. Bump state to PRIMARY.
		context.setState(ModelState.PRIMARY);		
	}
	
	private <F extends ObjectType> void processPrimaryToSecondary(LensContext<F> context, Task task, OperationResult result) {
		// Nothing to do now. The context is already recomputed.
		context.setState(ModelState.SECONDARY);
	}
	
	private <F extends ObjectType> void processSecondary(LensContext<F> context, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {
		if (context.getExecutionWave() > context.getMaxWave() + 1) {
			context.setState(ModelState.FINAL);
			return;
		}
		
		Holder<Boolean> restartRequestedHolder = new Holder<>();
		
		LensUtil.partialExecute("execution",
				() -> { 
					boolean restartRequested = changeExecutor.executeChanges(context, task, result);
					restartRequestedHolder.setValue(restartRequested);
				}, 
				context.getPartialProcessingOptions()::getExecution);
		
		audit(context, AuditEventStage.EXECUTION, task, result);
		
		rotContextIfNeeded(context);

		boolean restartRequested = false;
		if (restartRequestedHolder.getValue() != null) {
			restartRequested = restartRequestedHolder.getValue();
		}
		
		if (!restartRequested) {
			// TODO what if restart is requested indefinitely?
			context.incrementExecutionWave();
		} else {
			// explicitly rot context?
		}
		
		LensUtil.traceContext(LOGGER, "CLOCKWORK (" + context.getState() + ")", "change execution", false, context, false);
	}
	
	/**
	 * Force recompute for the next execution wave. Recompute only those contexts that were changed.
	 * This is more intelligent than context.rot()
	 */
	private <F extends ObjectType> void rotContextIfNeeded(LensContext<F> context) throws SchemaException {
		boolean rot = false;
    	for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
    		if (projectionContext.getWave() != context.getExecutionWave()) {
    			LOGGER.trace("Context rot: projection {} NOT rotten because of wrong wave number", projectionContext);
        		continue;
			}
//			if (!projectionContext.isDoReconciliation()) {	// meaning volatility is NONE
//				LOGGER.trace("Context rot: projection {} NOT rotten because the resource is non-volatile", projectionContext);
//				continue;
//			}
    		ObjectDelta<ShadowType> execDelta = projectionContext.getExecutableDelta();
    		if (isShadowDeltaSignificant(execDelta)) {
    			
    			LOGGER.debug("Context rot: projection {} rotten because of executable delta {}", projectionContext, execDelta);
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
    			LOGGER.debug("Context rot: context rotten because of focus execution delta {}", execDelta);
    			rot = true;
    		}
    		if (rot) {
	    		// It is OK to refresh focus all the time there was any change. This is cheap.
	    		focusContext.setFresh(false);
    		}
    		//remove secondary deltas from other than execution wave - we need to recompute them..
//    		cleanUpSecondaryDeltas(context);
    		
    		
    	}
    	if (rot) {
    		context.setFresh(false);
    	}
	}

//	// TODO this is quite unclear. Originally here was keeping the delta from the current wave (plus delta from wave #1).
//	// The reason was not clear.
//	// Let us erase everything.
//	private <F extends ObjectType> void cleanUpSecondaryDeltas(LensContext<F> context){
//		LensFocusContext focusContext = context.getFocusContext();
//		ObjectDeltaWaves<F> executionWaveDeltaList = focusContext.getSecondaryDeltas();
//		executionWaveDeltaList.clear();
//	}
	
	private <P extends ObjectType> boolean isShadowDeltaSignificant(ObjectDelta<P> delta) {
		if (delta == null || delta.isEmpty()) {
			return false;
		}
		if (delta.isAdd() || delta.isDelete()) {
			return true;
		}
		Collection<? extends ItemDelta<?,?>> attrDeltas = delta.findItemDeltasSubPath(new ItemPath(ShadowType.F_ATTRIBUTES));
		if (attrDeltas != null && !attrDeltas.isEmpty()) {
			return true;
		}
		return false;
	}
	
	private <F extends ObjectType> HookOperationMode processFinal(LensContext<F> context, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {
		auditFinalExecution(context, task, result);
		logFinalReadable(context, task, result);
		recordOperationExecution(context, null, task, result);
		
		HookOperationMode opmode = personaProcessor.processPersonaChanges(context, task, result);
		if (opmode == HookOperationMode.BACKGROUND) {
			return opmode;
		}
		
        return triggerReconcileAffected(context, task, result);
	}

	private <F extends ObjectType> void recordOperationExecution(LensContext<F> context, Throwable clockworkException,
			Task task, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		try {
			LOGGER.trace("recordOperationExecution starting; task = {}, clockworkException = {}", task, clockworkException);
			recordFocusOperationExecution(context, now, clockworkException, task, result);
			for (LensProjectionContext projectionContext : context.getProjectionContexts()) {
				recordProjectionOperationExecution(context, projectionContext, now, task, result);
			}
		} catch (Throwable t) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't record operation execution. Model context:\n{}", t, context.debugDump());
			// Let us ignore this for the moment. It should not have happened, sure. But it's not that crucial.
			// Administrator will be able to learn about the problem from the log.
		}
	}

	private <F extends ObjectType> void recordFocusOperationExecution(LensContext<F> context, XMLGregorianCalendar now,
			Throwable clockworkException, Task task, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null || focusContext.isDelete()) {
			LOGGER.trace("focusContext is null or 'delete', not recording focus operation execution");
			return;
		}
		PrismObject<F> objectNew = focusContext.getObjectNew();
		Validate.notNull(objectNew, "No focus object even if the context is not of 'delete' type");
		List<LensObjectDeltaOperation<F>> executedDeltas;
		if (clockworkException == null) {
			executedDeltas = focusContext.getExecutedDeltas();
		} else {
			executedDeltas = new ArrayList<>(focusContext.getExecutedDeltas());
			LensObjectDeltaOperation<F> odo = new LensObjectDeltaOperation<>();
			ObjectDelta<F> primaryDelta = focusContext.getPrimaryDelta();
			if (primaryDelta != null) {
				odo.setObjectDelta(primaryDelta);
			} else {
				@SuppressWarnings({"unchecked", "raw"})
				Class<F> fClass = (Class<F>) objectNew.asObjectable().getClass();
				ObjectDelta<F> fakeDelta = new ObjectDelta<>(fClass, ChangeType.MODIFY, prismContext);
				odo.setObjectDelta(fakeDelta);
			}
			odo.setExecutionResult(result);		// we rely on the fact that 'result' already contains record of the exception
			executedDeltas.add(odo);
		}
		LOGGER.trace("recordFocusOperationExecution: executedDeltas: {}", executedDeltas.size());
		recordOperationExecution(objectNew, false, executedDeltas, now, context.getChannel(), task, result);
	}

	private <F extends ObjectType> void recordProjectionOperationExecution(LensContext<F> context,
			LensProjectionContext projectionContext, XMLGregorianCalendar now, Task task, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		PrismObject<ShadowType> object = projectionContext.getObjectAny();
		if (object == null) {
			return;			// this can happen
		}
		recordOperationExecution(object, true, projectionContext.getExecutedDeltas(), now,
				context.getChannel(), task, result);
	}

	private <F extends ObjectType> void recordOperationExecution(PrismObject<F> object, boolean deletedOk,
			List<LensObjectDeltaOperation<F>> executedDeltas, XMLGregorianCalendar now,
			String channel, Task task, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		OperationExecutionType operation = new OperationExecutionType(prismContext);
		OperationResult summaryResult = new OperationResult("dummy");
		String oid = object.getOid();
		for (LensObjectDeltaOperation<F> deltaOperation : executedDeltas) {
			operation.getOperation().add(createObjectDeltaOperation(deltaOperation));
			if (deltaOperation.getExecutionResult() != null) {
				summaryResult.addSubresult(deltaOperation.getExecutionResult());
			}
			if (oid == null && deltaOperation.getObjectDelta() != null) {
				oid = deltaOperation.getObjectDelta().getOid();
			}
		}
		if (oid == null) {        // e.g. if there is an exception in provisioning.addObject method
			LOGGER.trace("recordOperationExecution: skipping because oid is null for object = {}", object);
			return;
		}
		summaryResult.computeStatus();
		OperationResultStatusType overallStatus = summaryResult.getStatus().createStatusType();
		setOperationContext(operation, overallStatus, now, channel, task);
		storeOperationExecution(object, oid, operation, deletedOk, result);
	}

	private <F extends ObjectType> void storeOperationExecution(@NotNull PrismObject<F> object, @NotNull String oid,
			@NotNull OperationExecutionType executionToAdd, boolean deletedOk, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		Integer recordsToKeep;
		Long deleteBefore;
		boolean keepNoExecutions = false;
		PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
		if (systemConfiguration != null && systemConfiguration.asObjectable().getCleanupPolicy() != null
				&& systemConfiguration.asObjectable().getCleanupPolicy().getObjectResults() != null) {
			CleanupPolicyType policy = systemConfiguration.asObjectable().getCleanupPolicy().getObjectResults();
			recordsToKeep = policy.getMaxRecords();
			if (recordsToKeep != null && recordsToKeep == 0) {
				LOGGER.trace("objectResults.recordsToKeep is 0, will skip storing operationExecutions");
				keepNoExecutions = true;
			}
			if (policy.getMaxAge() != null) {
				XMLGregorianCalendar limit = XmlTypeConverter.addDuration(
						XmlTypeConverter.createXMLGregorianCalendar(new Date()), policy.getMaxAge().negate());
				deleteBefore = XmlTypeConverter.toMillis(limit);
			} else {
				deleteBefore = null;
			}
		} else {
			recordsToKeep = DEFAULT_NUMBER_OF_RESULTS_TO_KEEP;
			deleteBefore = null;
		}
		List<OperationExecutionType> executionsToDelete = new ArrayList<>();
		List<OperationExecutionType> executions = new ArrayList<>(object.asObjectable().getOperationExecution());
		// delete all executions related to current task and all old ones
		String taskOid = executionToAdd.getTaskRef() != null ? executionToAdd.getTaskRef().getOid() : null;
		for (Iterator<OperationExecutionType> iterator = executions.iterator(); iterator.hasNext(); ) {
			OperationExecutionType execution = iterator.next();
			if (taskOid != null && execution.getTaskRef() != null && taskOid.equals(execution.getTaskRef().getOid())
					|| deleteBefore != null && XmlTypeConverter.toMillis(execution.getTimestamp()) < deleteBefore) {
				executionsToDelete.add(execution);
				iterator.remove();
			}
		}
		// delete all surplus executions
		if (recordsToKeep != null && object.asObjectable().getOperationExecution().size() > recordsToKeep - 1) {
			if (keepNoExecutions) {
				executionsToDelete.addAll(executions);
			} else {
				executions.sort(Comparator.nullsFirst(Comparator.comparing(e -> XmlTypeConverter.toDate(e.getTimestamp()))));
				executionsToDelete.addAll(executions.subList(0, executions.size() - (recordsToKeep - 1)));
			}
		}
		// construct and execute the delta
		Class<? extends ObjectType> objectClass = object.asObjectable().getClass();
		List<ItemDelta<?, ?>> deltas = new ArrayList<>();
		if (!keepNoExecutions) {
			deltas.add(DeltaBuilder.deltaFor(objectClass, prismContext)
					.item(ObjectType.F_OPERATION_EXECUTION)
					.add(executionToAdd)
					.asItemDelta());
		}
		if (!executionsToDelete.isEmpty()) {
			deltas.add(DeltaBuilder.deltaFor(objectClass, prismContext)
					.item(ObjectType.F_OPERATION_EXECUTION)
					.delete(PrismContainerValue.toPcvList(CloneUtil.cloneCollectionMembers(executionsToDelete)))
					.asItemDelta());
		}
		LOGGER.trace("Operation execution delta:\n{}", DebugUtil.debugDumpLazily(deltas));
		try {
			if (!deltas.isEmpty()) {
				repositoryService.modifyObject(objectClass, oid, deltas, result);
			}
		} catch (ObjectNotFoundException e) {
			if (!deletedOk) {
				throw e;
			} else {
				LOGGER.trace("Object {} deleted but this was expected.", oid);
				result.deleteLastSubresultIfError();
			}
		}
	}

	private void setOperationContext(OperationExecutionType operation,
			OperationResultStatusType overallStatus, XMLGregorianCalendar now, String channel, Task task) {
		if (task.getParentForLightweightAsynchronousTask() != null) {
			task = task.getParentForLightweightAsynchronousTask();
		}
		if (task.isPersistent()) {
			operation.setTaskRef(ObjectTypeUtil.createObjectRef(task.getTaskPrismObject()));
		}
		operation.setStatus(overallStatus);
		operation.setInitiatorRef(ObjectTypeUtil.createObjectRef(task.getOwner()));		// TODO what if the real initiator is different? (e.g. when executing approved changes)
		operation.setChannel(channel);
		operation.setTimestamp(now);
	}

	private <F extends ObjectType> ObjectDeltaOperationType createObjectDeltaOperation(LensObjectDeltaOperation<F> deltaOperation) {
		ObjectDeltaOperationType odo;
		try {
			odo = simplifyOperation(deltaOperation).toLensObjectDeltaOperationType().getObjectDeltaOperation();
		} catch (SchemaException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create operation information", e);
			odo = new ObjectDeltaOperationType();
			OperationResult r = new OperationResult(Clockwork.class.getName() + ".createObjectDeltaOperation");
			r.recordFatalError("Couldn't create operation information: " + e.getMessage(), e);
			odo.setExecutionResult(r.createOperationResultType());
		}
		return odo;
	}

	private <F extends ObjectType> LensObjectDeltaOperation<F> simplifyOperation(ObjectDeltaOperation<F> operation) {
		LensObjectDeltaOperation<F> rv = new LensObjectDeltaOperation<>();
		rv.setObjectDelta(simplifyDelta(operation.getObjectDelta()));
		rv.setExecutionResult(simplifyResult(operation.getExecutionResult()));
		rv.setObjectName(operation.getObjectName());
		rv.setResourceName(operation.getResourceName());
		rv.setResourceOid(operation.getResourceOid());
		return rv;
	}

	private OperationResult simplifyResult(OperationResult result) {
		return new OperationResult(result.getOperation(), result.getStatus(), result.getMessageCode(), result.getMessage());
	}

	private <F extends ObjectType> ObjectDelta<F> simplifyDelta(ObjectDelta<F> delta) {
		return new ObjectDelta<>(delta.getObjectTypeClass(), delta.getChangeType(), prismContext);
	}

	private <F extends ObjectType> HookOperationMode triggerReconcileAffected(LensContext<F> context, Task task, OperationResult result) throws SchemaException {
        // check applicability
        if (!ModelExecuteOptions.isReconcileAffected(context.getOptions())) {
            return HookOperationMode.FOREGROUND;
        }
        if (context.getFocusClass() == null || !RoleType.class.isAssignableFrom(context.getFocusClass())) {
            LOGGER.warn("ReconcileAffected requested but not available for {}. Doing nothing.", context.getFocusClass());
            return HookOperationMode.FOREGROUND;
        }

        // check preconditions
        if (context.getFocusContext() == null) {
            throw new IllegalStateException("No focus context when expected it");
        }
        PrismObject<RoleType> role = (PrismObject) context.getFocusContext().getObjectAny();
        if (role == null) {
            throw new IllegalStateException("No role when expected it");
        }

        // preparing the recompute/reconciliation task
        Task reconTask;
        if (task.isPersistent()) {
            reconTask = task.createSubtask();
        } else {
            reconTask = task;
        }
        assert !reconTask.isPersistent();

        // creating object query
        PrismPropertyDefinition propertyDef = prismContext.getSchemaRegistry()
                .findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);
        PrismReferenceValue referenceValue = new PrismReferenceValue(context.getFocusContext().getOid(), RoleType.COMPLEX_TYPE);
        ObjectFilter refFilter = QueryBuilder.queryFor(FocusType.class, prismContext)
				.item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(referenceValue)
				.buildFilter();
        SearchFilterType filterType = QueryConvertor.createSearchFilterType(refFilter, prismContext);
        QueryType queryType = new QueryType();
        queryType.setFilter(filterType);
        PrismProperty<QueryType> property = propertyDef.instantiate();
        property.setRealValue(queryType);
        reconTask.addExtensionProperty(property);

        // other parameters
        reconTask.setName("Recomputing users after changing role " + role.asObjectable().getName());
        reconTask.setBinding(TaskBinding.LOOSE);
        reconTask.setInitialExecutionStatus(TaskExecutionStatus.RUNNABLE);
        reconTask.setHandlerUri(RecomputeTaskHandler.HANDLER_URI);
        reconTask.setCategory(TaskCategory.RECOMPUTATION);
        taskManager.switchToBackground(reconTask, result);
		result.setBackgroundTaskOid(reconTask.getOid());
        result.recordStatus(OperationResultStatus.IN_PROGRESS, "Reconciliation task switched to background");
        return HookOperationMode.BACKGROUND;
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
	
	private <F extends ObjectType> void processClockworkException(LensContext<F> context, Exception e, Task task, OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		LOGGER.trace("Processing clockwork exception {}", e.toString());
		result.recordFatalError(e);
		auditEvent(context, AuditEventStage.EXECUTION, null, true, task, result);
		recordOperationExecution(context, e, task, result);
		reclaimSequences(context, task, result);
	}

	private <F extends ObjectType> void auditEvent(LensContext<F> context, AuditEventStage stage, 
			XMLGregorianCalendar timestamp, boolean alwaysAudit, Task task, OperationResult result) throws SchemaException {
		
		PrismObject<? extends ObjectType> primaryObject;
		ObjectDelta<? extends ObjectType> primaryDelta;
		if (context.getFocusContext() != null) {
			primaryObject = context.getFocusContext().getObjectOld();
			if (primaryObject == null) {
				primaryObject = context.getFocusContext().getObjectNew();
			}
			primaryDelta = context.getFocusContext().getDelta();
		} else {
			Collection<LensProjectionContext> projectionContexts = context.getProjectionContexts();
			if (projectionContexts == null || projectionContexts.isEmpty()) {
				throw new IllegalStateException("No focus and no projections in "+context);
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
		
		AuditEventType eventType;
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
			Collection<ObjectDeltaOperation<? extends ObjectType>> clonedDeltas = ObjectDeltaOperation.cloneDeltaCollection(context.getPrimaryChanges());
			checkNamesArePresent(clonedDeltas, primaryObject);
			auditRecord.addDeltas(clonedDeltas);
			if (auditRecord.getTarget() == null) {
				auditRecord.setTarget(Utils.determineAuditTargetDeltaOps(clonedDeltas));
			}
		} else if (stage == AuditEventStage.EXECUTION) {
			auditRecord.setOutcome(result.getComputeStatus());
			Collection<ObjectDeltaOperation<? extends ObjectType>> unauditedExecutedDeltas = context.getUnauditedExecutedDeltas();
			if (!alwaysAudit && (unauditedExecutedDeltas == null || unauditedExecutedDeltas.isEmpty())) {
				// No deltas, nothing to audit in this wave
				return;
			}
			Collection<ObjectDeltaOperation<? extends ObjectType>> clonedDeltas = ObjectDeltaOperation.cloneCollection(unauditedExecutedDeltas);
			checkNamesArePresent(clonedDeltas, primaryObject);
			auditRecord.addDeltas(clonedDeltas);
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

	private void checkNamesArePresent(Collection<ObjectDeltaOperation<? extends ObjectType>> deltas, PrismObject<? extends ObjectType> primaryObject) {
		if (primaryObject != null) {
            for (ObjectDeltaOperation<? extends ObjectType> delta : deltas) {
                if (delta.getObjectName() == null) {
                    delta.setObjectName(primaryObject.getName());
                }
            }
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
	
	private <F extends ObjectType> void authorizeContextRequest(LensContext<F> context, Task task, OperationResult parentResult) throws SecurityViolationException, SchemaException, ObjectNotFoundException {
		OperationResult result = parentResult.createMinorSubresult(Clockwork.class.getName()+".authorizeRequest");
		LOGGER.trace("Authorizing request");
		try {
			
			final LensFocusContext<F> focusContext = context.getFocusContext();
			OwnerResolver ownerResolver = new LensOwnerResolver<>(context, objectResolver, task, result);
			if (focusContext != null) {
				authorizeElementContext(context, focusContext, ownerResolver, true, task, result);
			}
			for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
				authorizeElementContext(context, projectionContext, ownerResolver, false, task, result);
			}
			context.setRequestAuthorized(true);
			result.recordSuccess();
			
			LOGGER.trace("Request authorized");
			
		} catch (Throwable e) {
			result.recordFatalError(e);
			throw e;
		}
	}
	
	private <F extends ObjectType, O extends ObjectType> ObjectSecurityConstraints authorizeElementContext(LensContext<F> context, LensElementContext<O> elementContext,
			OwnerResolver ownerResolver, boolean isFocus, Task task, OperationResult result) throws SecurityViolationException, SchemaException, ObjectNotFoundException {
		ObjectDelta<O> primaryDelta = elementContext.getPrimaryDelta();
		// If there is no delta then there is no request to authorize
		if (primaryDelta != null) {
			primaryDelta = primaryDelta.clone();
			PrismObject<O> object = elementContext.getObjectCurrent();
			if (object == null) {
				// This may happen when object is being added. 
				// But also in cases such as assignment of account and modification of 
				// the same account in one operation
				object = elementContext.getObjectNew();
			}
			String operationUrl = ModelUtils.getOperationUrlFromDelta(primaryDelta);
			ObjectSecurityConstraints securityConstraints = securityEnforcer.compileSecurityConstraints(object, ownerResolver);
			if (securityConstraints == null) {
				throw new AuthorizationException("Access denied");
			}

			if (isFocus) {
				// Process assignments first. If the assignments are allowed then we
				// have to ignore the assignment item in subsequent security checks
				ContainerDelta<Containerable> assignmentDelta = primaryDelta.findContainerDelta(FocusType.F_ASSIGNMENT);
				if (assignmentDelta != null) {
					AuthorizationDecisionType assignmentItemDecision = securityConstraints.findItemDecision(new ItemPath(FocusType.F_ASSIGNMENT),
							operationUrl, getRequestAuthorizationPhase(context));
					if (assignmentItemDecision == AuthorizationDecisionType.ALLOW) {
						// Nothing to do, operation is allowed for all values
						LOGGER.debug("Allow assignment/unassignment to {} becasue access to assignment container is explicitly allowed", object);
					} else if (assignmentItemDecision == AuthorizationDecisionType.DENY) {
						LOGGER.debug("Deny assignment/unassignment to {} becasue access to assignment container is explicitly denied", object);
						throw new AuthorizationException("Access denied");
					} else {
						AuthorizationDecisionType actionDecision = securityConstraints.getActionDecision(operationUrl, getRequestAuthorizationPhase(context));
						if (actionDecision == AuthorizationDecisionType.ALLOW) {
							// Nothing to do, operation is allowed for all values
						} else if (actionDecision == AuthorizationDecisionType.DENY) {
							throw new AuthorizationException("Access denied");
						} else {
							// No explicit decision for assignment modification yet
							// process each assignment individually							
							authorizeAssignmentRequest(context, ModelAuthorizationAction.ASSIGN.getUrl(), 
									object, ownerResolver, PlusMinusZero.PLUS, true, task, result);
							
							// We want to allow unassignment even if there are policies. Otherwise we would not be able to get
							// rid of that assignment
							authorizeAssignmentRequest(context, ModelAuthorizationAction.UNASSIGN.getUrl(), 
									object, ownerResolver,PlusMinusZero.MINUS, false, task, result);
						}
					}
					// assignments were authorized explicitly. Therefore we need to remove them from primary delta to avoid another
					// authorization
					if (primaryDelta.isAdd()) {
						PrismObject<O> objectToAdd = primaryDelta.getObjectToAdd();
						objectToAdd.removeContainer(FocusType.F_ASSIGNMENT);
					} else if (primaryDelta.isModify()) {
						primaryDelta.removeContainerModification(FocusType.F_ASSIGNMENT);
					}
				}
			}

			// Process credential changes explicitly. There is a special authorization for that.
			
			if (!primaryDelta.isDelete()) {
				if (primaryDelta.isAdd()) {
					PrismObject<O> objectToAdd = primaryDelta.getObjectToAdd();
					PrismContainer<CredentialsType> credentialsContainer = objectToAdd.findContainer(UserType.F_CREDENTIALS);
					if (credentialsContainer != null) {
						for (Item<?,?> item: credentialsContainer.getValue().getItems()) {
							ContainerDelta<?> cdelta = new ContainerDelta(item.getPath(), (PrismContainerDefinition)item.getDefinition(), prismContext);
							cdelta.addValuesToAdd(((PrismContainer)item).getValue().clone());
							AuthorizationDecisionType cdecision = evaluateCredentialDecision(context, securityConstraints, cdelta);
							LOGGER.trace("AUTZ: credential add {} decision: {}", item.getPath(), cdecision);
							if (cdecision == AuthorizationDecisionType.ALLOW) {
								// Remove it from primary delta, so it will not be evaluated later
								objectToAdd.removeContainer(item.getPath());
							} else if (cdecision == AuthorizationDecisionType.DENY) {
								throw new AuthorizationException("Access denied");
							} else {
								// Do nothing. The access will be evaluated later in a normal way
							}
						}
					}
				} else {
					// modify
					Collection<? extends ItemDelta<?, ?>> credentialChanges = primaryDelta.findItemDeltasSubPath(new ItemPath(UserType.F_CREDENTIALS));
					for (ItemDelta credentialChange: credentialChanges) {
						AuthorizationDecisionType cdecision = evaluateCredentialDecision(context, securityConstraints, credentialChange);
						LOGGER.trace("AUTZ: credential delta {} decision: {}", credentialChange.getPath(), cdecision);
						if (cdecision == AuthorizationDecisionType.ALLOW) {
							// Remove it from primary delta, so it will not be evaluated later
							primaryDelta.removeModification(credentialChange);
						} else if (cdecision == AuthorizationDecisionType.DENY) {
							throw new AuthorizationException("Access denied");
						} else {
							// Do nothing. The access will be evaluated later in a normal way
						}
					}
				}
			}

			if (primaryDelta != null && !primaryDelta.isEmpty()) {
				// TODO: optimize, avoid evaluating the constraints twice
				securityEnforcer.authorize(operationUrl, getRequestAuthorizationPhase(context) , object, primaryDelta, null, ownerResolver, result);
			}
			
			return securityConstraints;
		} else {
			return null;
		}
	}

	private <F extends ObjectType> AuthorizationPhaseType getRequestAuthorizationPhase(LensContext<F> context) {
		if (context.isExecutionPhaseOnly()) {
			return AuthorizationPhaseType.EXECUTION;
		} else {
			return AuthorizationPhaseType.REQUEST;
		}
	}

	private <F extends ObjectType> AuthorizationDecisionType evaluateCredentialDecision(LensContext<F> context, ObjectSecurityConstraints securityConstraints, ItemDelta credentialChange) {
		return securityConstraints.findItemDecision(credentialChange.getPath(),
				ModelAuthorizationAction.CHANGE_CREDENTIALS.getUrl(), getRequestAuthorizationPhase(context));
	}

	private <F extends ObjectType,O extends ObjectType> void authorizeAssignmentRequest(LensContext<F> context, String assignActionUrl, PrismObject<O> object,
			OwnerResolver ownerResolver, PlusMinusZero plusMinusZero, boolean prohibitPolicies, Task task, OperationResult result) throws SecurityViolationException, SchemaException, ObjectNotFoundException {
		// This is *request* authorization. Therefore we care only about primary delta.
		ObjectDelta<F> focusPrimaryDelta = context.getFocusContext().getPrimaryDelta();
		if (focusPrimaryDelta == null) {
			return;
		}
		ContainerDelta<AssignmentType> focusAssignmentDelta = focusPrimaryDelta.findContainerDelta(FocusType.F_ASSIGNMENT);
		if (focusAssignmentDelta == null) {
			return;
		}
		Collection<PrismContainerValue<AssignmentType>> changedAssignmentValues = focusAssignmentDelta.getValueChanges(plusMinusZero);
		for (PrismContainerValue<AssignmentType> changedAssignmentValue: changedAssignmentValues) {
			AssignmentType changedAssignment = changedAssignmentValue.getRealValue();
			ObjectReferenceType targetRef = changedAssignment.getTargetRef();
			if (targetRef == null || targetRef.getOid() == null) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} of non-target assignment denied", 
							assignActionUrl.substring(assignActionUrl.lastIndexOf('#') + 1));
				}
				securityEnforcer.failAuthorization("with assignment", getRequestAuthorizationPhase(context), object, null, null, result);
			}
			// We do not worry about performance here too much. The target was already evaluated. This will be retrieved from repo cache anyway.
			PrismObject<ObjectType> target = objectResolver.resolve(targetRef.asReferenceValue(), "resolving assignment target", task, result);
			
			if (prohibitPolicies) {
				if (changedAssignment.getPolicyRule() != null || !changedAssignment.getPolicyException().isEmpty() || !changedAssignment.getPolicySituation().isEmpty()) {
					securityEnforcer.failAuthorization("with assignment because of policies in the assignment", getRequestAuthorizationPhase(context), object, null, target, result);
				}
			}
			
			ObjectDelta<O> assignmentObjectDelta = object.createModifyDelta();
			ContainerDelta<AssignmentType> assignmentDelta = assignmentObjectDelta.createContainerModification(FocusType.F_ASSIGNMENT);
			// We do not care if this is add or delete. All that matters for authorization is that it is in a delta.
			assignmentDelta.addValuesToAdd(changedAssignment.asPrismContainerValue().clone());
			if (securityEnforcer.isAuthorized(assignActionUrl, getRequestAuthorizationPhase(context), object, assignmentObjectDelta, target, ownerResolver)) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} of target {} to {} allowed with {} authorization", 
							assignActionUrl.substring(assignActionUrl.lastIndexOf('#') + 1),
							target, object, assignActionUrl);
				}
				continue;
			}
			QName relation = targetRef.getRelation();
			if (ObjectTypeUtil.isDelegationRelation(relation)) {
				if (securityEnforcer.isAuthorized(ModelAuthorizationAction.DELEGATE.getUrl(), getRequestAuthorizationPhase(context), object, assignmentObjectDelta, target, ownerResolver)) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("{} of target {} to {} allowed with {} authorization", 
							assignActionUrl.substring(assignActionUrl.lastIndexOf('#') + 1),
							target, object, ModelAuthorizationAction.DELEGATE.getUrl());
					}
					continue;
				}
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("{} of target {} to {} denied", 
						assignActionUrl.substring(assignActionUrl.lastIndexOf('#')),
						target, object);
			}
			securityEnforcer.failAuthorization("with assignment", getRequestAuthorizationPhase(context), object, null, target, result);
		}
	}

	private <F extends ObjectType> void reclaimSequences(LensContext<F> context, Task task, OperationResult result) throws SchemaException {
		Map<String, Long> sequenceMap = context.getSequences();
		LOGGER.trace("Context sequence map: {}", sequenceMap);
		for (Entry<String, Long> sequenceMapEntry: sequenceMap.entrySet()) {
			Collection<Long> unusedValues = new ArrayList<>(1);
			unusedValues.add(sequenceMapEntry.getValue());
			try {
				LOGGER.trace("Returning value {} to sequence {}", sequenceMapEntry.getValue(), sequenceMapEntry.getKey());
				repositoryService.returnUnusedValuesToSequence(sequenceMapEntry.getKey(), unusedValues, result);
			} catch (ObjectNotFoundException e) {
				LOGGER.error("Cannot return unused value to sequence {}: it does not exist", sequenceMapEntry.getKey(), e);
				// ... but otherwise ignore it and go on
			}
		}
	}

}
