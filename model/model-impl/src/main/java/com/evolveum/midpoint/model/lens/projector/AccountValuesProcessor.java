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

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.expression.Expression;
import com.evolveum.midpoint.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.common.expression.Source;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.model.lens.ShadowConstraintsChecker;
import com.evolveum.midpoint.model.sync.CorrelationConfirmationEvaluator;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.IterationSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.common.InternalsConfig.consistencyChecks;

/**
 * Processor that determines values of account attributes. It does so by taking the pre-processed information left
 * behind by the assignment processor. It also does some checks, such as check of identifier uniqueness. It tries to
 * do several iterations over the value computations if a conflict is found (and this feature is enabled).
 * 
 * @author Radovan Semancik
 */
@Component
public class AccountValuesProcessor {
	
	private static final Trace LOGGER = TraceManager.getTrace(AccountValuesProcessor.class);
	
	@Autowired(required = true)
    private OutboundProcessor outboundProcessor;
	
	@Autowired(required = true)
    private ConsolidationProcessor consolidationProcessor;
	
	@Autowired(required = true)
    private AssignmentProcessor assignmentProcessor;
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	RepositoryService repositoryService;
	
	@Autowired(required = true)
	private ExpressionFactory expressionFactory;
	
	@Autowired(required = true)
	private PrismContext prismContext;
	
	@Autowired
	private CorrelationConfirmationEvaluator correlationConfirmationEvaluator;

	@Autowired(required = true)
	private ProvisioningService provisioningService;
	
