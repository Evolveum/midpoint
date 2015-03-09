/*
 * Copyright (c) 2010-2015 Evolveum
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.controller.ModelUtils;
import com.evolveum.midpoint.model.impl.lens.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.AssignmentPath;
import com.evolveum.midpoint.model.impl.lens.AssignmentPathSegment;
import com.evolveum.midpoint.model.impl.lens.Construction;
import com.evolveum.midpoint.model.impl.lens.ConstructionPack;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaMapTriple;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MultiplicityPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Assignment processor is recomputing user assignments. It recomputes all the assignements whether they are direct
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
    private MappingFactory mappingFactory;
    
    @Autowired(required = true)
    private ProvisioningService provisioningService;
    
    @Autowired(required = true)
	private ActivationComputer activationComputer;
    
    @Autowired(required = true)
    private ObjectTemplateProcessor objectTemplateProcessor;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentProcessor.class);

    /**
     * Processing all the assignments to determine which projections should be added, deleted or kept as they are.
     * Generic method for all projection types (theoretically). 
     */
    public <O extends ObjectType> void processAssignmentsProjections(LensContext<O> context, XMLGregorianCalendar now,
            Task task, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
    	LensFocusContext<O> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
    	if (!FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
    		// We can do this only for FocusType.
    		return;
    	}
//    	if (ModelExecuteOptions.isLimitPropagation(context.getOptions()) && SchemaConstants.CHANGE_CHANNEL_DISCOVERY.equals(QNameUtil.uriToQName(context.getChannel()))){
//    		//do not execute assignment if the execution was triggered by compensation mechanism and limitPropagation is set
//    		return;
//    	}
    	processAssignmentsProjectionsWithFocus((LensContext<? extends FocusType>)context, now, task, result);
    }
    
    /**
     * Processing focus-projection assignments (including roles).
     */
    private <F extends FocusType> void processAssignmentsProjectionsWithFocus(LensContext<F> context, XMLGregorianCalendar now, 
    		Task task, OperationResult result) throws SchemaException,
    		ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
    	
    	LensFocusContext<F> focusContext = context.getFocusContext();
        ObjectDelta<F> focusDelta = focusContext.getDelta();
        
    	if (focusDelta != null && focusDelta.isDelete()) {
			processFocusDelete(context, result);
			return;
		}

        checkAssignmentDeltaSanity(context);
        
        // Normal processing. The enforcement policy requires that assigned accounts should be added, so we need to figure out
        // which assignments were added. Do a complete recompute for all the enforcement modes. We can do that because this does
        // not create deltas, it just creates the triples. So we can decide what to do later when we convert triples to deltas.
        
        Collection<PrismContainerValue<AssignmentType>> assignmentsCurrent = new ArrayList<PrismContainerValue<AssignmentType>>();
        if (focusContext.getObjectCurrent() != null) {
            PrismContainer<AssignmentType> assignmentContainer = focusContext.getObjectCurrent().findContainer(FocusType.F_ASSIGNMENT);
            if (assignmentContainer != null) {
            	assignmentsCurrent.addAll(assignmentContainer.getValues());
            }
        }

        ContainerDelta<AssignmentType> assignmentDelta = getExecutionWaveAssignmentDelta(focusContext);
        assignmentDelta.expand(focusContext.getObjectCurrent());

        LOGGER.trace("Assignment delta:\n{}", assignmentDelta.debugDump());

        Collection<PrismContainerValue<AssignmentType>> changedAssignments = assignmentDelta.getValues(AssignmentType.class);
        // Changes assignments may be "light", i.e. they may contain just the identifier. Make sure that we always have
        // the full assignment data.
        Iterator<PrismContainerValue<AssignmentType>> iterator = changedAssignments.iterator();
        while (iterator.hasNext()) {
        	PrismContainerValue<AssignmentType> changedAssignment = iterator.next();
        	if (changedAssignment.getItems().isEmpty()) {
        		if (changedAssignment.getId() != null) {
        			for (PrismContainerValue<AssignmentType> assignmentCurrent: assignmentsCurrent) {
        				if (changedAssignment.getId().equals(assignmentCurrent.getId())) {
        					iterator.remove();
        					changedAssignments.add(assignmentCurrent.clone());
        				}
        			}
        		}
        	}
        }

        // Initializing assignment evaluator. This will be used later to process all the assignments including the nested
        // assignments (roles).
        AssignmentEvaluator<F> assignmentEvaluator = new AssignmentEvaluator<>();
        assignmentEvaluator.setRepository(repositoryService);
        assignmentEvaluator.setFocusOdo(focusContext.getObjectDeltaObject());
        assignmentEvaluator.setLensContext(context);
        assignmentEvaluator.setChannel(context.getChannel());
        assignmentEvaluator.setObjectResolver(objectResolver);
        assignmentEvaluator.setPrismContext(prismContext);
        assignmentEvaluator.setMappingFactory(mappingFactory);
        assignmentEvaluator.setActivationComputer(activationComputer);
        assignmentEvaluator.setNow(now);
        assignmentEvaluator.setSystemConfiguration(context.getSystemConfiguration());

        // We will be collecting the evaluated account constructions into these three maps. 
        // It forms a kind of delta set triple for the account constructions.
        DeltaMapTriple<ResourceShadowDiscriminator, ConstructionPack> constructionMapTriple = new DeltaMapTriple<>();

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Current assignments {}", SchemaDebugUtil.prettyPrint(assignmentsCurrent));
			LOGGER.trace("Changed assignments {}", SchemaDebugUtil.prettyPrint(changedAssignments));
		}

