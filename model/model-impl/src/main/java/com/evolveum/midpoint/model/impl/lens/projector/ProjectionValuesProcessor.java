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
package com.evolveum.midpoint.model.impl.lens.projector;

import static com.evolveum.midpoint.schema.internals.InternalsConfig.consistencyChecks;

import java.util.Collection;
import java.util.Iterator;

import com.evolveum.midpoint.model.impl.sync.SynchronizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.focus.AssignmentProcessor;
import com.evolveum.midpoint.model.impl.sync.CorrelationConfirmationEvaluator;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterationSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Processor that determines values of account attributes. It does so by taking the pre-processed information left
 * behind by the assignment processor. It also does some checks, such as check of identifier uniqueness. It tries to
 * do several iterations over the value computations if a conflict is found (and this feature is enabled).
 *
 * @author Radovan Semancik
 */
@Component
public class ProjectionValuesProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(ProjectionValuesProcessor.class);

	@Autowired
    private OutboundProcessor outboundProcessor;

	@Autowired
    private ConsolidationProcessor consolidationProcessor;

	@Autowired
    private AssignmentProcessor assignmentProcessor;

	@Autowired
	@Qualifier("cacheRepositoryService")
	RepositoryService repositoryService;

	@Autowired
	private ExpressionFactory expressionFactory;

	@Autowired
	private PrismContext prismContext;

	@Autowired
	private CorrelationConfirmationEvaluator correlationConfirmationEvaluator;

	@Autowired
	private SynchronizationService synchronizationService;

	@Autowired
    private ContextLoader contextLoader;

	@Autowired
	private ProvisioningService provisioningService;

	public <O extends ObjectType> void process(LensContext<O> context,
			LensProjectionContext projectionContext, String activityDescription, Task task, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException,
			CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException {
		LensFocusContext<O> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
    	if (!FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
    		// We can do this only for focus types.
    		return;
    	}
    	OperationResult processorResult = result.createMinorSubresult(ProjectionValuesProcessor.class.getName()+".processAccountsValues");
    	processorResult.recordSuccessIfUnknown();
    	processProjections((LensContext<? extends FocusType>) context, projectionContext,
    			activityDescription, task, processorResult);

	}

	private <F extends FocusType> void processProjections(LensContext<F> context,
			LensProjectionContext projContext, String activityDescription, Task task, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException,
			CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException {

		checkSchemaAndPolicies(context, projContext, activityDescription, result);

		SynchronizationPolicyDecision policyDecision = projContext.getSynchronizationPolicyDecision();
		if (policyDecision != null && policyDecision == SynchronizationPolicyDecision.UNLINK) {
			// We will not update accounts that are being unlinked.
			// we cannot skip deleted accounts here as the delete delta will be skipped as well
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Skipping processing of value for {} because the decision is {}", projContext.getHumanReadableName(), policyDecision);
			}
			return;
		}

		if (consistencyChecks) context.checkConsistence();

		if (!projContext.hasFullShadow() && hasIterationExpression(projContext)) {
			contextLoader.loadFullShadow(context, projContext, "iteration expression", task, result);
			if (projContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
            	return;
            }
		}

		int maxIterations = determineMaxIterations(projContext);
		int iteration = 0;
		String iterationToken = null;
		boolean wasResetIterationCounter = false;

		PrismObject<ShadowType> shadowCurrent = projContext.getObjectCurrent();
		if (shadowCurrent != null) {
			Integer shadowIteration = shadowCurrent.asObjectable().getIteration();
			if (shadowIteration != null) {
				iteration = shadowIteration;
			}
			iterationToken = shadowCurrent.asObjectable().getIterationToken();
		}

		boolean skipUniquenessCheck = false;
		while (true) {

			projContext.setIteration(iteration);
			if (iterationToken == null) {
				iterationToken = formatIterationToken(context, projContext, iteration, task, result);
			}
			projContext.setIterationToken(iterationToken);

			String conflictMessage;

			// These are normally null. But there may be leftover from the previous iteration.
			// While that should not affect the algorithm (it should overwrite it) it may confuse
			// people during debugging and unecessarily clutter the debug output.
			projContext.setOutboundConstruction(null);
			projContext.setSqueezedAttributes(null);
			projContext.setSqueezedAssociations(null);

			LOGGER.trace("Projection values iteration {}, token '{}' for {}",
					iteration, iterationToken, projContext.getHumanReadableName());

//			LensUtil.traceContext(LOGGER, activityDescription, "values (start)", false, context, true);

			if (!evaluateIterationCondition(context, projContext, iteration, iterationToken, true, task, result)) {

				conflictMessage = "pre-iteration condition was false";
				LOGGER.debug("Skipping iteration {}, token '{}' for {} because the pre-iteration condition was false",
						iteration, iterationToken, projContext.getHumanReadableName());
			} else {

				if (consistencyChecks) context.checkConsistence();

				// Re-evaluates the values in the account constructions (including roles)
				assignmentProcessor.processAssignmentsAccountValues(projContext, result);

				context.recompute();
				if (consistencyChecks) context.checkConsistence();

//				LensUtil.traceContext(LOGGER, activityDescription, "values (assignment account values)", false, context, true);

				// Evaluates the values in outbound mappings
				outboundProcessor.processOutbound(context, projContext, task, result);

				context.recompute();
				if (consistencyChecks) context.checkConsistence();

//				LensUtil.traceContext(LOGGER, activityDescription, "values (outbound)", false, context, true);

				// Merges the values together, processing exclusions and strong/weak mappings are needed
				consolidationProcessor.consolidateValues(context, projContext, task, result);

				if (consistencyChecks) context.checkConsistence();
		        context.recompute();
		        if (consistencyChecks) context.checkConsistence();

		        // Aux object classes may have changed during consolidation. Make sure we have up-to-date definitions.
		        context.refreshAuxiliaryObjectClassDefinitions();

		        // Check if we need to reset the iteration counter (and token) e.g. because we have rename
		        // we cannot do that before because the mappings are not yet evaluated and the triples and not
		        // consolidated to deltas. We can do it only now. It means that we will waste the first run
		        // but I don't see any easier way to do it now.
		        if (iteration != 0 && !wasResetIterationCounter && willResetIterationCounter(projContext)) {
		        	wasResetIterationCounter = true;
		        	iteration = 0;
		    		iterationToken = null;
		    		cleanupContext(projContext, null);
		    		LOGGER.trace("Resetting iteration counter and token because we have rename");
			        if (consistencyChecks) context.checkConsistence();
		    		continue;
		        }

		        // Too noisy for now
//		        LensUtil.traceContext(LOGGER, activityDescription, "values (consolidation)", false, context, true);


		        if (policyDecision != null && policyDecision == SynchronizationPolicyDecision.DELETE) {
		        	// No need to play the iterative game if the account is deleted
		        	break;
		        }

		        // Check constraints
		        boolean conflict = true;
		        ShadowConstraintsChecker<F> checker = new ShadowConstraintsChecker<F>(projContext);

		        if (skipUniquenessCheck) {
		        	skipUniquenessCheck = false;
		        	conflict = false;
		        } else {
		
			        checker.setPrismContext(prismContext);
			        checker.setContext(context);
			        checker.setProvisioningService(provisioningService);
			        checker.check(task, result);
			        if (checker.isSatisfiesConstraints()) {
			        	LOGGER.trace("Current shadow satisfies uniqueness constraints. Iteration {}, token '{}'", iteration, iterationToken);
			        	conflict = false;
			        } else {
			        	LOGGER.trace("Current shadow does not satisfy constraints. Conflicting shadow exists. Needed to found out what's wrong.");
			        	if (checker.getConflictingShadow() != null){
			        		PrismObject<ShadowType> fullConflictingShadow = null;
			        		try{
			        			Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));
								fullConflictingShadow = provisioningService.getObject(ShadowType.class, checker.getConflictingShadow().getOid(), options, task, result);
			        		} catch (ObjectNotFoundException ex){
			        			//if object not found exception occurred, its ok..the account was deleted by the discovery, so there esits no more conflicting shadow
			        			LOGGER.trace("Conflicting shadow was deleted by discovery. It does not exist anymore. Continue with adding current shadow.");
			        			conflict = false;
			
			        		}
			
			        		result.computeStatus();
							// if the result is fatal error, it may mean that the
							// already exists expection occures before..but in this
							// scenario it means, the exception was handled and we
							// can mute the result to give better understanding of
							// the situation which happend
		        			if (result.isError()){
		        				result.muteError();
		        			}
			
			        		if (conflict) {
								PrismObject<F> focus = repositoryService.searchShadowOwner(checker
										.getConflictingShadow().getOid(), SelectorOptions
										.createCollection(GetOperationOptions.createAllowNotFound()), result);
				
				
				        		//the owner of the shadow exist and it is a current user..so the shadow was successfully created, linked etc..no other recompute is needed..
				        		if (focus != null && focus.getOid().equals(context.getFocusContext().getOid())) {
				        			LOGGER.trace("Conflicting projection already linked to the current focus, no recompute needed, continue processing with conflicting projection.");
			//	        			accountContext.setSecondaryDelta(null);
				        			cleanupContext(projContext, fullConflictingShadow);
				        			projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.KEEP);
				        			projContext.setObjectOld(fullConflictingShadow.clone());
				        			projContext.setObjectCurrent(fullConflictingShadow);
				        			projContext.setOid(fullConflictingShadow.getOid());
				        			projContext.setFullShadow(true);
				        			ObjectDelta<ShadowType> secondaryDelta = projContext.getSecondaryDelta();
				        			if (secondaryDelta != null && projContext.getOid() != null) {
				        	        	secondaryDelta.setOid(projContext.getOid());
				        	        }
//				        			result.computeStatus();
//									// if the result is fatal error, it may mean that the
//									// already exists expection occures before..but in this
//									// scenario it means, the exception was handled and we
//									// can mute the result to give better understanding of
//									// the situation which happend
//				        			if (result.isError()){
//				        				result.muteError();
//				        			}
				        			// Re-do this same iteration again (do not increase iteration count).
				        			// It will recompute the values and therefore enforce the user deltas and enable reconciliation
				        			skipUniquenessCheck = true; // to avoid endless loop
				        			continue;
				        		}
				
				        		if (focus == null) {
					        		LOGGER.trace("There is no owner linked with the conflicting projection.");
					        		ResourceType resourceType = projContext.getResource();
					
					        		if (ResourceTypeUtil.isSynchronizationOpportunistic(resourceType)) {
					        			LOGGER.trace("Trying to find owner using correlation expression.");
										boolean match = synchronizationService.matchUserCorrelationRule(fullConflictingShadow,
												context.getFocusContext().getObjectNew(), resourceType, context.getSystemConfiguration(), task, result);

										if (match){
											//check if it is add account (primary delta contains add shadow deltu)..
											//if it is add account, create new context for conflicting account..
											//it ensures, that conflicting account is linked to the user

											if (projContext.getPrimaryDelta() != null && projContext.getPrimaryDelta().isAdd()){

												PrismObject<ShadowType> shadow = projContext.getPrimaryDelta().getObjectToAdd();
												LOGGER.trace("Found primary ADD delta of shadow {}.", shadow);

												LensProjectionContext conflictingAccountContext = context.findProjectionContext(projContext.getResourceShadowDiscriminator(), fullConflictingShadow.getOid());
												if (conflictingAccountContext == null){
													conflictingAccountContext = LensUtil.createAccountContext(context, projContext.getResourceShadowDiscriminator());
//													conflictingAccountContext = context.createProjectionContext(accountContext.getResourceShadowDiscriminator());
													conflictingAccountContext.setOid(fullConflictingShadow.getOid());
													conflictingAccountContext.setObjectOld(fullConflictingShadow.clone());
													conflictingAccountContext.setObjectCurrent(fullConflictingShadow);
													conflictingAccountContext.setFullShadow(true);
													conflictingAccountContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.KEEP);
													conflictingAccountContext.setResource(projContext.getResource());
													conflictingAccountContext.setDoReconciliation(true);
													conflictingAccountContext.getDependencies().clear();
													conflictingAccountContext.getDependencies().addAll(projContext.getDependencies());
													conflictingAccountContext.setWave(projContext.getWave());
													context.addConflictingProjectionContext(conflictingAccountContext);
												}

												projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
												result.recordFatalError("Could not add account " + projContext.getObjectNew() + ", because the account with the same identifier already exists on the resource. ");
												LOGGER.error("Could not add account {}, because the account with the same identifier already exists on the resource. ", projContext.getObjectNew());

												skipUniquenessCheck = true; // to avoid endless loop
							        			continue;
											}

											//found shadow belongs to the current user..need to link it and replace current shadow with the found shadow..
											cleanupContext(projContext, fullConflictingShadow);
											projContext.setObjectOld(fullConflictingShadow.clone());
											projContext.setObjectCurrent(fullConflictingShadow);
											projContext.setOid(fullConflictingShadow.getOid());
											projContext.setFullShadow(true);
											projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.KEEP);
											ObjectDelta<ShadowType> secondaryDelta = projContext.getSecondaryDelta();
											if (secondaryDelta != null && projContext.getOid() != null) {
									        	secondaryDelta.setOid(projContext.getOid());
									        }
											LOGGER.trace("User {} satisfies correlation rules.", context.getFocusContext().getObjectNew());

						        			// Re-do this same iteration again (do not increase iteration count).
						        			// It will recompute the values and therefore enforce the user deltas and enable reconciliation
											skipUniquenessCheck = true; // to avoid endless loop
						        			continue;
										} else{
											LOGGER.trace("User {} does not satisfy correlation rules.", context.getFocusContext().getObjectNew());
										}
					        		}

				        		} else{
				        			LOGGER.trace("Recomputing shadow identifier, because shadow with the some identifier exists and it belongs to other user.");
				        		}
			        		}
			        	}			
			        }
		        }

		        if (!conflict) {
					if (evaluateIterationCondition(context, projContext, iteration, iterationToken, false, task, result)) {
	    				// stop the iterations
	    				break;
	    			} else {
	    				conflictMessage = "post-iteration condition was false";
	    				LOGGER.debug("Skipping iteration {}, token '{}' for {} because the post-iteration condition was false",
	    						new Object[]{iteration, iterationToken, projContext.getHumanReadableName()});
	    			}
				} else {
					conflictMessage = checker.getMessages();
				}
			}

	        iteration++;
	        iterationToken = null;
			LensUtil.checkMaxIterations(iteration, maxIterations, conflictMessage, projContext.getHumanReadableName());

			cleanupContext(projContext, null);
	        if (consistencyChecks) context.checkConsistence();

		}

		addIterationTokenDeltas(projContext);
		result.cleanupResult();
		if (consistencyChecks) context.checkConsistence();


	}

	private boolean willResetIterationCounter(LensProjectionContext projectionContext) throws SchemaException {
		ObjectDelta<ShadowType> accountDelta = projectionContext.getDelta();
		if (accountDelta == null) {
			return false;
		}
		RefinedObjectClassDefinition oOcDef = projectionContext.getCompositeObjectClassDefinition();
		for (RefinedAttributeDefinition identifierDef: oOcDef.getPrimaryIdentifiers()) {
			ItemPath identifierPath = new ItemPath(ShadowType.F_ATTRIBUTES, identifierDef.getName());
			if (accountDelta.findPropertyDelta(identifierPath) != null) {
				return true;
			}
		}
		for (RefinedAttributeDefinition identifierDef: oOcDef.getSecondaryIdentifiers()) {
			ItemPath identifierPath = new ItemPath(ShadowType.F_ATTRIBUTES, identifierDef.getName());
			if (accountDelta.findPropertyDelta(identifierPath) != null) {
				return true;
			}
		}
		return false;
	}



	private boolean hasIterationExpression(LensProjectionContext accountContext) {
		ResourceObjectTypeDefinitionType accDef = accountContext.getResourceObjectTypeDefinitionType();
		if (accDef == null) {
			return false;
		}
		IterationSpecificationType iterationType = accDef.getIteration();
		if (iterationType == null) {
			return false;
		}
		if (iterationType.getTokenExpression() != null) {
			return true;
		}
		if (iterationType.getPostIterationCondition() != null) {
			return true;
		}
		if (iterationType.getPreIterationCondition() != null) {
			return true;
		}
		return false;
	}

	private int determineMaxIterations(LensProjectionContext accountContext) {
		ResourceObjectTypeDefinitionType accDef = accountContext.getResourceObjectTypeDefinitionType();
		if (accDef != null) {
			IterationSpecificationType iteration = accDef.getIteration();
			return LensUtil.determineMaxIterations(iteration);
		} else {
			return LensUtil.determineMaxIterations(null);
		}
	}

	private <F extends ObjectType> String formatIterationToken(LensContext<F> context,
			LensProjectionContext accountContext, int iteration, Task task, OperationResult result)
					throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		ResourceObjectTypeDefinitionType accDef = accountContext.getResourceObjectTypeDefinitionType();
		if (accDef == null) {
			return LensUtil.formatIterationTokenDefault(iteration);
		}
		IterationSpecificationType iterationType = accDef.getIteration();
		ExpressionVariables variables = createExpressionVariables(context, accountContext);
		return LensUtil.formatIterationToken(context, accountContext, iterationType, iteration,
				expressionFactory, variables, task, result);
	}

	private <F extends ObjectType> ExpressionVariables createExpressionVariables(LensContext<F> context,
			LensProjectionContext projectionContext) {
		return Utils.getDefaultExpressionVariables(context.getFocusContext().getObjectNew(), projectionContext.getObjectNew(),
				projectionContext.getResourceShadowDiscriminator(), projectionContext.getResource().asPrismObject(),
				context.getSystemConfiguration(), projectionContext);
	}

	private <F extends ObjectType> boolean evaluateIterationCondition(LensContext<F> context,
			LensProjectionContext accountContext, int iteration, String iterationToken,
			boolean beforeIteration, Task task, OperationResult result)
					throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		ResourceObjectTypeDefinitionType accDef = accountContext.getResourceObjectTypeDefinitionType();
		if (accDef == null) {
			return true;
		}
		IterationSpecificationType iterationType = accDef.getIteration();
		ExpressionVariables variables = createExpressionVariables(context, accountContext);
		return LensUtil.evaluateIterationCondition(context, accountContext, iterationType,
				iteration, iterationToken, beforeIteration, expressionFactory, variables, task, result);
	}

	/**
	 * Check that the primary deltas do not violate schema and policies
	 * TODO: implement schema check
	 */
	public <F extends ObjectType> void checkSchemaAndPolicies(LensContext<F> context,
			LensProjectionContext accountContext, String activityDescription, OperationResult result) throws SchemaException, PolicyViolationException {
		ObjectDelta<ShadowType> primaryDelta = accountContext.getPrimaryDelta();
		if (primaryDelta == null || primaryDelta.isDelete()) {
			return;
		}

		RefinedObjectClassDefinition rAccountDef = accountContext.getCompositeObjectClassDefinition();
		if (rAccountDef == null) {
			throw new SchemaException("No definition for account type '"
					+accountContext.getResourceShadowDiscriminator()+"' in "+accountContext.getResource());
		}

		if (primaryDelta.isAdd()) {
			PrismObject<ShadowType> accountToAdd = primaryDelta.getObjectToAdd();
			ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(accountToAdd);
			if (attributesContainer != null) {
				for (ResourceAttribute<?> attribute: attributesContainer.getAttributes()) {
					RefinedAttributeDefinition rAttrDef = rAccountDef.findAttributeDefinition(attribute.getElementName());
					if (!rAttrDef.isTolerant()) {
						throw new PolicyViolationException("Attempt to add object with non-tolerant attribute "+attribute.getElementName()+" in "+
								"account "+accountContext.getResourceShadowDiscriminator()+" during "+activityDescription);
					}
				}
			}
		} else if (primaryDelta.isModify()) {
			for(ItemDelta<?,?> modification: primaryDelta.getModifications()) {
				if (modification.getParentPath().equivalent(SchemaConstants.PATH_ATTRIBUTES)) {
					PropertyDelta<?> attrDelta = (PropertyDelta<?>) modification;
					RefinedAttributeDefinition rAttrDef = rAccountDef.findAttributeDefinition(attrDelta.getElementName());
					if (!rAttrDef.isTolerant()) {
						throw new PolicyViolationException("Attempt to modify non-tolerant attribute "+attrDelta.getElementName()+" in "+
								"account "+accountContext.getResourceShadowDiscriminator()+" during "+activityDescription);
					}
				}
			}
		} else {
			throw new IllegalStateException("Whoops!");
		}
	}

	/**
	 * Remove the intermediate results of values processing such as secondary deltas.
	 */
	private void cleanupContext(LensProjectionContext accountContext, PrismObject<ShadowType> fullConflictingShadow) throws SchemaException {
		// We must NOT clean up activation computation here. This has happened before, it will not happen again
		// and it does not depend on iteration. But, in fact we want to cleaup up activation changes if they
		// are already applied to the new shadow.
		ObjectDelta<ShadowType> secondaryDelta = accountContext.getSecondaryDelta();
		if (secondaryDelta != null) {
			boolean administrativeStatusDeltaRemoved = false;
			Collection<? extends ItemDelta> modifications = secondaryDelta.getModifications();
			if (modifications != null) {
				Iterator<? extends ItemDelta> iterator = modifications.iterator();
				while (iterator.hasNext()) {
					ItemDelta modification = iterator.next();
					if (SchemaConstants.PATH_ACTIVATION.equivalent(modification.getParentPath())) {
						if (fullConflictingShadow != null) {
							if (QNameUtil.match(ActivationType.F_ADMINISTRATIVE_STATUS,modification.getElementName())) {
								if (modification.isRedundant(fullConflictingShadow)) {
									LOGGER.trace("Removing redundant secondary activation delta: {}", modification);
									iterator.remove();
								}
								administrativeStatusDeltaRemoved = true;
							}
						}
					} else {
						iterator.remove();
					}
				}
				if (administrativeStatusDeltaRemoved) {
					iterator = modifications.iterator();
					while (iterator.hasNext()) {
						ItemDelta modification = iterator.next();
						if (SchemaConstants.PATH_ACTIVATION.equivalent(modification.getParentPath())) {
							if (QNameUtil.match(ActivationType.F_ENABLE_TIMESTAMP, modification.getElementName()) ||
									QNameUtil.match(ActivationType.F_DISABLE_TIMESTAMP, modification.getElementName()) ||
									QNameUtil.match(ActivationType.F_DISABLE_REASON, modification.getElementName()) ||
									QNameUtil.match(ActivationType.F_ARCHIVE_TIMESTAMP, modification.getElementName())) {
								LOGGER.trace("Removing secondary activation delta because redundant delta was removed before: {}", modification);
								iterator.remove();
							}
						}
					}
				}
			}
			if (secondaryDelta.isEmpty()) {
				accountContext.setSecondaryDelta(null);
			}
		}
		accountContext.clearIntermediateResults();
		accountContext.recompute();
	}
	

	/**
	 * Adds deltas for iteration and iterationToken to the shadow if needed.
	 */
	private void addIterationTokenDeltas(LensProjectionContext accountContext) throws SchemaException {
		PrismObject<ShadowType> shadowCurrent = accountContext.getObjectCurrent();
		if (shadowCurrent != null) {
			Integer iterationOld = shadowCurrent.asObjectable().getIteration();
			String iterationTokenOld = shadowCurrent.asObjectable().getIterationToken();
			if (iterationOld != null && iterationOld == accountContext.getIteration() &&
					iterationTokenOld != null && iterationTokenOld.equals(accountContext.getIterationToken())) {
				// Already stored
				return;
			}
		}
		PrismObjectDefinition<ShadowType> shadowDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);

		PrismPropertyValue<Integer> iterationVal = new PrismPropertyValue<Integer>(accountContext.getIteration());
		iterationVal.setOriginType(OriginType.OUTBOUND);
		PropertyDelta<Integer> iterationDelta = PropertyDelta.createReplaceDelta(shadowDef,
				ShadowType.F_ITERATION, iterationVal);
		accountContext.swallowToSecondaryDelta(iterationDelta);

		PrismPropertyValue<String> iterationTokenVal = new PrismPropertyValue<String>(accountContext.getIterationToken());
		iterationTokenVal.setOriginType(OriginType.OUTBOUND);
		PropertyDelta<String> iterationTokenDelta = PropertyDelta.createReplaceDelta(shadowDef,
				ShadowType.F_ITERATION_TOKEN, iterationTokenVal);
		accountContext.swallowToSecondaryDelta(iterationTokenDelta);

	}


}