	public <F extends ObjectType, P extends ObjectType> void process(LensContext<F,P> context, 
			LensProjectionContext<P> projectionContext, String activityDescription, OperationResult result) 
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, 
			CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
    	if (focusContext.getObjectTypeClass() != UserType.class) {
    		// We can do this only for user.
    		return;
    	}
    	processAccounts((LensContext<UserType,ShadowType>) context, (LensProjectionContext<ShadowType>)projectionContext, 
    			activityDescription, result);
	}
	
	public void processAccounts(LensContext<UserType,ShadowType> context, 
			LensProjectionContext<ShadowType> accountContext, String activityDescription, OperationResult result) 
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException,
			CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException {
		
		checkSchemaAndPolicies(context, accountContext, activityDescription, result);
		
		SynchronizationPolicyDecision policyDecision = accountContext.getSynchronizationPolicyDecision();
		if (policyDecision != null && policyDecision == SynchronizationPolicyDecision.UNLINK) {
			// We will not update accounts that are being unlinked.
			// we cannot skip deleted accounts here as the delete delta will be skipped as well
			return;
		}
		
		if (consistencyChecks) context.checkConsistence();
		
		int maxIterations = determineMaxIterations(accountContext);
		int iteration = 0;
		boolean skipUniquenessCheck = false;
		while (true) {
			
			accountContext.setIteration(iteration);
			String iterationToken = formatIterationToken(context, accountContext, iteration, result);
			String conflictMessage;
			
			LOGGER.trace("Projection values iteration {}, token '{}' for {}", new Object[]{iteration, iterationToken, accountContext.getHumanReadableName()});
			
			if (!evaluateIterationCondition(context, accountContext, iteration, iterationToken, true, result)) {
				
				conflictMessage = "pre-iteration condition was false";
				LOGGER.debug("Skipping iteration {}, token '{}' for {} because the pre-iteration condition was false",
						new Object[]{iteration, iterationToken, accountContext.getHumanReadableName()});
			} else {

				accountContext.setIterationToken(iterationToken);			
				if (consistencyChecks) context.checkConsistence();
				
				// Re-evaluates the values in the account constructions (including roles)
				assignmentProcessor.processAssignmentsAccountValues(accountContext, result);
				
				context.recompute();
				if (consistencyChecks) context.checkConsistence();
				
				// Evaluates the values in outbound mappings
				outboundProcessor.processOutbound(context, accountContext, result);
				
				context.recompute();
				if (consistencyChecks) context.checkConsistence();
				
				// Merges the values together, processing exclusions and strong/weak mappings are needed
				consolidationProcessor.consolidateValues(context, accountContext, result);
				
				if (consistencyChecks) context.checkConsistence();
		        context.recompute();
		        if (consistencyChecks) context.checkConsistence();
		
		        // Too noisy for now
	//	        LensUtil.traceContext(LOGGER, activityDescription, "values", context, true);
		        
		        if (policyDecision != null && policyDecision == SynchronizationPolicyDecision.DELETE) {
		        	// No need to play the iterative game if the account is deleted
		        	break;
		        }
		        
		        // Check constraints
		        boolean conflict = true;
		        ShadowConstraintsChecker checker = new ShadowConstraintsChecker(accountContext);
		        
		        if (skipUniquenessCheck) {
		        	skipUniquenessCheck = false;
		        	conflict = false;
		        } else {
		        	
			        checker.setPrismContext(prismContext);
			        checker.setContext(context);
			        checker.setRepositoryService(repositoryService);
			        checker.check(result);
			        if (checker.isSatisfiesConstraints()) {
			        	LOGGER.trace("Current shadow satisfies uniqueness constraints. Iteration {}, token '{}'", iteration, iterationToken);
			        	conflict = false;
			        } else {
			        	if (checker.getConflictingShadow() != null){
			        		PrismObject<ShadowType> fullConflictingShadow = null;
			        		try{
			        			fullConflictingShadow = provisioningService.getObject(ShadowType.class, checker.getConflictingShadow().getOid(), null, result);
			        		} catch (ObjectNotFoundException ex){
			        			//if object not found exception occurred, its ok..the account was deleted by the discovery, so there esits no more conflicting shadow
			        			LOGGER.trace("Conflicting shadow was deleted by discovery. It does not exist anymore. Continue with adding current shadow.");
			        			conflict = false;
			        		}
			        		
			        		if (conflict) {
				        		PrismObject<UserType> user = repositoryService.listAccountShadowOwner(checker.getConflictingShadow().getOid(), result);
				        		
				        		
				        		//the owner of the shadow exist and it is a current user..so the shadow was successfully created, linked etc..no other recompute is needed..
				        		if (user != null && user.getOid().equals(context.getFocusContext().getOid())) {
			//	        			accountContext.setSecondaryDelta(null);
				        			cleanupContext(accountContext);	        			
				        			accountContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.KEEP);
				        			accountContext.setObjectOld(fullConflictingShadow);
				        			accountContext.setFullShadow(true);
				        			ObjectDelta<ShadowType> secondaryDelta = accountContext.getSecondaryDelta();
				        			if (secondaryDelta != null && accountContext.getOid() != null) {
				        	        	secondaryDelta.setOid(accountContext.getOid());
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
				        			// Re-do this same iteration again (do not increase iteration count).
				        			// It will recompute the values and therefore enforce the user deltas and enable reconciliation
				        			skipUniquenessCheck = true; // to avoid endless loop
				        			continue;
				        		}
				        		
				        		if (user == null) {
					        		
					        		ResourceType resourceType = accountContext.getResource();
					        		
					        		if (ResourceTypeUtil.isSynchronizationOpportunistic(resourceType)) {
					        		
										boolean match = correlationConfirmationEvaluator.matchUserCorrelationRule(fullConflictingShadow, context
												.getFocusContext().getObjectNew(), resourceType, result);
										
										if (match){
											//found shadow belongs to the current user..need to link it and replace current shadow with the found shadow..
											cleanupContext(accountContext);
											accountContext.setObjectOld(fullConflictingShadow);
											accountContext.setFullShadow(true);
											accountContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.KEEP);
											ObjectDelta<ShadowType> secondaryDelta = accountContext.getSecondaryDelta();
											if (secondaryDelta != null && accountContext.getOid() != null) {
									        	secondaryDelta.setOid(accountContext.getOid());
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
					if (evaluateIterationCondition(context, accountContext, iteration, iterationToken, false, result)) {
	    				// stop the iterations
	    				break;
	    			} else {
	    				conflictMessage = "post-iteration condition was false";
	    				LOGGER.debug("Skipping iteration {}, token '{}' for {} because the post-iteration condition was false",
	    						new Object[]{iteration, iterationToken, accountContext.getHumanReadableName()});
	    			}
				} else {
					conflictMessage = checker.getMessages();
				}
			}
			
	        iteration++;
	        if (iteration > maxIterations) {
	        	StringBuilder sb = new StringBuilder();
	        	if (iteration == 1) {
	        		sb.append("Error processing ");
	        	} else {
	        		sb.append("Too many iterations ("+iteration+") for ");
	        	}
	        	sb.append(accountContext.getHumanReadableName());
	        	if (iteration == 1) {
	        		sb.append(": constraint violation: ");
	        	} else {
	        		sb.append(": cannot determine values that satisfy constraints: ");
	        	}
	        	if (conflictMessage != null) {
	        		sb.append(conflictMessage);
	        	}
	        	throw new ObjectAlreadyExistsException(sb.toString());
	        }
	        
	        cleanupContext(accountContext);
	        if (consistencyChecks) context.checkConsistence();
	        
		}
		
		if (consistencyChecks) context.checkConsistence();
					
	}
	
	private int determineMaxIterations(LensProjectionContext<ShadowType> accountContext) {
		ResourceObjectTypeDefinitionType accDef = accountContext.getResourceAccountTypeDefinitionType();
		if (accDef != null) {
			IterationSpecificationType iteration = accDef.getIteration();
			if (iteration != null) {
				return iteration.getMaxIterations();
			}
		}
		return 0;
	}

	private String formatIterationToken(LensContext<UserType,ShadowType> context, 
			LensProjectionContext<ShadowType> accountContext, int iteration, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		ResourceObjectTypeDefinitionType accDef = accountContext.getResourceAccountTypeDefinitionType();
		if (accDef == null) {
			return formatIterationTokenDefault(iteration);
		}
		IterationSpecificationType iterationType = accDef.getIteration();
		if (iterationType == null) {
			return formatIterationTokenDefault(iteration);
		}
		ExpressionType tokenExpressionType = iterationType.getTokenExpression();
		if (tokenExpressionType == null) {
			return formatIterationTokenDefault(iteration);
		}
		PrismPropertyDefinition<String> outputDefinition = new PrismPropertyDefinition<String>(ExpressionConstants.VAR_ITERATION_TOKEN,
				ExpressionConstants.VAR_ITERATION_TOKEN, DOMUtil.XSD_STRING, prismContext);
		Expression<PrismPropertyValue<String>> expression = expressionFactory.makeExpression(tokenExpressionType, outputDefinition , "iteration token expression in "+accountContext.getHumanReadableName(), result);
		
		Collection<Source<?>> sources = new ArrayList<Source<?>>();
		PrismPropertyDefinition<Integer> inputDefinition = new PrismPropertyDefinition<Integer>(ExpressionConstants.VAR_ITERATION,
				ExpressionConstants.VAR_ITERATION, DOMUtil.XSD_INT, prismContext);
		inputDefinition.setMaxOccurs(1);
		PrismProperty<Integer> input = inputDefinition.instantiate();
		input.add(new PrismPropertyValue<Integer>(iteration));
		ItemDeltaItem<PrismPropertyValue<Integer>> idi = new ItemDeltaItem<PrismPropertyValue<Integer>>(input);
		Source<PrismPropertyValue<Integer>> iterationSource = new Source<PrismPropertyValue<Integer>>(idi, ExpressionConstants.VAR_ITERATION);
		sources.add(iterationSource);
		
		Map<QName, Object> variables = createExpressionVariables(context, accountContext);
		ExpressionEvaluationContext expressionContext = new ExpressionEvaluationContext(sources , variables, "iteration token expression in "+accountContext.getHumanReadableName(), result);
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = expression.evaluate(expressionContext);
		Collection<PrismPropertyValue<String>> outputValues = outputTriple.getNonNegativeValues();
		if (outputValues.isEmpty()) {
			return "";
		}
		if (outputValues.size() > 1) {
			throw new ExpressionEvaluationException("Iteration token expression in "+accountContext.getHumanReadableName()+" returned more than one value ("+outputValues.size()+" values)");
		}
		String realValue = outputValues.iterator().next().getValue();
		if (realValue == null) {
			return "";
		}
		return realValue;
	}
		
	private Map<QName, Object> createExpressionVariables(LensContext<UserType,ShadowType> context, 
			LensProjectionContext<ShadowType> accountContext) {
		Map<QName, Object> variables = new HashMap<QName, Object>();
		variables.put(ExpressionConstants.VAR_FOCUS, context.getFocusContext().getObjectNew());
		variables.put(ExpressionConstants.VAR_SHADOW, accountContext.getObjectNew());
		return variables;
	}

	private String formatIterationTokenDefault(int iteration) {
		if (iteration == 0) {
			return "";
		}
		return Integer.toString(iteration);
	}

	private boolean evaluateIterationCondition(LensContext<UserType, ShadowType> context, 
			LensProjectionContext<ShadowType> accountContext, int iteration, String iterationToken, 
			boolean beforeIteration, OperationResult result) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException {
		ResourceObjectTypeDefinitionType accDef = accountContext.getResourceAccountTypeDefinitionType();
		if (accDef == null) {
			return true;
		}
		IterationSpecificationType iterationType = accDef.getIteration();
		if (iterationType == null) {
			return true;
		}
		ExpressionType expressionType;
		String desc;
		if (beforeIteration) {
			expressionType = iterationType.getPreIterationCondition();
			desc = "pre-iteration expression in "+accountContext.getHumanReadableName();
		} else {
			expressionType = iterationType.getPostIterationCondition();
			desc = "post-iteration expression in "+accountContext.getHumanReadableName();
		}
		if (expressionType == null) {
			return true;
		}
		PrismPropertyDefinition<Boolean> outputDefinition = new PrismPropertyDefinition<Boolean>(ExpressionConstants.OUTPUT_ELMENT_NAME,
				ExpressionConstants.OUTPUT_ELMENT_NAME, DOMUtil.XSD_BOOLEAN, prismContext);
		Expression<PrismPropertyValue<Boolean>> expression = expressionFactory.makeExpression(expressionType, outputDefinition , desc, result);
		
		Map<QName, Object> variables = createExpressionVariables(context, accountContext);
		variables.put(ExpressionConstants.VAR_ITERATION, iteration);
		variables.put(ExpressionConstants.VAR_ITERATION_TOKEN, iterationToken);
		
		ExpressionEvaluationContext expressionContext = new ExpressionEvaluationContext(null , variables, desc, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = expression.evaluate(expressionContext);
		Collection<PrismPropertyValue<Boolean>> outputValues = outputTriple.getNonNegativeValues();
		if (outputValues.isEmpty()) {
			return false;
		}
		if (outputValues.size() > 1) {
			throw new ExpressionEvaluationException(desc+" returned more than one value ("+outputValues.size()+" values)");
		}
		Boolean realValue = outputValues.iterator().next().getValue();
		if (realValue == null) {
			return false;
		}
		return realValue;

	}

	/**
	 * Check that the primary deltas do not violate schema and policies
	 * TODO: implement schema check 
	 */
	public void checkSchemaAndPolicies(LensContext<UserType,ShadowType> context, 
			LensProjectionContext<ShadowType> accountContext, String activityDescription, OperationResult result) throws SchemaException, PolicyViolationException {
		ObjectDelta<ShadowType> primaryDelta = accountContext.getPrimaryDelta();
		if (primaryDelta == null || primaryDelta.isDelete()) {
			return;
		}
		
		RefinedObjectClassDefinition rAccountDef = accountContext.getRefinedAccountDefinition();
		if (rAccountDef == null) {
			throw new SchemaException("No definition for account type '"
					+accountContext.getResourceShadowDiscriminator().getIntent()+"' in "+accountContext.getResource());
		}
		
		if (primaryDelta.isAdd()) {
			PrismObject<ShadowType> accountToAdd = primaryDelta.getObjectToAdd();
			ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(accountToAdd);
			if (attributesContainer != null) {
				for (ResourceAttribute<?> attribute: attributesContainer.getAttributes()) {
					RefinedAttributeDefinition rAttrDef = rAccountDef.findAttributeDefinition(attribute.getName());
					if (!rAttrDef.isTolerant()) {
						throw new PolicyViolationException("Attempt to add object with non-tolerant attribute "+attribute.getName()+" in "+
								"account "+accountContext.getResourceShadowDiscriminator()+" during "+activityDescription);
					}
				}
			}
		} else if (primaryDelta.isModify()) {
			for(ItemDelta<?> modification: primaryDelta.getModifications()) {
				if (modification.getParentPath().equals(SchemaConstants.PATH_ATTRIBUTES)) {
					PropertyDelta<?> attrDelta = (PropertyDelta<?>) modification;
					RefinedAttributeDefinition rAttrDef = rAccountDef.findAttributeDefinition(attrDelta.getName());
					if (!rAttrDef.isTolerant()) {
						throw new PolicyViolationException("Attempt to modify non-tolerant attribute "+attrDelta.getName()+" in "+
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
	private void cleanupContext(LensProjectionContext<ShadowType> accountContext) throws SchemaException {
		// We must NOT clean up activation computation. This has happened before, it will not happen again
		// and it does not depend on iteration
		ObjectDelta<ShadowType> secondaryDelta = accountContext.getSecondaryDelta();
		if (secondaryDelta != null) {
			Collection<? extends ItemDelta> modifications = secondaryDelta.getModifications();
			if (modifications != null) {
				Iterator<? extends ItemDelta> iterator = modifications.iterator();
				while (iterator.hasNext()) {
					ItemDelta modification = iterator.next();
					if (! new ItemPath(FocusType.F_ACTIVATION).equals(modification.getParentPath())) {
						iterator.remove();
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

	
}
