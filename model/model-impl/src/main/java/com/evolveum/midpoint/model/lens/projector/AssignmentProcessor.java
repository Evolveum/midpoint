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

import ch.qos.logback.classic.Logger;

import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.mapping.MappingFactory;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.controller.ModelUtils;
import com.evolveum.midpoint.model.lens.AccountConstruction;
import com.evolveum.midpoint.model.lens.AccountConstructionPack;
import com.evolveum.midpoint.model.lens.Assignment;
import com.evolveum.midpoint.model.lens.AssignmentEvaluator;
import com.evolveum.midpoint.model.lens.AssignmentPath;
import com.evolveum.midpoint.model.lens.AssignmentPathSegment;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProjectionPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExclusionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.Map.Entry;

/**
 * Assignment processor is recomputing user assignments. It recomputes all the assignemts whether they are direct
 * or indirect (roles). 
 * 
 * Processor does not do the complete recompute. Only the account "existence" is recomputed. I.e. the processor determines
 * what accounts should be added, deleted or kept as they are. The result is marked in account context SynchronizationPolicyDecision.
 * This step does not create any deltas. It recomputes the attributes to delta set triples but does not "refine" them to deltas yet.
 * It cannot create deltas as other mapping may interfere, e.g. outbound mappings. These needs to be computed before we can
 * create the final deltas (because there may be mapping exclusions, interference of weak mappings, etc.)
 * 
 * The result of assignment processor are intermediary data in the context such as LensContext.evaluatedAssignmentTriple and
 * LensProjectionContext.accountConstructionDeltaSetTriple.
 * 
 * @author Radovan Semancik
 */
@Component
public class AssignmentProcessor {

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired(required = true)
    private ObjectResolver objectResolver;

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private MappingFactory valueConstructionFactory;
    
