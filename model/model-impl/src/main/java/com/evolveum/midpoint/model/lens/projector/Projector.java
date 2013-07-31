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
package com.evolveum.midpoint.model.lens.projector;

import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensObjectDeltaOperation;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.prism.PrismObject;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectTypeDependencyStrictnessType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectTypeDependencyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import static com.evolveum.midpoint.common.InternalsConfig.consistencyChecks;

/**
 * Projector recomputes the context. It takes the context with a few basic data as input. It uses all the policies 
 * and mappings to derive all the other data. E.g. a context with only a user (primary) delta. It applies user template,
 * outbound mappings and the inbound mappings and then inbound and outbound mappings of other accounts and so on until
 * all the data are computed. The output is the original context with all the computed delta.
 * 
 * Primary deltas are in the input, secondary deltas are computed in projector. Projector "projects" primary deltas to
 * the secondary deltas of user and accounts.
 * 
 * Projector does NOT execute the deltas. It only recomputes the context. It may read a lot of objects (user, accounts, policies).
 * But it does not change any of them.
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class Projector {
	
	@Autowired(required = true)
	private ContextLoader contextLoader;
	
	@Autowired(required = true)
    private UserPolicyProcessor userPolicyProcessor;

    @Autowired(required = true)
    private AssignmentProcessor assignmentProcessor;

    @Autowired(required = true)
    private InboundProcessor inboundProcessor;
    
    @Autowired(required = true)
    private AccountValuesProcessor accountValuesProcessor;

    @Autowired(required = true)
    private ReconciliationProcessor reconciliationProcessor;

    @Autowired(required = true)
    private CredentialsProcessor credentialsProcessor;

    @Autowired(required = true)
    private ActivationProcessor activationProcessor;
    
    @Autowired(required = true)
    private Clock clock;
	
	private static final Trace LOGGER = TraceManager.getTrace(Projector.class);
	
	public <F extends ObjectType, P extends ShadowType> void project(LensContext<F,P> context, String activityDescription, 
			OperationResult parentResult) 
			throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, 
			ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		
		LOGGER.debug("Projecting context {}", context);
		
		LensUtil.traceContext(LOGGER, activityDescription, "projector start", false, context, false);
		
		if (consistencyChecks) context.checkConsistence();
		
		context.normalize();
		context.resetProjectionWave();
		
		OperationResult result = parentResult.createSubresult(Projector.class.getName() + ".project");
		result.addContext("executionWave", context.getExecutionWave());
		
		// Read the time at the beginning so all processors have the same notion of "now"
		// this provides nicer unified timestamp that can be used in equality checks in tests and also for
		// troubleshooting
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		
		// Projector is using a set of "processors" to do parts of its work. The processors will be called in sequence
		// in the following code.
		
		try {
		
			contextLoader.load(context, activityDescription, result);
			// Set the "fresh" mark now so following consistency check will be stricter
			context.setFresh(true);
			if (consistencyChecks) context.checkConsistence();
			
			sortAccountsToWaves(context);
	        // Let's do one extra wave with no accounts in it. This time we expect to get the results of the execution to the user
	        // via inbound, e.g. identifiers generated by the resource, DNs and similar things. Hence the +2 instead of +1
	        int maxWaves = context.getMaxWave() + 2;
	        // Note that the number of waves may be recomputed as new accounts appear in the context (usually after assignment step).
	                
	        // Start the waves ....
	        LOGGER.trace("Starting the waves. There will be {} waves (or so we think now)", maxWaves);
	        context.setProjectionWave(0);
	        while (context.getProjectionWave() < maxWaves) {
	    
	        	// Process the user-related aspects of the context. That means inbound, user policy
	        	// and assignments.
	        	
	        	if (consistencyChecks) context.checkConsistence();
		        // Loop through the account changes, apply inbound expressions
		        inboundProcessor.processInbound(context, result);
		        if (consistencyChecks) context.checkConsistence();
		        context.recomputeFocus();
		        LensUtil.traceContext(LOGGER, activityDescription, "inbound", false, context, false);
		        if (consistencyChecks) context.checkConsistence();
		
		        userPolicyProcessor.processUserPolicy(context, now, result);
		        context.recomputeFocus();
		        LensUtil.traceContext(LOGGER, activityDescription,"user policy", false, context, false);
		        if (consistencyChecks) context.checkConsistence();
		        
		        checkContextSanity(context, "inbound and user policy", result);
		
		        assignmentProcessor.processAssignmentsProjections(context, result);
		        assignmentProcessor.processOrgAssignments(context, result);
		        context.recompute();
		        sortAccountsToWaves(context);
		        maxWaves = context.getMaxWave() + 2;
		        LensUtil.traceContext(LOGGER, activityDescription,"assignments", false, context, true);
		        if (consistencyChecks) context.checkConsistence();
		        
		        assignmentProcessor.checkForAssignmentConflicts(context, result);
		
		        // User-related processing is over. Now we will process accounts in a loop.
		        for (LensProjectionContext<P> projectionContext: context.getProjectionContexts()) {
		        	if (projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN ||
		        			projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE) {
						continue;
		        	}
		        	String projectionDesc;
		        	if (projectionContext.getResource() != null) {
		        		projectionDesc = projectionContext.getResource() + "("+projectionContext.getResourceShadowDiscriminator().getIntent()+")";		        		
		        	} else {
		        		ResourceShadowDiscriminator discr = projectionContext.getResourceShadowDiscriminator();
		        		if (discr != null) {
		        			projectionDesc = projectionContext.getResourceShadowDiscriminator().toString();
		        		} else {
		        			projectionDesc = "(UNKNOWN)";
		        		}
		        	}
		        	
		        	if (projectionContext.getWave() != context.getProjectionWave()) {
		        		// Let's skip accounts that do not belong into this wave.
		        		continue;
		        	}
		        	if (!projectionContext.isShadow()) {
		        		continue;
		        	}
		        	
		        	if (consistencyChecks) context.checkConsistence();
		        	
		        	activationProcessor.processActivation(context, projectionContext, now, result);
		        	
		        	projectionContext.recompute();
		        	
		        	LensUtil.traceContext(LOGGER, activityDescription, "activation of "+projectionDesc, false, context, false);
		        	
		        	if (projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN ||
		        			projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE) {
						continue;
		        	}
		        	
		        	if (!checkDependencies(context, projectionContext, now, result)) {
		        		continue;
		        	}
		        	
		        	// TODO: decide if we need to continue
		        	
		        	// This is a "composite" processor. it contains several more processor invocations inside
		        	accountValuesProcessor.process(context, projectionContext, activityDescription, result);
		        	
		        	if (consistencyChecks) context.checkConsistence();
		        	
		        	projectionContext.recompute();
		        	//SynchronizerUtil.traceContext("values", context, false);
		        	if (consistencyChecks) context.checkConsistence();
		        	
		        	credentialsProcessor.processCredentials(context, projectionContext, result);
		        	
		        	//SynchronizerUtil.traceContext("credentials", context, false);
		        	if (consistencyChecks) context.checkConsistence();
			        
		        	projectionContext.recompute();
		        	LensUtil.traceContext(LOGGER, activityDescription, "values and credentials of "+projectionDesc, false, context, true);
			        if (consistencyChecks) context.checkConsistence();
			
			        reconciliationProcessor.processReconciliation(context, projectionContext, result);
			        projectionContext.recompute();
			        LensUtil.traceContext(LOGGER, activityDescription, "reconciliation of "+projectionDesc, false, context, false);
			        if (consistencyChecks) context.checkConsistence();
		        }
		        
		        if (consistencyChecks) context.checkConsistence();
		        
		        context.incrementProjectionWave();
	        }
	        
	        if (consistencyChecks) context.checkConsistence();
	        
	        result.recordSuccess();
	        result.cleanupResult();
	        
		} catch (SchemaException e) {
			recordFatalError(e, result);
			throw e;
		} catch (PolicyViolationException e) {
			recordFatalError(e, result);
			throw e;
		} catch (ExpressionEvaluationException e) {
			recordFatalError(e, result);
			throw e;
		} catch (ObjectNotFoundException e) {
			recordFatalError(e, result);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			recordFatalError(e, result);
			throw e;
		} catch (CommunicationException e) {
			recordFatalError(e, result);
			throw e;
		} catch (ConfigurationException e) {
			recordFatalError(e, result);
			throw e;
		} catch (SecurityViolationException e) {
			recordFatalError(e, result);
			throw e;
		} catch (RuntimeException e) {
			recordFatalError(e, result);
			// This should not normally happen unless there is something really bad or there is a bug.
			// Make sure that it is logged.
			LOGGER.error("Runtime error in projector: {}", e.getMessage(), e);
			throw e;
		}
		
	}

	private void recordFatalError(Exception e, OperationResult result) {
		result.recordFatalError(e);
		result.cleanupResult(e);
	}

	public <F extends ObjectType, P extends ObjectType> void sortAccountsToWaves(LensContext<F,P> context) throws PolicyViolationException {
		for (LensProjectionContext<P> projectionContext: context.getProjectionContexts()) {
			determineAccountWave(context, projectionContext);
		}
	}
	
	// TODO: check for circular dependencies
	private <F extends ObjectType, P extends ObjectType> void determineAccountWave(LensContext<F,P> context, LensProjectionContext<P> accountContext) throws PolicyViolationException {
		if (accountContext.getWave() >= 0) {
			// This was already processed
			return;
		}
		int wave = 0;
		for (ResourceObjectTypeDependencyType dependency :accountContext.getDependencies()) {
			ResourceShadowDiscriminator refRat = new ResourceShadowDiscriminator(dependency);
			LensProjectionContext<P> dependencyAccountContext = context.findProjectionContext(refRat);
			if (dependencyAccountContext == null) {
				if (dependency.getStrictness() == null || dependency.getStrictness() == ResourceObjectTypeDependencyStrictnessType.STRICT) {
					throw new PolicyViolationException("Unsatisfied strict dependency of account "+accountContext.getResourceShadowDiscriminator()+
						" dependent on "+refRat+": Account not provisioned");
				} else if (dependency.getStrictness() == ResourceObjectTypeDependencyStrictnessType.LAX) {
					// independent object not in the context, just ignore it
					LOGGER.debug("Unsatisfied lax dependency of account "+accountContext.getResourceShadowDiscriminator()+
						" dependent on "+refRat+"; depencency skipped");
				} else if (dependency.getStrictness() == ResourceObjectTypeDependencyStrictnessType.RELAXED) {
					// independent object not in the context, just ignore it
					LOGGER.debug("Unsatisfied relaxed dependency of account "+accountContext.getResourceShadowDiscriminator()+
						" dependent on "+refRat+"; depencency skipped");
				} else {
					throw new IllegalArgumentException("Unknown dependency strictness "+dependency.getStrictness()+" in "+refRat);
				}
			} else {
				determineAccountWave(context, dependencyAccountContext);
				if (dependencyAccountContext.getWave() + 1 > wave) {
					wave = dependencyAccountContext.getWave() + 1;
				}
			}
		}
//		LOGGER.trace("Wave for {}: {}", accountContext.getResourceAccountType(), wave);
		accountContext.setWave(wave);
	}
	
	private <F extends ObjectType, P extends ShadowType> boolean checkDependencies(LensContext<F,P> context, 
    		LensProjectionContext<P> accountContext, XMLGregorianCalendar now, OperationResult result) throws PolicyViolationException {
		for (ResourceObjectTypeDependencyType dependency: accountContext.getDependencies()) {
			ResourceShadowDiscriminator refRat = new ResourceShadowDiscriminator(dependency);
			LensProjectionContext<P> dependencyAccountContext = context.findProjectionContext(refRat);
			if (dependencyAccountContext == null) {
				if (accountContext.isDelete()) {
					// It is OK if we depend on something that is not there if we are being removed
					return true;
				}
				if (dependency.getStrictness() == null || dependency.getStrictness() == ResourceObjectTypeDependencyStrictnessType.STRICT) {
					// This should not happen, it is checked before projection
					throw new PolicyViolationException("Unsatisfied strict dependency of account "+accountContext.getResourceShadowDiscriminator()+
						" dependent on "+refRat+": No context in dependency check");
				} else if (dependency.getStrictness() == ResourceObjectTypeDependencyStrictnessType.LAX) {
					// independent object not in the context, just ignore it
					LOGGER.trace("Unsatisfied lax dependency of account "+accountContext.getResourceShadowDiscriminator()+
						" dependent on "+refRat+"; depencency skipped");
				} else if (dependency.getStrictness() == ResourceObjectTypeDependencyStrictnessType.RELAXED) {
					// independent object not in the context, just ignore it
					LOGGER.trace("Unsatisfied relaxed dependency of account "+accountContext.getResourceShadowDiscriminator()+
						" dependent on "+refRat+"; depencency skipped");
				} else {
					throw new IllegalArgumentException("Unknown dependency strictness "+dependency.getStrictness()+" in "+refRat);
				}
			} else {
				// We have the context of the object that we depend on. We need to check if it was provisioned.
				if (dependency.getStrictness() == null || dependency.getStrictness() == ResourceObjectTypeDependencyStrictnessType.STRICT
						|| dependency.getStrictness() == ResourceObjectTypeDependencyStrictnessType.RELAXED) {
					if (wasProvisioned(dependencyAccountContext, context.getExecutionWave())) {
						// everything OK
					} else {
						// We do not want to throw exception here. That will stop entire projection.
						// Let's just mark the projection as broken and skip it.
						LOGGER.warn("Unsatisfied dependency of account "+accountContext.getResourceShadowDiscriminator()+
								" dependent on "+refRat+": Account not provisioned in dependency check (execution wave "+context.getExecutionWave()+", account wave "+accountContext.getWave() + ", depenedency account wave "+dependencyAccountContext.getWave()+")");
						accountContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
						return false;
					}
				} else if (dependency.getStrictness() == ResourceObjectTypeDependencyStrictnessType.LAX) {
					// we don't care what happened, just go on
					return true;
				} else {
					throw new IllegalArgumentException("Unknown dependency strictness "+dependency.getStrictness()+" in "+refRat);
				}
			}
		}
		return true;
	}
	
	private <F extends ObjectType, P extends ShadowType> boolean wasProvisioned(LensProjectionContext<P> accountContext, int executionWave) {
		int accountWave = accountContext.getWave();
		if (accountWave >= executionWave) {
			// This had no chance to be provisioned yet, so we assume it will be provisioned
			return true;
		}
		if (accountContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN 
				|| accountContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE) {
			return false;
		}
		PrismObject<P> objectOld = accountContext.getObjectOld();
		if (objectOld != null && objectOld.asObjectable().getFailedOperationType() != null) {
			// There is unfinished operation in the shadow. We cannot continue.
			return false;
		}
		if (accountContext.isExists()) {
			return true;
		}
		if (accountContext.isAdd()) {
			List<LensObjectDeltaOperation<P>> executedDeltas = accountContext.getExecutedDeltas();
			if (executedDeltas == null || executedDeltas.isEmpty()) {
				return false;
			}
			for (LensObjectDeltaOperation<P> executedDelta: executedDeltas) {
				OperationResult executionResult = executedDelta.getExecutionResult();
				if (executionResult == null || !executionResult.isSuccess()) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private <F extends ObjectType, P extends ObjectType> void checkContextSanity(LensContext<F,P> context, String activityDescription, 
			OperationResult result) throws SchemaException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext != null) {
			PrismObject<F> focusObjectNew = focusContext.getObjectNew();
			if (focusObjectNew != null) {
				if (focusObjectNew.asObjectable().getName() == null) {
					throw new SchemaException("Focus "+focusObjectNew+" does not have a name after "+activityDescription);
				}
			}
		}
	}

}