//        ObjectType source = determineSource(focusContext);
        ObjectType source = null;
        if (focusContext.getObjectCurrent() != null) {
            source = focusContext.getObjectCurrent().asObjectable();
        } else if (focusContext.getObjectNew() != null){
            source = focusContext.getObjectNew().asObjectable();
        }
        
        DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple = new DeltaSetTriple<>();
        context.setEvaluatedAssignmentTriple((DeltaSetTriple)evaluatedAssignmentTriple);
        
        // Iterate over all the assignments. I mean really all. This is a union of the existing and changed assignments
        // therefore it contains all three types of assignments (plus, minus and zero). As it is an union each assignment
        // will be processed only once. Inside the loop we determine whether it was added, deleted or remains unchanged.
        // This is a first step of the processing. It takes all the account constructions regardless of the resource and
        // account type (intent). Therefore several constructions for the same resource and intent may appear in the resulting
        // sets. This is not good as we want only a single account for each resource/intent combination. But that will be
        // sorted out later.
        Collection<PrismContainerValue<AssignmentType>> allAssignments = mergeAssignments(assignmentsCurrent, changedAssignments);
        for (PrismContainerValue<AssignmentType> assignmentCVal : allAssignments) {
            AssignmentType assignmentType = assignmentCVal.asContainerable();
            PrismContainerValue<AssignmentType> assignmentCValOld = assignmentCVal;
            PrismContainerValue<AssignmentType> assignmentCValNew = assignmentCVal;
            ItemDeltaItem<PrismContainerValue<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
            assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
            
            boolean forceRecon = false;
            // This really means whether the WHOLE assignment was changed (e.g. added/delted/replaced). It tells nothing
            // about "micro-changes" inside assignment, these will be processed later.
            boolean isAssignmentChanged = containsRealValue(changedAssignments, assignmentCVal);
            String assignmentPlacementDesc;
            
            if (isAssignmentChanged) {
            	// Whole assignment added or deleted
            	assignmentPlacementDesc = "delta for "+source;
            } else {
            	assignmentPlacementDesc = source.toString();
            	Collection<? extends ItemDelta<?>> assignmentItemDeltas = getExecutionWaveAssignmentItemDeltas(focusContext, assignmentCVal.getId());
            	if (assignmentItemDeltas != null && !assignmentItemDeltas.isEmpty()) {
            		// Small changes inside assignment, but otherwise the assignment stays as it is (not added or deleted)
            		assignmentIdi.setSubItemDeltas(assignmentItemDeltas);
            		
            		// The subItemDeltas above will handle some changes. But not other.
            		// E.g. a replace of the whole construction will not be handled properly.
            		// Therefore we force recon to sort it out.
	            	forceRecon = true;
	            	
	            	isAssignmentChanged = true;
	            	PrismContainer<AssignmentType> assContNew = focusContext.getObjectNew().findContainer(FocusType.F_ASSIGNMENT);
	            	assignmentCValNew = assContNew.getValue(assignmentCVal.getId());
            	}
            }
            
            assignmentIdi.recompute();
            
            // The following code is using collectToAccountMap() to collect the account constructions to one of the three "delta"
            // sets (zero, plus, minus). It is handling several situations that needs to be handled specially.
            // It is also collecting assignments to evaluatedAssignmentTriple.
            
            if (focusDelta != null && focusDelta.isDelete()) {
            	
            	// USER DELETE
            	// If focus (user) is being deleted that all the assignments are to be gone. Including those that
            	// were not changed explicitly.
            	if (LOGGER.isTraceEnabled()) {
            		LOGGER.trace("Processing focus delete for: {}", SchemaDebugUtil.prettyPrint(assignmentCVal));
            	}
            	EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(assignmentIdi, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
                if (evaluatedAssignment == null) {
                	continue;
                }
                collectToMinus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
                
            } else {
            	if (assignmentDelta.isReplace()) {

            		if (LOGGER.isTraceEnabled()) {
            			LOGGER.trace("Processing replace of all assignments for: {}", SchemaDebugUtil.prettyPrint(assignmentCVal));
            		}
            		// ASSIGNMENT REPLACE
            		// Handling assignment replace delta. This needs to be handled specially as all the "old"
            		// assignments should be considered deleted - except those that are part of the new value set
            		// (remain after replace). As account delete and add are costly operations (and potentially dangerous)
            		// we optimize here are consider the assignments that were there before replace and still are there
            		// after it as unchanged.
            		boolean hadValue = containsRealValue(assignmentsCurrent, assignmentCVal);
            		boolean willHaveValue = assignmentDelta.isValueToReplace(assignmentCVal, true);
            		if (hadValue && willHaveValue) {
            			// No change
            			EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(assignmentIdi, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
                        if (evaluatedAssignment == null) {
                        	continue;
                        }
    	                collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
            		} else if (willHaveValue) {
            			// add
            			EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(assignmentIdi, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
                        if (evaluatedAssignment == null) {
                        	continue;
                        }
	                    collectToPlus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
            		} else if (hadValue) {
            			// delete
            			EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(assignmentIdi, true, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
                        if (evaluatedAssignment == null) {
                        	continue;
                        }
	                    collectToMinus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
            		} else {
            			throw new SystemException("Whoops. Unexpected things happen. Assignment is not old nor new (replace delta)");
            		}
            		
            	} else {

            		// ADD/DELETE of entire assignment or small changes inside existing assignments 
            		// This is the usual situation.
		            // Just sort assignments to sets: unchanged (zero), added (plus), removed (minus)
		            if (isAssignmentChanged) {
		                // There was some change
		
		            	boolean isAdd = assignmentDelta.isValueToAdd(assignmentCVal, true);
		            	boolean isDelete = assignmentDelta.isValueToDelete(assignmentCVal, true);
		                if (isAdd & !isDelete) {
		                	// Entirely new assignment is added
		                	if (containsRealValue(assignmentsCurrent, assignmentCVal)) {
		                		// Phantom add: adding assignment that is already there
		                		if (LOGGER.isTraceEnabled()) {
				            		LOGGER.trace("Processing changed assignment, phantom add: {}", SchemaDebugUtil.prettyPrint(assignmentCVal));
				            	}
		                		EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(assignmentIdi, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
		                        if (evaluatedAssignment == null) {
		                        	continue;
		                        }
		                        collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
		                	} else {
		                		if (LOGGER.isTraceEnabled()) {
				            		LOGGER.trace("Processing changed assignment, add: {}", SchemaDebugUtil.prettyPrint(assignmentCVal));
				            	}
		                		EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(assignmentIdi, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
		                        if (evaluatedAssignment == null) {
		                        	continue;
		                        }
			                    collectToPlus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
		                	}

		                } else if (isDelete && !isAdd) {
		                	// Existing assignment is removed
		                	if (LOGGER.isTraceEnabled()) {
			            		LOGGER.trace("Processing changed assignment, delete: {}", SchemaDebugUtil.prettyPrint(assignmentCVal));
			            	}
		                	EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(assignmentIdi, true, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
		                    if (evaluatedAssignment == null) {
		                    	continue;
		                    }
		                    collectToMinus(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
		                    
		                } else {
		                	// Small change inside an assignment
		                	// The only thing that we need to worry about is assignment validity change. That is a cause
		                	// of provisioning/deprovisioning of the projections. So check that explicitly. Other changes are
		                	// not significant, i.e. reconciliation can handle them.
		                	boolean isValidOld = LensUtil.isValid(assignmentCValOld.asContainerable(), now, activationComputer);
		                	boolean isValid = LensUtil.isValid(assignmentCValNew.asContainerable(), now, activationComputer);
		                	if (isValid == isValidOld) {
		                		// No change in validity -> right to the zero set
			                	// The change is not significant for assignment applicability. Recon will sort out the details.
		                		if (LOGGER.isTraceEnabled()) {
				            		LOGGER.trace("Processing changed assignment, minor change (add={}, delete={}, valid={}): {}", 
				            				new Object[]{isAdd, isDelete, isValid, SchemaDebugUtil.prettyPrint(assignmentCVal)});
				            	}
		                		EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(assignmentIdi, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
		                        if (evaluatedAssignment == null) {
		                        	continue;
		                        }
				                collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, true);
		                	} else if (isValid) {
		                		// Assignment became valid. We need to place it in plus set to initiate provisioning
		                		if (LOGGER.isTraceEnabled()) {
				            		LOGGER.trace("Processing changed assignment, assignment becomes valid (add={}, delete={}): {}", 
				            				new Object[]{isAdd, isDelete, SchemaDebugUtil.prettyPrint(assignmentCVal)});
				            	}
		                		EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(assignmentIdi, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
		                        if (evaluatedAssignment == null) {
		                        	continue;
		                        }
			                    collectToPlus(evaluatedAssignmentTriple, evaluatedAssignment, true);
		                	} else {
		                		// Assignment became invalid. We need to place is in minus set to initiate deprovisioning
		                		if (LOGGER.isTraceEnabled()) {
				            		LOGGER.trace("Processing changed assignment, assignment becomes invalid (add={}, delete={}): {}", 
				            				new Object[]{isAdd, isDelete, SchemaDebugUtil.prettyPrint(assignmentCVal)});
				            	}
		                		EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(assignmentIdi, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
		                        if (evaluatedAssignment == null) {
		                        	continue;
		                        }
		                        collectToMinus(evaluatedAssignmentTriple, evaluatedAssignment, true);
		                	}
		                }
		
		            } else {
		                // No change in assignment
		            	if (LOGGER.isTraceEnabled()) {
		            		LOGGER.trace("Processing unchanged assignment {}", new Object[]{SchemaDebugUtil.prettyPrint(assignmentCVal)});
		            	}
		            	boolean isValid = LensUtil.isValid(assignmentType, now, activationComputer);   // TODO unused: why? [med]
		            	EvaluatedAssignmentImpl<F> evaluatedAssignment = evaluateAssignment(assignmentIdi, false, context, source, assignmentEvaluator, assignmentPlacementDesc, task, result);
		                if (evaluatedAssignment == null) {
		                	continue;
		                }
		                collectToZero(evaluatedAssignmentTriple, evaluatedAssignment, forceRecon);
		            }
            	}
            }
        }
        
        if (LOGGER.isTraceEnabled()) {
        	LOGGER.trace("evaluatedAssignmentTriple:\n{}", evaluatedAssignmentTriple.debugDump());
        }
        
        // PROCESSING POLICIES
        
        // Checking for assignment exclusions. This means mostly role exclusions (SoD) 
        checkExclusions(context, evaluatedAssignmentTriple.getZeroSet(), evaluatedAssignmentTriple.getPlusSet());
        checkExclusions(context, evaluatedAssignmentTriple.getPlusSet(), evaluatedAssignmentTriple.getPlusSet());
        checkAssigneeConstraints(context, evaluatedAssignmentTriple, result);
        
        // PROCESSING FOCUS
        
        Map<ItemPath,DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> focusOutputTripleMap = new HashMap<>();
        collectFocusTripleFromMappings(evaluatedAssignmentTriple.getPlusSet(), focusOutputTripleMap, PlusMinusZero.PLUS);
        collectFocusTripleFromMappings(evaluatedAssignmentTriple.getMinusSet(), focusOutputTripleMap, PlusMinusZero.MINUS);
        collectFocusTripleFromMappings(evaluatedAssignmentTriple.getZeroSet(), focusOutputTripleMap, PlusMinusZero.ZERO);
        ObjectDeltaObject<F> focusOdo = focusContext.getObjectDeltaObject();
		Collection<ItemDelta<? extends PrismValue>> focusDeltas = objectTemplateProcessor.computeItemDeltas(focusOutputTripleMap,
				focusOdo, focusContext.getObjectDefinition(), "focus mappings in assignments of "+focusContext.getHumanReadableName());
		LOGGER.trace("Computed focus deltas: {}", focusDeltas);
		focusContext.applyProjectionWaveSecondaryDeltas(focusDeltas);
		focusContext.recompute();

		
        // PROCESSING PROJECTIONS
		
		// Evaluate the constructions in assignements now. These were not evaluated in the first pass of AssignmentEvaluator
		// because there may be interaction from focusMappings of some roles to outbound mappings of other roles.
		// Now we have complete focus with all the focusMappings so we can evaluate the constructions
		evaluateConstructions(context, evaluatedAssignmentTriple, task, result);
		collectToConstructionMaps(context, evaluatedAssignmentTriple, constructionMapTriple, result);
        
        if (LOGGER.isTraceEnabled()) {
            // Dump the maps
            LOGGER.trace("constructionMapTriple:\n{}", constructionMapTriple.debugDump());
        }
        
        
        
        // Now we are processing constructions from all the three sets once again. We will create projection contexts
        // for them if not yet created. Now we will do the usual routing for converting the delta triples to deltas. 
        // I.e. zero means unchanged, plus means added, minus means deleted. That will be recorded in the SynchronizationPolicyDecision.
        // We will also collect all the construction triples to projection context. These will be used later for computing
        // actual attribute deltas (in consolidation processor).
        Collection<ResourceShadowDiscriminator> allAccountTypes = constructionMapTriple.unionKeySets();
        for (ResourceShadowDiscriminator rat : allAccountTypes) {
        
            if (rat.getResourceOid() == null) {
                throw new IllegalStateException("Resource OID null in ResourceAccountType during assignment processing");
            }
            if (rat.getIntent() == null) {
                throw new IllegalStateException("Account type is null in ResourceAccountType during assignment processing");
            }
            
            boolean processOnlyExistingProjCxts = false;
            if (ModelExecuteOptions.isLimitPropagation(context.getOptions())){
				if (context.getTriggeredResourceOid() != null
						&& !rat.getResourceOid().equals(context.getTriggeredResourceOid())) {
					LOGGER.trace(
							"Skipping processing construction for shadow identified by {} because of limitation to propagate changes only for resource {}",
							rat, context.getTriggeredResourceOid());
					continue;
				}
				
				if (SchemaConstants.CHANGE_CHANNEL_DISCOVERY.equals(QNameUtil.uriToQName(context.getChannel()))){
					LOGGER.trace("Processing of shadow identified by {} will be skipped because of limitation for discovery channel.");	// TODO is this message OK? [med]
					processOnlyExistingProjCxts = true;
				}
            }
            
            String desc = rat.toHumanReadableString();

            // SITUATION: The projection is ASSIGNED
            if (constructionMapTriple.getPlusMap().containsKey(rat)) {
        	
	        	ConstructionPack constructionPack = constructionMapTriple.getPlusMap().get(rat);
	            if (constructionPack.hasValidAssignment()) {
	            	LensProjectionContext projectionContext = LensUtil.getOrCreateProjectionContext(context, rat);
	            	projectionContext.setAssigned(true);
                    projectionContext.setAssignedOld(false);
	            	projectionContext.setLegalOld(false);
	            	AssignmentPolicyEnforcementType assignmentPolicyEnforcement = projectionContext.getAssignmentPolicyEnforcementType();
	            	if (assignmentPolicyEnforcement != AssignmentPolicyEnforcementType.NONE) {
	            		LOGGER.trace("Projection {} legal: assigned (valid)", desc);
	            		projectionContext.setLegal(true);
	            	}
	            } else {
	            	// Just ignore it, do not even create projection context
	            	LOGGER.trace("Projection {} ignoring: assigned (invalid)", desc);
	            }
            
            // SITUATION: The projection should exist (is valid), there is NO CHANGE in assignments
            } else if (constructionMapTriple.getZeroMap().containsKey(rat) && constructionMapTriple.getZeroMap().get(rat).hasValidAssignment()) {
            	
                LensProjectionContext projectionContext = context.findProjectionContext(rat);
                if (projectionContext == null) {
                	if (processOnlyExistingProjCxts){
                		continue;
                	}
                	// The projection should exist before the change but it does not
                	// This happens during reconciliation if there is an inconsistency. 
                	// Pretend that the assignment was just added. That should do.
                	projectionContext = LensUtil.getOrCreateProjectionContext(context, rat);
                }
            	LOGGER.trace("Projection {} legal: unchanged (valid)", desc);
            	projectionContext.setLegal(true);
            	projectionContext.setLegalOld(true);
            	projectionContext.setAssigned(true);
                projectionContext.setAssignedOld(true);
                
            // SITUATION: The projection is both ASSIGNED and UNASSIGNED
            } else if (constructionMapTriple.getPlusMap().containsKey(rat) && constructionMapTriple.getMinusMap().containsKey(rat)) {
            	// Account was removed and added in the same operation. This is the case if e.g. one role is
            	// removed and another is added and they include the same account.
            	// Keep original account state
            	
            	ConstructionPack plusPack = constructionMapTriple.getPlusMap().get(rat);
            	ConstructionPack minusPack = constructionMapTriple.getMinusMap().get(rat);
            	
            	if (plusPack.hasValidAssignment() && minusPack.hasValidAssignment()) {
            	
	            	LensProjectionContext projectionContext = context.findProjectionContext(rat);
	            	if (projectionContext == null) {
	            		if (processOnlyExistingProjCxts){
	                		continue;
	                	}
	                	// The projection should exist before the change but it does not
	                	// This happens during reconciliation if there is an inconsistency. 
	                	// Pretend that the assignment was just added. That should do.
	            		projectionContext = LensUtil.getOrCreateProjectionContext(context, rat);
	                }
	            	LOGGER.trace("Projection {} legal: both assigned and unassigned (valid)", desc);
	            	projectionContext.setAssigned(true);
                    projectionContext.setAssignedOld(true);
	            	projectionContext.setLegal(true);
	            	projectionContext.setLegalOld(true);
	            	
            	} else if (!plusPack.hasValidAssignment() && !minusPack.hasValidAssignment()) {
            		// Just ignore it, do not even create projection context
                	LOGGER.trace("Projection {} ignoring: both assigned and unassigned (invalid)", desc);
                	
            	} else if (plusPack.hasValidAssignment() && !minusPack.hasValidAssignment()) {
            		// Assignment became valid. Same as if it was assigned.
            		LensProjectionContext projectionContext = context.findProjectionContext(rat);
            		if (projectionContext == null){
            			if (processOnlyExistingProjCxts){
                    		continue;
                    	}
            			projectionContext = LensUtil.getOrCreateProjectionContext(context, rat);
            		}
            		projectionContext.setAssigned(true);
                    projectionContext.setAssignedOld(false);
	            	projectionContext.setLegalOld(false);
	            	AssignmentPolicyEnforcementType assignmentPolicyEnforcement = projectionContext.getAssignmentPolicyEnforcementType();
	            	if (assignmentPolicyEnforcement != AssignmentPolicyEnforcementType.NONE) {
	            		LOGGER.trace("Projection {} legal: both assigned and unassigned (invalid->valid)", desc);
	            		projectionContext.setLegal(true);
	            	}
	            	
            	} else if (!plusPack.hasValidAssignment() && minusPack.hasValidAssignment()) {
            		// Assignment became invalid. Same as if it was unassigned.
            		if (accountExists(context,rat)) {
            			LensProjectionContext projectionContext = context.findProjectionContext(rat);
                		if (projectionContext == null){
                			if (processOnlyExistingProjCxts){
                        		continue;
                        	}
                			projectionContext = LensUtil.getOrCreateProjectionContext(context, rat);
                		}
                		projectionContext.setAssigned(false);
                        projectionContext.setAssignedOld(true);
                		projectionContext.setLegalOld(true);
                		
                		AssignmentPolicyEnforcementType assignmentPolicyEnforcement = projectionContext.getAssignmentPolicyEnforcementType();
                		// TODO: check for MARK and LEGALIZE enforcement policies ....add delete laso for relative enforcemenet
                		if (assignmentPolicyEnforcement == AssignmentPolicyEnforcementType.FULL 
                				|| assignmentPolicyEnforcement == AssignmentPolicyEnforcementType.RELATIVE) {
                			LOGGER.trace("Projection {} illegal: both assigned and unassigned (valid->invalid)", desc);
    	                	projectionContext.setLegal(false);
                		} else {
                			LOGGER.trace("Projection {} legal: both assigned and unassigned (valid->invalid), but allowed by policy ({})", desc, assignmentPolicyEnforcement);
    	                	projectionContext.setLegal(true);
                		}
                	} else {

                		LOGGER.trace("Projection {} nothing: both assigned and unassigned (valid->invalid) but not there", desc);
                		// We have to delete something that is not there. Nothing to do.
                	}

            	} else {
            		throw new IllegalStateException("Whoops!?!");
            	}

        	// SITUATION: The projection is UNASSIGNED
            } else if (constructionMapTriple.getMinusMap().containsKey(rat)) {
            	
            	if (accountExists(context,rat)) {
            		LensProjectionContext projectionContext = LensUtil.getOrCreateProjectionContext(context, rat);
            		projectionContext.setAssigned(false);
                    projectionContext.setAssignedOld(true);
            		projectionContext.setLegalOld(true);
            		
            		AssignmentPolicyEnforcementType assignmentPolicyEnforcement = projectionContext.getAssignmentPolicyEnforcementType();
            		// TODO: check for MARK and LEGALIZE enforcement policies ....add delete laso for relative enforcemenet
            		if (assignmentPolicyEnforcement == AssignmentPolicyEnforcementType.FULL 
            				|| assignmentPolicyEnforcement == AssignmentPolicyEnforcementType.RELATIVE) {
            			LOGGER.trace("Projection {} illegal: unassigned", desc);
	                	projectionContext.setLegal(false);
            		} else {
            			LOGGER.trace("Projection {} legal: unassigned, but allowed by policy ({})", desc, assignmentPolicyEnforcement);
	                	projectionContext.setLegal(true);
            		}
            	} else {

            		LOGGER.trace("Projection {} nothing: unassigned but not there", desc);
            		// We have to delete something that is not there. Nothing to do.
            	}

            // SITUATION: The projection should exist (invalid), there is NO CHANGE in assignments
            } else if (constructionMapTriple.getZeroMap().containsKey(rat) && !constructionMapTriple.getZeroMap().get(rat).hasValidAssignment()) {
            	
                LensProjectionContext projectionContext = context.findProjectionContext(rat);
                if (projectionContext == null) {
                	if (processOnlyExistingProjCxts){
                		continue;
                	}
                	// The projection should exist before the change but it does not
                	// This happens during reconciliation if there is an inconsistency. 
                	// Pretend that the assignment was just added. That should do.
                	projectionContext = LensUtil.getOrCreateProjectionContext(context, rat);
                }
            	LOGGER.trace("Projection {} illegal: unchanged (invalid)", desc);
            	projectionContext.setLegal(false);
            	projectionContext.setLegalOld(false);
            	projectionContext.setAssigned(false);
                projectionContext.setAssignedOld(false);

            } else {
                throw new IllegalStateException("Projection " + desc + " went looney");
            }

            PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountDeltaSetTriple = 
            		new PrismValueDeltaSetTriple<PrismPropertyValue<Construction>>(
            				getConstructions(constructionMapTriple.getZeroMap().get(rat), true),
            				getConstructions(constructionMapTriple.getPlusMap().get(rat), true),
            				getConstructions(constructionMapTriple.getMinusMap().get(rat), false));
            LensProjectionContext accountContext = context.findProjectionContext(rat);
            if (accountContext != null) {
            	// This can be null in a exotic case if we delete already deleted account
            	accountContext.setConstructionDeltaSetTriple(accountDeltaSetTriple);
            	if (isForceRecon(constructionMapTriple.getZeroMap().get(rat)) || isForceRecon(constructionMapTriple.getPlusMap().get(rat)) || isForceRecon(constructionMapTriple.getMinusMap().get(rat))) {
            		accountContext.setDoReconciliation(true);
            	}
            }

        }
        
        removeIgnoredContexts(context);
        finishLegalDecisions(context);
        
    }

    /**
     * Checks if we do not try to modify assignment.targetRef or assignment.construction.kind or intent.
     *
     * @param context
     * @param <F>
     * @throws SchemaException
     */
    private <F extends FocusType> void checkAssignmentDeltaSanity(LensContext<F> context) throws SchemaException {
        ObjectDelta<F> focusDelta = context.getFocusContext().getDelta();
        if (focusDelta == null || !focusDelta.isModify() || focusDelta.getModifications() == null) {
            return;
        }

        final ItemPath TARGET_REF_PATH = new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF);
        final ItemPath CONSTRUCTION_KIND_PATH = new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_KIND);
        final ItemPath CONSTRUCTION_INTENT_PATH = new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_INTENT);

        for (ItemDelta itemDelta : focusDelta.getModifications()) {
            ItemPath itemPath = itemDelta.getPath().namedSegmentsOnly();
            if (TARGET_REF_PATH.isSubPathOrEquivalent(itemPath)) {
                throw new SchemaException("It is not allowed to change targetRef in an assignment. Offending path: " + itemPath);
            }
            if (CONSTRUCTION_KIND_PATH.isSubPathOrEquivalent(itemPath)) {
                throw new SchemaException("It is not allowed to change construction.kind in an assignment. Offending path: " + itemPath);
            }
            if (CONSTRUCTION_INTENT_PATH.isSubPathOrEquivalent(itemPath)) {
                throw new SchemaException("It is not allowed to change construction.intent in an assignment. Offending path: " + itemPath);
            }
            // TODO some mechanism to detect changing kind/intent by add/delete/replace whole ConstructionType (should be implemented in the caller)
        }
    }

    private <F extends ObjectType> ObjectType determineSource(LensFocusContext<F> focusContext)
			throws SchemaException {
		ObjectDelta delta = focusContext.getWaveDelta(focusContext.getLensContext().getExecutionWave());
		if (delta != null && !delta.isEmpty()) {
			return focusContext.getObjectNew().asObjectable();
		}

		if (focusContext.getObjectCurrent() != null) {
			return focusContext.getObjectCurrent().asObjectable();
		}

		return focusContext.getObjectNew().asObjectable();
	}
    
    private <F extends FocusType> void collectToZero(DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple, 
    		EvaluatedAssignmentImpl<F> evaluatedAssignment, boolean forceRecon) {
        if (forceRecon) {
            evaluatedAssignment.setForceRecon(true);
        }
    	evaluatedAssignmentTriple.addToZeroSet(evaluatedAssignment);
    }

    private <F extends FocusType> void collectToPlus(DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple, 
    		EvaluatedAssignmentImpl<F> evaluatedAssignment, boolean forceRecon) {
        if (forceRecon) {
            evaluatedAssignment.setForceRecon(true);
        }
    	evaluatedAssignmentTriple.addToPlusSet(evaluatedAssignment);
    }

    private <F extends FocusType> void collectToMinus(DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple, 
    		EvaluatedAssignmentImpl<F> evaluatedAssignment, boolean forceRecon) {
        if (forceRecon) {
            evaluatedAssignment.setForceRecon(true);
        }
    	evaluatedAssignmentTriple.addToMinusSet(evaluatedAssignment);
    }

    private <F extends FocusType> void evaluateConstructions(LensContext<F> context, 
    		DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
    	evaluateConstructions(context, evaluatedAssignmentTriple.getZeroSet(), task, result);
    	evaluateConstructions(context, evaluatedAssignmentTriple.getPlusSet(), task, result);
    	evaluateConstructions(context, evaluatedAssignmentTriple.getMinusSet(), task, result);
    }
    
    private <F extends FocusType> void evaluateConstructions(LensContext<F> context, 
    		Collection<EvaluatedAssignmentImpl<F>> evaluatedAssignments, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
    	if (evaluatedAssignments == null) {
    		return;
    	}
    	ObjectDeltaObject<F> focusOdo = null;
    	LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext != null) {
    		focusOdo = focusContext.getObjectDeltaObject();
    	}
    	Iterator<EvaluatedAssignmentImpl<F>> iterator = evaluatedAssignments.iterator();
    	while (iterator.hasNext()) {
    		EvaluatedAssignmentImpl<F> evaluatedAssignment = iterator.next();
    		try {
				evaluatedAssignment.evaluateConstructions(focusOdo, task, result);
    		} catch (ObjectNotFoundException ex){
            	if (LOGGER.isTraceEnabled()) {
                	LOGGER.trace("Processing of assignment resulted in error {}: {}", ex, SchemaDebugUtil.prettyPrint(evaluatedAssignment.getAssignmentType()));
                }
            	iterator.remove();
            	if (!ModelExecuteOptions.isForce(context.getOptions())){
            		ModelUtils.recordFatalError(result, ex);
            	}
            } catch (SchemaException ex){
            	if (LOGGER.isTraceEnabled()) {
                	LOGGER.trace("Processing of assignment resulted in error {}: {}", ex, SchemaDebugUtil.prettyPrint(evaluatedAssignment.getAssignmentType()));
                }
            	ModelUtils.recordFatalError(result, ex);
            	String resourceOid = determineResource(evaluatedAssignment.getAssignmentType());
            	if (resourceOid == null) {
            		// This is a role assignment or something like that. Just throw the original exception for now.
            		throw ex;
            	}
            	ResourceShadowDiscriminator rad = new ResourceShadowDiscriminator(resourceOid, 
            			determineKind(evaluatedAssignment.getAssignmentType()), determineIntent(evaluatedAssignment.getAssignmentType()));
    			LensProjectionContext accCtx = context.findProjectionContext(rad);
    			if (accCtx != null) {
    				accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
    			}
    			iterator.remove();
            }
    	}
    }
    
    private <F extends FocusType> void collectToConstructionMaps(LensContext<F> context,
    		DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple, 
    		DeltaMapTriple<ResourceShadowDiscriminator, ConstructionPack> constructionMapTriple,
    		OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
    	
    	collectToConstructionMapFromEvaluatedAssignments(context, evaluatedAssignmentTriple.getZeroSet(), constructionMapTriple, PlusMinusZero.ZERO, result);
    	collectToConstructionMapFromEvaluatedAssignments(context, evaluatedAssignmentTriple.getPlusSet(), constructionMapTriple, PlusMinusZero.PLUS, result);
    	collectToConstructionMapFromEvaluatedAssignments(context, evaluatedAssignmentTriple.getMinusSet(), constructionMapTriple, PlusMinusZero.MINUS, result);
    }
    
    private <F extends FocusType> void collectToConstructionMapFromEvaluatedAssignments(LensContext<F> context,
    		Collection<EvaluatedAssignmentImpl<F>> evaluatedAssignments,
    		DeltaMapTriple<ResourceShadowDiscriminator, ConstructionPack> constructionMapTriple, PlusMinusZero mode,
    		OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		for (EvaluatedAssignmentImpl<F> evaluatedAssignment: evaluatedAssignments) {
	    	if (LOGGER.isTraceEnabled()) {
	    		LOGGER.trace("Collecting constructions from evaluated assignment:\n{}", evaluatedAssignment.debugDump());
	    	}
	    	DeltaSetTriple<Construction<F>> constructionTriple = evaluatedAssignment.getConstructions();
	    	collectToConstructionMapFromEvaluatedConstructions(context, evaluatedAssignment, constructionTriple.getZeroSet(), constructionMapTriple, mode, PlusMinusZero.ZERO, result);
	    	collectToConstructionMapFromEvaluatedConstructions(context, evaluatedAssignment, constructionTriple.getPlusSet(), constructionMapTriple, mode, PlusMinusZero.PLUS, result);
	    	collectToConstructionMapFromEvaluatedConstructions(context, evaluatedAssignment, constructionTriple.getMinusSet(), constructionMapTriple, mode, PlusMinusZero.MINUS, result);
		}
    }
    
    private <F extends FocusType> void collectToConstructionMapFromEvaluatedConstructions(LensContext<F> context,
    		EvaluatedAssignmentImpl<F> evaluatedAssignment,
    		Collection<Construction<F>> evaluatedConstructions,
    		DeltaMapTriple<ResourceShadowDiscriminator, ConstructionPack> constructionMapTriple, 
    		PlusMinusZero mode1, PlusMinusZero mode2,
    		OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
    	
        for (Construction<F> construction : evaluatedConstructions) {
        	
        	PlusMinusZero mode = PlusMinusZero.compute(mode1, mode2);
        	Map<ResourceShadowDiscriminator, ConstructionPack> constructionMap = constructionMapTriple.getMap(mode);
        	if (constructionMap == null) {
        		continue;
        	}
        	
            String resourceOid = construction.getResource(result).getOid();
            String intent = construction.getIntent();
            ShadowKindType kind = construction.getKind();
            ResourceType resource = LensUtil.getResource(context, resourceOid, provisioningService, result);
            intent = LensUtil.refineProjectionIntent(kind, intent, resource, prismContext);
            ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceOid, kind, intent);
            ConstructionPack constructionPack = null;
            if (constructionMap.containsKey(rat)) {
                constructionPack = constructionMap.get(rat);
            } else {
                constructionPack = new ConstructionPack();
                constructionMap.put(rat, constructionPack);
            }
            constructionPack.add(new PrismPropertyValue<Construction>(construction));
            if (evaluatedAssignment.isValid()) {
            	constructionPack.setHasValidAssignment(true);
            }
            if (evaluatedAssignment.isForceRecon()) {
            	constructionPack.setForceRecon(true);
            }
        }
	}
    
    

    
	private Collection<PrismContainerValue<AssignmentType>> mergeAssignments(
			Collection<PrismContainerValue<AssignmentType>> currentAssignments,
			Collection<PrismContainerValue<AssignmentType>> changedAssignments) {
		Collection<PrismContainerValue<AssignmentType>> all = new ArrayList<>(currentAssignments.size() + changedAssignments.size());
		all.addAll(currentAssignments);
		for (PrismContainerValue<AssignmentType> changedAssignment: changedAssignments) {
			boolean skip = false;
			for (PrismContainerValue<AssignmentType> currentAssignment: currentAssignments) {
				if (currentAssignment.match(changedAssignment)) {
					skip = true;
					break;
				}
			}
			if (!skip) {
				all.add(changedAssignment);
			}
		}
		return all;
	}
	
	private <F extends FocusType> EvaluatedAssignmentImpl<F> evaluateAssignment(ItemDeltaItem<PrismContainerValue<AssignmentType>> assignmentIdi,
			boolean evaluateOld, LensContext<F> context, ObjectType source, AssignmentEvaluator<F> assignmentEvaluator, 
			String assignmentPlacementDesc, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, PolicyViolationException {
        try{
        	// Evaluate assignment. This follows to the assignment targets, follows to the inducements, 
        	// evaluates all the expressions, etc. 
        	EvaluatedAssignmentImpl<F> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, evaluateOld, source, assignmentPlacementDesc, task, result);
        	context.rememberResources(evaluatedAssignment.getResources(result));
        	return evaluatedAssignment;
        } catch (ObjectNotFoundException ex){
        	if (LOGGER.isTraceEnabled()) {
            	LOGGER.trace("Processing of assignment resulted in error {}: {}", ex, SchemaDebugUtil.prettyPrint(LensUtil.getAssignmentType(assignmentIdi, evaluateOld)));
            }
        	if (ModelExecuteOptions.isForce(context.getOptions())){
        		return null;
        	} 
        	ModelUtils.recordFatalError(result, ex);
        	return null;
        } catch (SchemaException ex){
        	AssignmentType assignmentType = LensUtil.getAssignmentType(assignmentIdi, evaluateOld);
        	if (LOGGER.isTraceEnabled()) {
            	LOGGER.trace("Processing of assignment resulted in error {}: {}", ex, SchemaDebugUtil.prettyPrint(assignmentType));
            }
        	ModelUtils.recordFatalError(result, ex);
        	String resourceOid = determineResource(assignmentType);
        	if (resourceOid == null) {
        		// This is a role assignment or something like that. Just throw the original exception for now.
        		throw ex;
        	}
        	ResourceShadowDiscriminator rad = new ResourceShadowDiscriminator(resourceOid, 
        			determineKind(assignmentType), determineIntent(assignmentType));
			LensProjectionContext accCtx = context.findProjectionContext(rad);
			if (accCtx != null) {
				accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
			}
        	return null;
        }
	}

	/**
	 * Simply mark all projections as illegal - except those that are being unliked
	 */
	private <F extends FocusType> void processFocusDelete(LensContext<F> context, OperationResult result) {
		for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
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
		if (construction != null){
			if (construction.getIntent() != null){
				return construction.getIntent();
			} 
			
			return "default";
		}
		
		throw new IllegalArgumentException("Construction not defined in the assigment.");
	}
	
	private ShadowKindType determineKind(AssignmentType assignmentType) {
		ConstructionType construction = assignmentType.getConstruction();
		if (construction != null){
			if (construction.getKind() != null){
				return construction.getKind();
			} 
			
			return ShadowKindType.ACCOUNT;
		}
		
		throw new IllegalArgumentException("Construction not defined in the assigment.");
	}

	private Collection<PrismPropertyValue<Construction>> getConstructions(ConstructionPack accountConstructionPack, boolean validOnly) {
		if (accountConstructionPack == null) {
			return null;
		}
		if (validOnly && !accountConstructionPack.hasValidAssignment()) {
			return null;
		}
		return accountConstructionPack.getConstructions();
	}
	
	private boolean isForceRecon(ConstructionPack accountConstructionPack) {
		if (accountConstructionPack == null) {
			return false;
		}
		return accountConstructionPack.isForceRecon();
	}

	/**
	 * Set 'legal' flag for the accounts that does not have it already 
	 */
	private <F extends FocusType> void finishLegalDecisions(LensContext<F> context) throws PolicyViolationException, SchemaException {
		for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
			
			if (projectionContext.isLegal() != null) {
				// already have decision
				propagateLegalDecisionToHigherOrders(context, projectionContext);
				continue;
			}
		
			String desc = projectionContext.toHumanReadableString();
			if (projectionContext.isLegalize()){
				LOGGER.trace("Projection {} legal: legalized", desc);
				createAssignmentDelta(context, projectionContext);
				projectionContext.setAssigned(true);
                projectionContext.setAssignedOld(false);
				projectionContext.setLegal(true);
				projectionContext.setLegalOld(false);
			} else {
			
				AssignmentPolicyEnforcementType enforcementType = projectionContext.getAssignmentPolicyEnforcementType();
				
				if (enforcementType == AssignmentPolicyEnforcementType.FULL) {
					LOGGER.trace("Projection {} illegal: no assignment in FULL enforcement", desc);
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
						LOGGER.trace("Projection {} legal: added in NONE policy", desc);
						projectionContext.setLegal(true);
						projectionContext.setLegalOld(false);
					} else {
						if (projectionContext.isExists()) {
							LOGGER.trace("Projection {} legal: exists in NONE policy", desc);
						} else {
							LOGGER.trace("Projection {} illegal: does not exists in NONE policy", desc);
						}
						// Everything that exists was legal and is legal. Nothing really changes.
						projectionContext.setLegal(projectionContext.isExists());
						projectionContext.setLegalOld(projectionContext.isExists());
					}
				
				} else if (enforcementType == AssignmentPolicyEnforcementType.POSITIVE && !projectionContext.isThombstone()) {
					// Everything that is not yet dead is legal in POSITIVE enforcement mode
					LOGGER.trace("Projection {} legal: not dead in POSITIVE policy", desc);
					projectionContext.setLegal(true);
					projectionContext.setLegalOld(true);
					
				} else if (enforcementType == AssignmentPolicyEnforcementType.RELATIVE && !projectionContext.isThombstone() &&
						projectionContext.isLegal() == null && projectionContext.isLegalOld() == null) {
					// RELATIVE mode and nothing has changed. Maintain status quo. Pretend that it is legal.
					LOGGER.trace("Projection {} legal: no change in RELATIVE policy", desc);
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
			
			propagateLegalDecisionToHigherOrders(context, projectionContext);
		}
	}

	private <F extends ObjectType> void propagateLegalDecisionToHigherOrders(
			LensContext<F> context, LensProjectionContext refProjCtx) {
		ResourceShadowDiscriminator refDiscr = refProjCtx.getResourceShadowDiscriminator();
		if (refDiscr == null) {
			return;
		}
		for (LensProjectionContext aProjCtx: context.getProjectionContexts()) {
			ResourceShadowDiscriminator aDiscr = aProjCtx.getResourceShadowDiscriminator();
			if (aDiscr != null && refDiscr.equivalent(aDiscr) && (refDiscr.getOrder() < aDiscr.getOrder())) {
				aProjCtx.setLegal(refProjCtx.isLegal());
				aProjCtx.setLegalOld(refProjCtx.isLegalOld());
				aProjCtx.setExists(refProjCtx.isExists());
			}
		}
	}

	private <F extends FocusType, T extends ObjectType> void createAssignmentDelta(LensContext<F> context, LensProjectionContext accountContext) throws SchemaException{
        Class<F> focusClass = context.getFocusClass();
        ContainerDelta<AssignmentType> assignmentDelta = ContainerDelta.createDelta(FocusType.F_ASSIGNMENT, focusClass, prismContext);
		AssignmentType assignment = new AssignmentType();
		ConstructionType constructionType = new ConstructionType();
		constructionType.setResourceRef(ObjectTypeUtil.createObjectRef(accountContext.getResource()));
		assignment.setConstruction(constructionType);
		assignmentDelta.addValueToAdd(assignment.asPrismContainerValue());
		assignmentDelta.applyDefinition(prismContext.getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(focusClass)
				.findContainerDefinition(FocusType.F_ASSIGNMENT));
		context.getFocusContext().swallowToProjectionWaveSecondaryDelta(assignmentDelta);
		
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
	
	public <F extends ObjectType> void processOrgAssignments(LensContext<F> context, 
			OperationResult result) throws SchemaException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		DeltaSetTriple<EvaluatedAssignmentImpl> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
		if (focusContext == null || evaluatedAssignmentTriple == null) {
			return;
		}

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Starting processing org assignments into parentOrgRef delta(s); evaluatedAssignmentTriple is:\n{}",
                    evaluatedAssignmentTriple.debugDump());
        }

        Class<F> focusClass = focusContext.getObjectTypeClass();
        PrismObjectDefinition<F> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusClass);
		PrismReferenceDefinition orgRefDef = userDef.findReferenceDefinition(FocusType.F_PARENT_ORG_REF);
		ItemPath orgRefPath = new ItemPath(FocusType.F_PARENT_ORG_REF);

        // check if parentOrgRef recon is needed - it is when something inside OrgType assignment has changed
        boolean forceRecon = false;
        for (EvaluatedAssignmentImpl assignment: evaluatedAssignmentTriple.getAllValues()) {
            if (assignment.isForceRecon() &&
                    assignment.getAssignmentType() != null &&
                    assignment.getAssignmentType().getTargetRef() != null &&
                    OrgType.COMPLEX_TYPE.equals(assignment.getAssignmentType().getTargetRef().getType())) {
                forceRecon = true;
                break;
            }
        }
        // for zero and minus sets we check isForceRecon for all non-construction-related assignments (MID-2242)
        if (!forceRecon) {
            for (EvaluatedAssignmentImpl assignment: evaluatedAssignmentTriple.getNonPositiveValues()) {
                if (assignment.isForceRecon() &&
                        (assignment.getConstructions() == null || assignment.getConstructions().isEmpty())) {
                    forceRecon = true;
                    break;
                }
            }
        }

        if (!forceRecon) {      // if no recon, we simply add/delete values as needed

            LOGGER.trace("No reconciliation requested, processing plus and minus sets");

            // A list of values that are _not_ to be removed - these are all the values from zero set,
            // as well as values from plus set.
            //
            // Contrary to existing standard delta merge algorithm (where add+delete means "keep the current state"),
            // we ignore any delete of values that should be existing or added.

            Collection<PrismReferenceValue> notToBeDeletedCanonical = new HashSet<>();
            for (EvaluatedAssignmentImpl assignment : evaluatedAssignmentTriple.getZeroSet()) {
                Collection<PrismReferenceValue> orgs = assignment.getOrgRefVals();
                for (PrismReferenceValue org : orgs) {
                    notToBeDeletedCanonical.add(org.toCannonical());
                }
            }

            // Plus
            for (EvaluatedAssignmentImpl assignment : evaluatedAssignmentTriple.getPlusSet()) {
                Collection<PrismReferenceValue> orgs = assignment.getOrgRefVals();
                for (PrismReferenceValue org : orgs) {
                    ItemDelta orgRefDelta = orgRefDef.createEmptyDelta(orgRefPath);
                    PrismReferenceValue orgCanonical = org.toCannonical();
                    orgRefDelta.addValueToAdd(orgCanonical);
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Created parentOrgRef delta:\n{}", orgRefDelta.debugDump());
                    }
                    focusContext.swallowToProjectionWaveSecondaryDelta(orgRefDelta);

                    notToBeDeletedCanonical.add(orgCanonical);
                }
            }

            // Minus (except for these that are also in zero set)
            for (EvaluatedAssignmentImpl assignment : evaluatedAssignmentTriple.getMinusSet()) {
                Collection<PrismReferenceValue> orgs = assignment.getOrgRefVals();
                for (PrismReferenceValue org : orgs) {
                    ItemDelta orgRefDelta = orgRefDef.createEmptyDelta(orgRefPath);
                    PrismReferenceValue orgCanonical = org.toCannonical();
                    if (notToBeDeletedCanonical.contains(orgCanonical)) {
                        LOGGER.trace("Not removing {} because it is in the zero or plus set", orgCanonical);
                    } else {
                        orgRefDelta.addValueToDelete(orgCanonical);
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Created parentOrgRef delta:\n{}", orgRefDelta.debugDump());
                        }
                        focusContext.swallowToProjectionWaveSecondaryDelta(orgRefDelta);
                    }
                }
            }

        } else {        // if reconciliation is requested, we recreate parentOrgRef from scratch

            LOGGER.trace("Reconciliation requested, collecting all non-negative values");

            ItemDelta orgRefDelta = orgRefDef.createEmptyDelta(orgRefPath);
            Set<PrismReferenceValue> valuesToReplace = new HashSet<>();

            for (EvaluatedAssignmentImpl assignment : evaluatedAssignmentTriple.getNonNegativeValues()) {
                Collection<PrismReferenceValue> orgs = assignment.getOrgRefVals();
                for (PrismReferenceValue org : orgs) {
                    PrismReferenceValue canonical = org.toCannonical();
                    valuesToReplace.add(canonical);     // if valuesToReplace would be a list, we should check for duplicates!
                }
            }
            orgRefDelta.setValuesToReplace(valuesToReplace);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Created parentOrgRef delta:\n{}", orgRefDelta.debugDump());
            }
            focusContext.swallowToProjectionWaveSecondaryDelta(orgRefDelta);
        }
        LOGGER.trace("Processing org assignments into parentOrgRef delta(s) done.");
	}
	
	public <F extends ObjectType> void checkForAssignmentConflicts(LensContext<F> context, 
			OperationResult result) throws PolicyViolationException {
		for(LensProjectionContext projectionContext: context.getProjectionContexts()) {
			if (AssignmentPolicyEnforcementType.NONE == projectionContext.getAssignmentPolicyEnforcementType()){
				continue;
			}
			if (projectionContext.isAssigned()) {
				ObjectDelta<ShadowType> projectionPrimaryDelta = projectionContext.getPrimaryDelta();
				if (projectionPrimaryDelta != null) {
					if (projectionPrimaryDelta.isDelete()) {
						throw new PolicyViolationException("Attempt to delete "+projectionContext.getHumanReadableName()+" while " +
								"it is assigned violates an assignment policy");
					}
				}
			}
		}
	}
	

	public void processAssignmentsAccountValues(LensProjectionContext accountContext, OperationResult result) throws SchemaException,
		ObjectNotFoundException, ExpressionEvaluationException {
            
		// TODO: reevaluate constructions
		// This should re-evaluate all the constructions. They are evaluated already, evaluated in the assignment step before.
		// But if there is any iteration counter that it will not be taken into account
		
    }

    private String dumpAccountMap(Map<ResourceShadowDiscriminator, ConstructionPack> accountMap) {
        StringBuilder sb = new StringBuilder();
        Set<Entry<ResourceShadowDiscriminator, ConstructionPack>> entrySet = accountMap.entrySet();
        Iterator<Entry<ResourceShadowDiscriminator, ConstructionPack>> i = entrySet.iterator();
        while (i.hasNext()) {
            Entry<ResourceShadowDiscriminator, ConstructionPack> entry = i.next();
            sb.append(entry.getKey()).append(": ");
            sb.append(entry.getValue());
            if (i.hasNext()) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private <F extends ObjectType> boolean accountExists(LensContext<F> context, ResourceShadowDiscriminator rat) {
    	LensProjectionContext accountSyncContext = context.findProjectionContext(rat);
    	if (accountSyncContext == null) {
    		return false;
    	}
    	if (accountSyncContext.getObjectCurrent() == null) {
    		return false;
    	}
    	return true;
    }
        
    private void markPolicyDecision(LensProjectionContext accountSyncContext, SynchronizationPolicyDecision decision) {
        if (accountSyncContext.getSynchronizationPolicyDecision() == null) {
            accountSyncContext.setSynchronizationPolicyDecision(decision);
        }
    }

	private <F extends FocusType> void checkExclusions(LensContext<F> context, Collection<EvaluatedAssignmentImpl<F>> assignmentsA,
			Collection<EvaluatedAssignmentImpl<F>> assignmentsB) throws PolicyViolationException {
		for (EvaluatedAssignmentImpl<F> assignmentA: assignmentsA) {
			checkExclusion(context, assignmentA, assignmentsB);
		}
	}

	private <F extends FocusType> void checkExclusion(LensContext<F> context, EvaluatedAssignmentImpl<F> assignmentA,
			Collection<EvaluatedAssignmentImpl<F>> assignmentsB) throws PolicyViolationException {
		for (EvaluatedAssignmentImpl<F> assignmentB: assignmentsB) {
			checkExclusion(context, assignmentA, assignmentB);
		}
	}

	private <F extends FocusType> void checkExclusion(LensContext<F> context, EvaluatedAssignmentImpl<F> assignmentA, EvaluatedAssignmentImpl<F> assignmentB) throws PolicyViolationException {
		if (assignmentA == assignmentB) {
			// Same thing, this cannot exclude itself
			return;
		}
		for(Construction<F> constructionA: assignmentA.getConstructions().getNonNegativeValues()) {
			for(Construction<F> constructionB: assignmentB.getConstructions().getNonNegativeValues()) {
				checkExclusion(constructionA, assignmentA, constructionB, assignmentB);
			}
		}
	}

	private <F extends FocusType> void checkExclusion(Construction<F> constructionA, EvaluatedAssignment<F> assignmentA,
			Construction<F> constructionB, EvaluatedAssignment<F> assignmentB) throws PolicyViolationException {
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
		PolicyConstraintsType policyConstraints = roleA.getPolicyConstraints();
		if (policyConstraints != null) {
			for (ExclusionPolicyConstraintType exclusionA : policyConstraints.getExclusion()) {
				ObjectReferenceType targetRef = exclusionA.getTargetRef();
				if (roleB.getOid().equals(targetRef.getOid())) {
					if (exclusionA.getEnforcement() == null || exclusionA.getEnforcement() == PolicyConstraintEnforcementType.ENFORCE) {
						throw new PolicyViolationException("Violation of SoD policy: "+roleA+" excludes "+roleB+
								", they cannot be assigned at the same time");
					} else {
						// TODO
					}
				}
			}
		}
		
		// Deprecated
		for (ExclusionPolicyConstraintType exclusionA : roleA.getExclusion()) {
			ObjectReferenceType targetRef = exclusionA.getTargetRef();
			if (roleB.getOid().equals(targetRef.getOid())) {
				throw new PolicyViolationException("Violation of SoD policy: "+roleA+" excludes "+roleB+
						", they cannot be assigned at the same time");
			}
		}
	}
	
	private <F extends FocusType> void checkAssigneeConstraints(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
			OperationResult result) throws PolicyViolationException, SchemaException {
		for (EvaluatedAssignmentImpl<F> assignment: evaluatedAssignmentTriple.union()) {
			if (evaluatedAssignmentTriple.presentInPlusSet(assignment)) {
				checkAssigneeConstraints(context, assignment, PlusMinusZero.PLUS, result);
			} else if (evaluatedAssignmentTriple.presentInZeroSet(assignment)) {
				// No need to check anything here. Maintain status quo.
//				checkAssigneeConstraints(context, assignment, PlusMinusZero.ZERO, result);
			} else {
				checkAssigneeConstraints(context, assignment, PlusMinusZero.MINUS, result);
			}
		}
	}
	
	private <F extends FocusType> void checkAssigneeConstraints(LensContext<F> context, EvaluatedAssignment<F> assignment, PlusMinusZero plusMinus, OperationResult result) throws PolicyViolationException, SchemaException {
		PrismObject<?> target = assignment.getTarget();
		if (target != null) {
			Objectable targetType = target.asObjectable();
			if (targetType instanceof AbstractRoleType) {
				PolicyConstraintsType policyConstraints = ((AbstractRoleType)targetType).getPolicyConstraints();
				if (policyConstraints != null && (!policyConstraints.getMinAssignees().isEmpty() || !policyConstraints.getMaxAssignees().isEmpty())) {
					int assigneeCount = countAssignees((PrismObject<? extends AbstractRoleType>)target, result);
					if (plusMinus == PlusMinusZero.PLUS) {
						assigneeCount++;
					}
					if (plusMinus == PlusMinusZero.MINUS) {
						assigneeCount--;
					}
					for (MultiplicityPolicyConstraintType constraint: policyConstraints.getMinAssignees()) {
						Integer multiplicity = XsdTypeMapper.multiplicityToInteger(constraint.getMultiplicity());
						// Complain only if the situation is getting worse
						if (multiplicity >= 0 && assigneeCount < multiplicity && plusMinus == PlusMinusZero.MINUS) {
							if (constraint.getEnforcement() == null || constraint.getEnforcement() == PolicyConstraintEnforcementType.ENFORCE) {
								throw new PolicyViolationException("Policy violation: "+target+" requires at least "+multiplicity+
										" assignees. The operation would result in "+assigneeCount+" assignees.");
							}
						}
					}
					for (MultiplicityPolicyConstraintType constraint: policyConstraints.getMaxAssignees()) {
						Integer multiplicity = XsdTypeMapper.multiplicityToInteger(constraint.getMultiplicity());
						// Complain only if the situation is getting worse
						if (multiplicity >= 0 && assigneeCount > multiplicity && plusMinus == PlusMinusZero.PLUS) {
							if (constraint.getEnforcement() == null || constraint.getEnforcement() == PolicyConstraintEnforcementType.ENFORCE) {
								throw new PolicyViolationException("Policy violation: "+target+" requires at most "+multiplicity+
										" assignees. The operation would result in "+assigneeCount+" assignees.");
							}
						}
					}
				}
			}
		}
	}

	private int countAssignees(PrismObject<? extends AbstractRoleType> target, OperationResult result) throws SchemaException {
		ObjectFilter filter = RefFilter.createReferenceEqual(
				new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF), UserType.class, prismContext, target.getOid());
		ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		return repositoryService.countObjects(FocusType.class, query, result);
	}

	public <F extends ObjectType> void removeIgnoredContexts(LensContext<F> context) {
		Collection<LensProjectionContext> projectionContexts = context.getProjectionContexts();
		Iterator<LensProjectionContext> projectionIterator = projectionContexts.iterator();
		while (projectionIterator.hasNext()) {
			LensProjectionContext projectionContext = projectionIterator.next();
			
			if (projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE) {
				projectionIterator.remove();
			}
		}
	}
	
	/**
     * Returns delta of user assignments, both primary and secondary (merged together).
     * The returned object is (kind of) immutable. Changing it may do strange things (but most likely the changes will be lost).
     *
     * This is relative to execution wave to avoid re-processing of already executed assignments.
     */
	private <F extends FocusType> ContainerDelta<AssignmentType> getExecutionWaveAssignmentDelta(LensFocusContext<F> focusContext) throws SchemaException {
        ObjectDelta<? extends FocusType> focusDelta = (ObjectDelta<? extends FocusType>) focusContext.getWaveDelta(focusContext.getLensContext().getExecutionWave());
        if (focusDelta == null) {
            return createEmptyAssignmentDelta(focusContext);
        }
        ContainerDelta<AssignmentType> assignmentDelta = focusDelta.findContainerDelta(new ItemPath(FocusType.F_ASSIGNMENT));
        if (assignmentDelta == null) { 
            return createEmptyAssignmentDelta(focusContext);
        }
        return assignmentDelta;
    }
    
	private <F extends FocusType> Collection<? extends ItemDelta<?>> getExecutionWaveAssignmentItemDeltas(LensFocusContext<F> focusContext, Long id) throws SchemaException {
        ObjectDelta<? extends FocusType> focusDelta = (ObjectDelta<? extends FocusType>) focusContext.getWaveDelta(focusContext.getLensContext().getExecutionWave());
        if (focusDelta == null) {
            return null;
        }
        return focusDelta.findItemDeltasSubPath(new ItemPath(new NameItemPathSegment(FocusType.F_ASSIGNMENT),
        									  new IdItemPathSegment(id)));
	}

    private <F extends FocusType> ContainerDelta<AssignmentType> createEmptyAssignmentDelta(LensFocusContext<F> focusContext) {
        return new ContainerDelta<AssignmentType>(getAssignmentContainerDefinition(focusContext), prismContext);
    }
    
    private <F extends FocusType> PrismContainerDefinition<AssignmentType> getAssignmentContainerDefinition(LensFocusContext<F> focusContext) {
		return focusContext.getObjectDefinition().findContainerDefinition(FocusType.F_ASSIGNMENT);
	}
    
    private <V extends PrismValue, F extends FocusType> XMLGregorianCalendar collectFocusTripleFromMappings(
    		Collection<EvaluatedAssignmentImpl<F>> evaluatedAssignmnents, 
    		Map<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> outputTripleMap,
    		PlusMinusZero plusMinusZero) throws SchemaException {
		
		XMLGregorianCalendar nextRecomputeTime = null;
		
		for (EvaluatedAssignmentImpl<F> ea: evaluatedAssignmnents) {
			Collection<Mapping<V>> focusMappings = (Collection)ea.getFocusMappings();
			for (Mapping<V> mapping: focusMappings) {
				
				ItemPath itemPath = mapping.getOutputPath();
				DeltaSetTriple<ItemValueWithOrigin<V>> outputTriple = ItemValueWithOrigin.createOutputTriple(mapping);
				if (outputTriple == null) {
					continue;
				}
				if (plusMinusZero == PlusMinusZero.PLUS) {
					outputTriple.addAllToPlusSet(outputTriple.getZeroSet());
					outputTriple.clearZeroSet();
					outputTriple.clearMinusSet();
				} else if (plusMinusZero == PlusMinusZero.MINUS) {
					outputTriple.addAllToMinusSet(outputTriple.getZeroSet());
					outputTriple.clearZeroSet();
					outputTriple.clearPlusSet();
				}
				DeltaSetTriple<ItemValueWithOrigin<V>> mapTriple = (DeltaSetTriple<ItemValueWithOrigin<V>>) outputTripleMap.get(itemPath);
				if (mapTriple == null) {
					outputTripleMap.put(itemPath, outputTriple);
				} else {
					mapTriple.merge(outputTriple);
				}
			}
		}
		
		return nextRecomputeTime;
	}

}