    @Autowired(required = true)
    private ProvisioningService provisioningService;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentProcessor.class);

    /**
     * Processing all the assignments to determine which projections should be added, deleted or kept as they are.
     * Generic method for all projection types (theoretically). 
     */
    public <F extends ObjectType, P extends ObjectType> void processAssignmentsProjections(LensContext<F,P> context, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
    	LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
    	if (focusContext.getObjectTypeClass() != UserType.class) {
    		// We can do this only for user.
    		return;
    	}
    	processAssignmentsAccounts((LensContext<UserType,ShadowType>) context, result);
    }
    
    /**
     * Processing user-account assignments (including roles). Specific user-account method.
     */
    public void processAssignmentsAccounts(LensContext<UserType,ShadowType> context, OperationResult result) throws SchemaException,
    		ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
    	
    	LensFocusContext<UserType> focusContext = context.getFocusContext();
        ObjectDelta<UserType> focusDelta = focusContext.getDelta();
        
    	if (focusDelta != null && focusDelta.isDelete()) {
			processFocusDelete(context, result);
			return;
		}
        
        // Normal processing. The enforcement policy requires that assigned accounts should be added, so we need to figure out
        // which assignments were added. Do a complete recompute for all the enforcement modes. We can do that because this does
        // not create deltas, it just creates the triples. So we can decide what to do later when we convert triples to deltas.
        
        Collection<PrismContainerValue<AssignmentType>> assignmentsCurrent = new ArrayList<PrismContainerValue<AssignmentType>>();
        if (focusContext.getObjectCurrent() != null) {
            PrismContainer<AssignmentType> assignmentContainer = focusContext.getObjectCurrent().findContainer(UserType.F_ASSIGNMENT);
            if (assignmentContainer != null) {
            	assignmentsCurrent.addAll(assignmentContainer.getValues());
            }
        }

        ContainerDelta<AssignmentType> assignmentDelta = focusContext.getExecutionWaveAssignmentDelta();

        LOGGER.trace("Assignment delta {}", assignmentDelta.dump());

        Collection<PrismContainerValue<AssignmentType>> changedAssignments = assignmentDelta.getValues(AssignmentType.class);

        // Initializing assignemnt evaluator. This will be used later to process all the assignments including the nested
        // assignments (roles).
        AssignmentEvaluator assignmentEvaluator = new AssignmentEvaluator();
        assignmentEvaluator.setRepository(repositoryService);
        assignmentEvaluator.setUserOdo(focusContext.getObjectDeltaObject());
        assignmentEvaluator.setLensContext(context);
        assignmentEvaluator.setChannel(context.getChannel());
        assignmentEvaluator.setObjectResolver(objectResolver);
        assignmentEvaluator.setPrismContext(prismContext);
        assignmentEvaluator.setValueConstructionFactory(valueConstructionFactory);

        // We will be collecting the evaluated account constructions into these three sets. 
        // It forms a kind of delta set triple for the account constructions.
        Map<ResourceShadowDiscriminator, AccountConstructionPack> zeroAccountMap = new HashMap<ResourceShadowDiscriminator, AccountConstructionPack>();
        Map<ResourceShadowDiscriminator, AccountConstructionPack> plusAccountMap = new HashMap<ResourceShadowDiscriminator, AccountConstructionPack>();
        Map<ResourceShadowDiscriminator, AccountConstructionPack> minusAccountMap = new HashMap<ResourceShadowDiscriminator, AccountConstructionPack>();

        LOGGER.trace("Current assignments {}", SchemaDebugUtil.prettyPrint(assignmentsCurrent));
        LOGGER.trace("Changed assignments {}", SchemaDebugUtil.prettyPrint(changedAssignments));

        ObjectType source = null;
        if (focusContext.getObjectCurrent() != null) {
            source = focusContext.getObjectCurrent().asObjectable();
        } else if (focusContext.getObjectNew() != null){
            source = focusContext.getObjectNew().asObjectable();
        }
        
        DeltaSetTriple<Assignment> evaluatedAssignmentTriple = new DeltaSetTriple<Assignment>();
        context.setEvaluatedAssignmentTriple(evaluatedAssignmentTriple);
        
        // Iterate over all the assignments. I mean really all. This is a union of the existing and changed assignments
        // therefore it contains all three types of assignments (plus, minus and zero). As it is an union each assignment
        // will be processed only once. Inside the loop we determine whether it was added, deleted or remains unchanged.
        // This is a first step of the processing. It takes all the account constructions regardless of the resource and
        // account type (intent). Therefore several constructions for the same resource and intent may appear in the resulting
        // sets. This is not good as we want only a single account for each resource/intent combination. But that will be
        // sorted out later.
        Collection<PrismContainerValue<AssignmentType>> allAssignments = MiscUtil.union(assignmentsCurrent, changedAssignments);
        for (PrismContainerValue<AssignmentType> assignmentCVal : allAssignments) {
            AssignmentType assignmentType = assignmentCVal.asContainerable();
            
            boolean forceRecon = false;
            // This really means whether the WHOLE assignment was changed (e.g. added/delted/replaced). It tells nothing
            // about "micro-changes" inside assignment, these will be processed later.
            boolean isAssignmentChanged = containsRealValue(changedAssignments,assignmentCVal);
            String assignmentPlacementDesc;
            if (isAssignmentChanged) {
            	assignmentPlacementDesc = "delta for "+source;
            } else {
            	assignmentPlacementDesc = source.toString();
            	Collection<? extends ItemDelta<?>> assignmentItemDeltas = focusContext.getExecutionWaveAssignmentItemDeltas(assignmentCVal.getId());
            	if (assignmentItemDeltas != null && !assignmentItemDeltas.isEmpty()) {
	            	// Make sure we clone first to avoid side-effects
	            	PrismContainerValue<AssignmentType> assignmentCValClone = assignmentCVal.clone();
	            	assignmentCValClone.setParent(assignmentCVal.getParent());
	            	assignmentCVal = assignmentCValClone;
	            	assignmentType = assignmentCVal.asContainerable();
	            	applyAssignemntMicroDeltas(assignmentItemDeltas, assignmentCVal);
	            	// We do not exactly know what was changed. This may be a replace change, etc.
	            	// Even if we know we do not bother to compute it now. This is not a performance-critical case anyway
	            	// So we just force reconciliation for this case. It will sort it out.
	            	forceRecon = true;
            	}
            }

            LOGGER.trace("Processing assignment {}", SchemaDebugUtil.prettyPrint(assignmentType));
            
            Assignment evaluatedAssignment = null;
            try{
            	evaluatedAssignment = assignmentEvaluator.evaluate(assignmentType, source, assignmentPlacementDesc, result);
            } catch (ObjectNotFoundException ex){
            	if (ModelExecuteOptions.isForce(context.getOptions())){
            		continue;
            	} 
            	ModelUtils.recordFatalError(result, ex);
            	continue;
            } catch (SchemaException ex){
            	ModelUtils.recordFatalError(result, ex);
            	String resourceOid = determineResource(assignmentType);
            	if (resourceOid == null) {
            		// This is a role assignment or something like that. Just throw the original exception for now.
            		throw ex;
            	}
            	ResourceShadowDiscriminator rad = new ResourceShadowDiscriminator(resourceOid, determineIntent(assignmentType));
				LensProjectionContext<ShadowType> accCtx = context.findProjectionContext(rad);
				if (accCtx != null) {
					accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
				}
            	continue;
            }
            
            context.rememberResources(evaluatedAssignment.getResources(result));
            
            // The following code is using collectToAccountMap() to collect the account constructions to one of the three "delta"
            // sets (zero, plus, minus). It is handling several situations that needs to be handled specially.
            // It is also collecting assignments to evaluatedAssignmentTriple.
            
            if (focusDelta != null && focusDelta.isDelete()) {
            	
            	// USER DELETE
            	// If focus (user) is being deleted that all the assignments are to be gone. Including those that
            	// were not changed explicitly.
            	collectToAccountMap(context, minusAccountMap, evaluatedAssignment, forceRecon, result);
                evaluatedAssignmentTriple.addToMinusSet(evaluatedAssignment);
                
            } else {
            	if (assignmentDelta.isReplace()) {

            		// ASSIGNMENT REPLACE
            		// Handling assignment replace delta. This needs to be handled specially as all the "old"
            		// assignments should be considered deleted - except those that are part of the new value set
            		// (remain after replace). As account delete and add are costly operations (and potentiall dangerous)
            		// we optimize here are consider the assignments that were there before replace and still are there
            		// after it as unchanged.
            		boolean hadValue = containsRealValue(assignmentsCurrent, assignmentCVal);
            		boolean willHaveValue = assignmentDelta.isValueToReplace(assignmentCVal);
            		if (hadValue && willHaveValue) {
            			// No change
            			collectToAccountMap(context, zeroAccountMap, evaluatedAssignment, forceRecon, result);
    	                evaluatedAssignmentTriple.addToZeroSet(evaluatedAssignment);
            		} else if (willHaveValue) {
            			// add
            			collectToAccountMap(context, plusAccountMap, evaluatedAssignment, forceRecon, result);
	                    evaluatedAssignmentTriple.addToPlusSet(evaluatedAssignment);
            		} else if (hadValue) {
            			// delete
            			collectToAccountMap(context, minusAccountMap, evaluatedAssignment, forceRecon, result);
	                    evaluatedAssignmentTriple.addToMinusSet(evaluatedAssignment);
            		} else {
            			throw new SystemException("Whoops. Unexpected things happen. Assignment is not old nor new (replace delta)");
            		}
            		
            	} else {

            		// ASSIGNMENT ADD/DELETE
            		// This is the usual situation.
		            // Just sort assignments to sets: unchanged (zero), added (plus), removed (minus)
		            if (isAssignmentChanged) {
		                // There was some change
		
		                if (assignmentDelta.isValueToAdd(assignmentCVal)) {
		                	if (containsRealValue(assignmentsCurrent, assignmentCVal)) {
		                		// Phantom add: adding assignment that is already there
		                        collectToAccountMap(context, zeroAccountMap, evaluatedAssignment, forceRecon, result);
		                        evaluatedAssignmentTriple.addToZeroSet(evaluatedAssignment);
		                	} else {
			                    collectToAccountMap(context, plusAccountMap, evaluatedAssignment, forceRecon, result);
			                    evaluatedAssignmentTriple.addToPlusSet(evaluatedAssignment);
		                	}
		                }
		                if (assignmentDelta.isValueToDelete(assignmentCVal)) {
		                    collectToAccountMap(context, minusAccountMap, evaluatedAssignment, forceRecon, result);
		                    evaluatedAssignmentTriple.addToMinusSet(evaluatedAssignment);
		                }
		
		            } else {
		                // No change in assignment
		                collectToAccountMap(context, zeroAccountMap, evaluatedAssignment, forceRecon, result);
		                evaluatedAssignmentTriple.addToZeroSet(evaluatedAssignment);
		            }
            	}
            }
        }
        
        // Checking for assignment exclusions. This means mostly role exclusions (SoD) 
        checkExclusions(context, evaluatedAssignmentTriple.getZeroSet(), evaluatedAssignmentTriple.getPlusSet());
        checkExclusions(context, evaluatedAssignmentTriple.getPlusSet(), evaluatedAssignmentTriple.getPlusSet());
        
        if (LOGGER.isTraceEnabled()) {
            // Dump the maps
            LOGGER.trace("Account maps:\nZERO:\n{}\nPLUS:\n{}\nMINUS:\n{}\n", new Object[]{dumpAccountMap(zeroAccountMap),
                    dumpAccountMap(plusAccountMap), dumpAccountMap(minusAccountMap)});
        }

        // Now we are processing account constructions from all the three sets once again. We will create projection contexts
        // for them if not yet created. Now we will do the usual routing for converting the delta triples to deltas. 
        // I.e. zero means unchanged, plus means added, minus means deleted. That will be recorded in the SynchronizationPolicyDecision.
        // We will also collect all the construction triples to projection context. These will be used later for computing
        // actual attribute deltas (in consolidation processor).
        Collection<ResourceShadowDiscriminator> allAccountTypes = MiscUtil.union(zeroAccountMap.keySet(), plusAccountMap.keySet(), minusAccountMap.keySet());
        for (ResourceShadowDiscriminator rat : allAccountTypes) {

            if (rat.getResourceOid() == null) {
                throw new IllegalStateException("Resource OID null in ResourceAccountType during assignment processing");
            }
            if (rat.getIntent() == null) {
                throw new IllegalStateException("Account type is null in ResourceAccountType during assignment processing");
            }

            // SITUATION: The projection should exist, there is NO CHANGE in assignments
            if (zeroAccountMap.containsKey(rat)) {
            	
                LensProjectionContext<ShadowType> accountSyncContext = context.findProjectionContext(rat);
                if (accountSyncContext == null) {
                	// The projection should exist before the change but it does not
                	// This happens during reconciliation if there is an inconsistency. 
                	// Pretend that the assignment was just added. That should do.
                	accountSyncContext = LensUtil.getOrCreateAccountContext(context, rat);
                }
            	accountSyncContext.setLegal(true);
            	accountSyncContext.setLegalOld(true);
            	accountSyncContext.setAssigned(true);

                
            // SITUATION: The projection is both ASSIGNED and UNASSIGNED
            } else if (plusAccountMap.containsKey(rat) && minusAccountMap.containsKey(rat)) {
            	
            	LensProjectionContext<ShadowType> projectionContext = context.findProjectionContext(rat);
            	projectionContext.setAssigned(true);
            	projectionContext.setLegal(true);
            	projectionContext.setLegalOld(true);
                // Account was removed and added in the same operation, therefore keep its original state
                // TODO
                throw new UnsupportedOperationException("add+delete of projection is not supported yet");
                //continue;

                
            // SITUATION: The projection is ASSIGNED
            } else if (plusAccountMap.containsKey(rat)) {
            	
            	LensProjectionContext<ShadowType> projectionContext = LensUtil.getOrCreateAccountContext(context, rat);
            	projectionContext.setAssigned(true);
            	projectionContext.setLegalOld(false);
            	AssignmentPolicyEnforcementType assignmentPolicyEnforcement = projectionContext.getAssignmentPolicyEnforcementType();
            	if (assignmentPolicyEnforcement != AssignmentPolicyEnforcementType.NONE) {
            		projectionContext.setLegal(true);
            	}

        	// SITUATION: The projection is UNASSIGNED
            } else if (minusAccountMap.containsKey(rat)) {
            	
            	if (accountExists(context,rat)) {
            		LensProjectionContext<ShadowType> projectionContext = LensUtil.getOrCreateAccountContext(context, rat);
            		projectionContext.setAssigned(false);
            		projectionContext.setLegalOld(true);
            		
            		AssignmentPolicyEnforcementType assignmentPolicyEnforcement = projectionContext.getAssignmentPolicyEnforcementType();
            		// TODO: check for MARK and LEGALIZE enforcement policies ....add delete laso for relative enforcemenet
            		if (assignmentPolicyEnforcement == AssignmentPolicyEnforcementType.FULL 
            				|| assignmentPolicyEnforcement == AssignmentPolicyEnforcementType.RELATIVE) {
	                	projectionContext.setLegal(false);
            		} else {
	                	projectionContext.setLegal(true);
            		}
            	} else {

            		// We have to delete something that is not there. Nothing to do.
            	}

            } else {
                throw new IllegalStateException("Projection " + rat + " went looney");
            }

            PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> accountDeltaSetTriple = 
            		new PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>>(
            				getConstructions(zeroAccountMap.get(rat)),
            				getConstructions(plusAccountMap.get(rat)),
            				getConstructions(minusAccountMap.get(rat)));
            LensProjectionContext<ShadowType> accountContext = context.findProjectionContext(rat);
            if (accountContext != null) {
            	// This can be null in a exotic case if we delete already deleted account
            	accountContext.setAccountConstructionDeltaSetTriple(accountDeltaSetTriple);
            	if (isForceRecon(zeroAccountMap.get(rat)) || isForceRecon(plusAccountMap.get(rat)) || isForceRecon(minusAccountMap.get(rat))) {
            		accountContext.setDoReconciliation(true);
            	}
            }

        }
        
        removeIgnoredContexts(context);
        finishLegalDecisions(context);
        
    }
    
	/**
	 * Simply mark all projections as illegal - except those that are being unliked
	 */
	private void processFocusDelete(LensContext<UserType, ShadowType> context, OperationResult result) {
		for (LensProjectionContext<ShadowType> projectionContext: context.getProjectionContexts()) {
			if (projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.UNLINK) {
				// We do not want to affect unliked projections
				continue;
			}
			projectionContext.setLegal(false);
			projectionContext.setLegalOld(true);
		}
	}

	private String determineResource(AssignmentType assignmentType) {
		ConstructionType construction = assignmentType.getConstruction();
		if (construction == null) {
			construction = assignmentType.getAccountConstruction();
		}
		if (construction != null){
			if (construction.getResource() != null){
				return construction.getResource().getOid();
			} else if (construction.getResourceRef() != null){
				return construction.getResourceRef().getOid();
			} 
			
			return null;
		}
		
		return null;
	}
	
	private String determineIntent(AssignmentType assignmentType) {
		ConstructionType construction = assignmentType.getConstruction();
		if (construction == null) {
			construction = assignmentType.getAccountConstruction();
		}
		if (construction != null){
			if (construction.getIntent() != null){
				return construction.getIntent();
			} 
			
			return "default";
		}
		
		throw new IllegalArgumentException("Construction not defined in the assigment.");
	}

	private Collection<PrismPropertyValue<AccountConstruction>> getConstructions(AccountConstructionPack accountConstructionPack) {
		if (accountConstructionPack == null) {
			return null;
		}
		return accountConstructionPack.getConstructions();
	}
	
	private boolean isForceRecon(AccountConstructionPack accountConstructionPack) {
		if (accountConstructionPack == null) {
			return false;
		}
		return accountConstructionPack.isForceRecon();
	}


	private void applyAssignemntMicroDeltas(Collection<? extends ItemDelta<?>> assignmentItemDeltas, PrismContainerValue<AssignmentType> assignmentCVal) throws SchemaException {
		for (ItemDelta<?> assignmentItemDelta: assignmentItemDeltas) {
			ItemDelta<?> assignmentItemDeltaClone = assignmentItemDelta.clone();
			ItemPath deltaPath = assignmentItemDeltaClone.getParentPath();
			ItemPath tailPath = deltaPath.tail();
			if (tailPath.first() instanceof IdItemPathSegment) {
				tailPath = tailPath.tail();
			}
			assignmentItemDeltaClone.setParentPath(tailPath);
			assignmentItemDeltaClone.applyTo(assignmentCVal);
		}
	}

	/**
	 * Set 'legal' flag for the accounts that does not have it already 
	 */
	private void finishLegalDecisions(LensContext<UserType,ShadowType> context) throws PolicyViolationException, SchemaException {
		for (LensProjectionContext<ShadowType> projectionContext: context.getProjectionContexts()) {
			
			if (projectionContext.isLegal() != null) {
				// already have decision
				continue;
			}
		
			if (projectionContext.isLegalize()){
				createAssignmentDelta(context, projectionContext);
				projectionContext.setAssigned(true);
				projectionContext.setLegal(true);
				projectionContext.setLegalOld(false);
			} else {
			
				AssignmentPolicyEnforcementType enforcementType = projectionContext.getAssignmentPolicyEnforcementType();
				
				if (enforcementType == AssignmentPolicyEnforcementType.FULL) {
					// What is not explicitly allowed is illegal in FULL enforcement mode
					projectionContext.setLegal(false);
					// We need to set the old value for legal to false. There was no assignment delta for it.
					// If it were then the code could not get here.
					projectionContext.setLegalOld(false);
					if (projectionContext.isAdd()) {
						throw new PolicyViolationException("Attempt to add projection "+projectionContext.toHumanReadableString()
								+" while the synchronization enforcement policy is FULL and the projection is not assigned");
					}
					
				} else if (enforcementType == AssignmentPolicyEnforcementType.NONE && !projectionContext.isThombstone()) {
					if (projectionContext.isAdd()) {
						projectionContext.setLegal(true);
						projectionContext.setLegalOld(false);
					} else {
						// Everything that exists was legal and is legal. Nothing really changes.
						projectionContext.setLegal(projectionContext.isExists());
						projectionContext.setLegalOld(projectionContext.isExists());
					}
				
				} else if (enforcementType == AssignmentPolicyEnforcementType.POSITIVE && !projectionContext.isThombstone()) {
					// Everything that is not yet dead is legal in POSITIVE enforcement mode
					projectionContext.setLegal(true);
					projectionContext.setLegalOld(true);
					
				} else if (enforcementType == AssignmentPolicyEnforcementType.RELATIVE && !projectionContext.isThombstone() &&
						projectionContext.isLegal() == null && projectionContext.isLegalOld() == null) {
					// RELATIVE mode and nothing has changed. Maintain status quo. Pretend that it is legal.
					projectionContext.setLegal(true);
					projectionContext.setLegalOld(true);
				}
			}
			
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Finishing legal decision for {}, thombstone {}, enforcement mode {}, legalize {}: {} -> {}",
						new Object[]{projectionContext.toHumanReadableString(), projectionContext.isThombstone(),
						projectionContext.getAssignmentPolicyEnforcementType(),
						projectionContext.isLegalize(), projectionContext.isLegalOld(), projectionContext.isLegal()});
			}
		}
	}

	private <F extends ObjectType, P extends ObjectType, T extends ObjectType> void createAssignmentDelta(LensContext<F, P> context, LensProjectionContext<T> accountContext) throws SchemaException{
		ContainerDelta<AssignmentType> assignmentDelta = ContainerDelta.createDelta(prismContext, UserType.class, UserType.F_ASSIGNMENT);
		AssignmentType assignmet = new AssignmentType();
		ConstructionType constructionType = new ConstructionType();
		constructionType.setResourceRef(ObjectTypeUtil.createObjectRef(accountContext.getResource()));
		assignmet.setConstruction(constructionType);
		assignmentDelta.addValueToAdd(assignmet.asPrismContainerValue());
		assignmentDelta.applyDefinition(prismContext.getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(UserType.class)
				.findContainerDefinition(UserType.F_ASSIGNMENT));
		context.getFocusContext().swallowToProjectionWaveSecondaryDelta(assignmentDelta);//, context.getProjectionWave());//addSecondaryDelta(assignmentDelta);
		
	}

	private boolean containsRealValue(Collection<PrismContainerValue<AssignmentType>> assignmentValuesCollection,
			PrismContainerValue<AssignmentType> assignmentValue) {
		for (PrismContainerValue<AssignmentType> colValue: assignmentValuesCollection) {
			if (colValue.equalsRealValue(assignmentValue)) {
				return true;
			}
		}
		return false;
	}
	
	public <F extends ObjectType, P extends ObjectType> void processOrgAssignments(LensContext<F,P> context, 
			OperationResult result) throws SchemaException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		DeltaSetTriple<Assignment> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
		if (focusContext == null || evaluatedAssignmentTriple == null) {
			return;
		}
		
		PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
		PrismReferenceDefinition orgRefDef = userDef.findReferenceDefinition(UserType.F_PARENT_ORG_REF);
		ItemPath orgRefPath = new ItemPath(UserType.F_PARENT_ORG_REF);
		
		// Plus
		for (Assignment assignment: evaluatedAssignmentTriple.getPlusSet()) {
			Collection<PrismReferenceValue> orgs = assignment.getOrgRefVals();
			for (PrismReferenceValue org: orgs) {
				ItemDelta orgRefDelta = orgRefDef.createEmptyDelta(orgRefPath);
				orgRefDelta.addValueToAdd(org.toCannonical());
				focusContext.swallowToProjectionWaveSecondaryDelta(orgRefDelta);
			}
		}
		
		// Minus
		for (Assignment assignment: evaluatedAssignmentTriple.getMinusSet()) {
			Collection<PrismReferenceValue> orgs = assignment.getOrgRefVals();
			for (PrismReferenceValue org: orgs) {
				ItemDelta orgRefDelta = orgRefDef.createEmptyDelta(orgRefPath);
				orgRefDelta.addValueToDelete(org.toCannonical());
				focusContext.swallowToProjectionWaveSecondaryDelta(orgRefDelta);
			}
		}
		
		// TODO: zero set if reconciliation?
	}
	
	public <F extends ObjectType, P extends ObjectType> void checkForAssignmentConflicts(LensContext<F,P> context, 
			OperationResult result) throws PolicyViolationException {
		for(LensProjectionContext<P> projectionContext: context.getProjectionContexts()) {
			if (AssignmentPolicyEnforcementType.NONE == projectionContext.getAssignmentPolicyEnforcementType()){
				continue;
			}
			if (projectionContext.isAssigned()) {
				ObjectDelta<P> projectionPrimaryDelta = projectionContext.getPrimaryDelta();
				if (projectionPrimaryDelta != null) {
					if (projectionPrimaryDelta.isDelete()) {
						throw new PolicyViolationException("Attempt to delete "+projectionContext.getHumanReadableName()+" while " +
								"it is assigned violates an assignment policy");
					}
				}
			}
		}
	}
	

	public void processAssignmentsAccountValues(LensProjectionContext<ShadowType> accountContext, OperationResult result) throws SchemaException,
		ObjectNotFoundException, ExpressionEvaluationException {
            
		// TODO: reevaluate constructions
		// This should re-evaluate all the constructions. They are evaluated already, evaluated in the assignment step before.
		// But if there is any iteration counter that it will not be taken into account
		
    }

    private void collectToAccountMap(LensContext<UserType,ShadowType> context,
            Map<ResourceShadowDiscriminator, AccountConstructionPack> accountMap, Assignment evaluatedAssignment, 
            boolean forceRecon, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        for (AccountConstruction accountConstruction : evaluatedAssignment.getAccountConstructions()) {
            String resourceOid = accountConstruction.getResource(result).getOid();
            String accountType = accountConstruction.getAccountType();
            ResourceType resource = LensUtil.getResource(context, resourceOid, provisioningService, result);
            accountType = LensUtil.refineAccountType(accountType, resource, prismContext);
            ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceOid, accountType);
            AccountConstructionPack constructionPack = null;
            if (accountMap.containsKey(rat)) {
                constructionPack = accountMap.get(rat);
            } else {
                constructionPack = new AccountConstructionPack();
                accountMap.put(rat, constructionPack);
            }
            constructionPack.add(new PrismPropertyValue<AccountConstruction>(accountConstruction));
            if (forceRecon) {
            	constructionPack.setForceRecon(true);
            }
        }
    }

    private String dumpAccountMap(Map<ResourceShadowDiscriminator, AccountConstructionPack> accountMap) {
        StringBuilder sb = new StringBuilder();
        Set<Entry<ResourceShadowDiscriminator, AccountConstructionPack>> entrySet = accountMap.entrySet();
        Iterator<Entry<ResourceShadowDiscriminator, AccountConstructionPack>> i = entrySet.iterator();
        while (i.hasNext()) {
            Entry<ResourceShadowDiscriminator, AccountConstructionPack> entry = i.next();
            sb.append(entry.getKey()).append(": ");
            sb.append(entry.getValue());
            if (i.hasNext()) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private boolean accountExists(LensContext<UserType,ShadowType> context, ResourceShadowDiscriminator rat) {
    	LensProjectionContext<ShadowType> accountSyncContext = context.findProjectionContext(rat);
    	if (accountSyncContext == null) {
    		return false;
    	}
    	if (accountSyncContext.getObjectCurrent() == null) {
    		return false;
    	}
    	return true;
    }
        
    private void markPolicyDecision(LensProjectionContext<ShadowType> accountSyncContext, SynchronizationPolicyDecision decision) {
        if (accountSyncContext.getSynchronizationPolicyDecision() == null) {
            accountSyncContext.setSynchronizationPolicyDecision(decision);
        }
    }

	private void checkExclusions(LensContext<UserType,ShadowType> context, Collection<Assignment> assignmentsA,
			Collection<Assignment> assignmentsB) throws PolicyViolationException {
		for (Assignment assignmentA: assignmentsA) {
			checkExclusion(context, assignmentA, assignmentsB);
		}
	}

	private void checkExclusion(LensContext<UserType,ShadowType> context, Assignment assignmentA,
			Collection<Assignment> assignmentsB) throws PolicyViolationException {
		for (Assignment assignmentB: assignmentsB) {
			checkExclusion(context, assignmentA, assignmentB);
		}
	}

	private void checkExclusion(LensContext<UserType,ShadowType> context, Assignment assignmentA, Assignment assignmentB) throws PolicyViolationException {
		if (assignmentA == assignmentB) {
			// Same thing, this cannot exclude itself
			return;
		}
		for(AccountConstruction constructionA: assignmentA.getAccountConstructions()) {
			for(AccountConstruction constructionB: assignmentB.getAccountConstructions()) {
				checkExclusion(constructionA, assignmentA, constructionB, assignmentB);
			}
		}
	}

	private void checkExclusion(AccountConstruction constructionA, Assignment assignmentA,
			AccountConstruction constructionB, Assignment assignmentB) throws PolicyViolationException {
		AssignmentPath pathA = constructionA.getAssignmentPath();
		AssignmentPath pathB = constructionB.getAssignmentPath();
		for (AssignmentPathSegment segmentA: pathA.getSegments()) {
			if (segmentA.getTarget() != null && segmentA.getTarget() instanceof AbstractRoleType) {
				for (AssignmentPathSegment segmentB: pathB.getSegments()) {
					if (segmentB.getTarget() != null && segmentB.getTarget() instanceof AbstractRoleType) {
						checkExclusion((AbstractRoleType)segmentA.getTarget(), (AbstractRoleType)segmentB.getTarget());
					}
				}
			}
		}
	}

	private void checkExclusion(AbstractRoleType roleA, AbstractRoleType roleB) throws PolicyViolationException {
		checkExclusionOneWay(roleA, roleB);
		checkExclusionOneWay(roleB, roleA);
	}

	private void checkExclusionOneWay(AbstractRoleType roleA, AbstractRoleType roleB) throws PolicyViolationException {
		for (ExclusionType exclusionA :roleA.getExclusion()) {
			ObjectReferenceType targetRef = exclusionA.getTargetRef();
			if (roleB.getOid().equals(targetRef.getOid())) {
				throw new PolicyViolationException("Violation of SoD policy: "+roleA+" excludes "+roleB+
						", they cannot be assigned at the same time");
			}
		}
	}
	

	public <F extends ObjectType, P extends ObjectType> void removeIgnoredContexts(LensContext<F, P> context) {
		Collection<LensProjectionContext<P>> projectionContexts = context.getProjectionContexts();
		Iterator<LensProjectionContext<P>> projectionIterator = projectionContexts.iterator();
		while (projectionIterator.hasNext()) {
			LensProjectionContext<P> projectionContext = projectionIterator.next();
			
			if (projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE) {
				projectionIterator.remove();
			}
		}
	}

}
