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

import java.util.*;
import java.util.Map.Entry;
import java.util.Objects;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.repo.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.controller.ModelUtils;
import com.evolveum.midpoint.model.impl.lens.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.Construction;
import com.evolveum.midpoint.model.impl.lens.ConstructionPack;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
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
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Assignment processor is recomputing user assignments. It recomputes all the assignments whether they are direct
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

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired
    private ObjectResolver objectResolver;
    
    @Autowired
	private SystemObjectCache systemObjectCache;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private MappingFactory mappingFactory;
    
    @Autowired
    private MappingEvaluator mappingEvaluator;
        
    @Autowired(required=true)
	private ActivationComputer activationComputer;
    
    @Autowired(required=true)
    private ProvisioningService provisioningService;
    
    @Autowired(required=true)
    private ConstructionProcessor constructionProcessor;
    
    @Autowired
    private ObjectTemplateProcessor objectTemplateProcessor;
    
    @Autowired
    private PolicyRuleProcessor policyRuleProcessor;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentProcessor.class);

    /**
     * Processing all the assignments to determine which projections should be added, deleted or kept as they are.
     * Generic method for all projection types (theoretically). 
     */
    @SuppressWarnings("unchecked")
	public <O extends ObjectType> void processAssignmentsProjections(LensContext<O> context, XMLGregorianCalendar now,
            Task task, OperationResult parentResult) throws SchemaException,
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
    	
    	OperationResult result = parentResult.createSubresult(AssignmentProcessor.class.getName() + ".processAssignmentsProjections");
    	
    	try {
    		processAssignmentsProjectionsWithFocus((LensContext<? extends FocusType>)context, now, task, result);
    	} catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | PolicyViolationException | 
    			CommunicationException | ConfigurationException | SecurityViolationException | RuntimeException | Error e) {
    		result.recordFatalError(e);
    		throw e;
    	}
    	
    	OperationResultStatus finalStatus = OperationResultStatus.SUCCESS;
    	String message = null;
    	int errors = 0;
    	for (OperationResult subresult: result.getSubresults()) {
    		if (subresult.isError()) {
    			errors++;
    			if (message == null) {
    				message = subresult.getMessage();
    			} else {
    				message = errors + " errors";
    			}
    			finalStatus = OperationResultStatus.PARTIAL_ERROR;
    		}
    	}
		result.setStatus(finalStatus);
		result.setMessage(message);
		result.cleanupResult();
    }
    
    /**
     * Processing focus-projection assignments (including roles).
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private <F extends FocusType> void processAssignmentsProjectionsWithFocus(LensContext<F> context, XMLGregorianCalendar now, 
    		Task task, OperationResult result) throws SchemaException,
    		ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
    	
    	// PREPARE ASSIGNMENT DELTA
    	
    	LensFocusContext<F> focusContext = context.getFocusContext();
        ObjectDelta<F> focusDelta = focusContext.getDelta();
        
    	if (focusDelta != null && focusDelta.isDelete()) {
			processFocusDelete(context, result);
			return;
		}

        checkAssignmentDeltaSanity(context);
        
        
        // ASSIGNMENT EVALUATION
        
        // Initializing assignment evaluator. This will be used later to process all the assignments including the nested
        // assignments (roles).
        AssignmentEvaluator<F> assignmentEvaluator = createAssignmentEvaluator(context, now);
        ObjectType source = determineSource(focusContext);
        
        AssignmentTripleEvaluator<F> assignmentTripleEvaluator = new AssignmentTripleEvaluator<>();
        assignmentTripleEvaluator.setActivationComputer(activationComputer);
        assignmentTripleEvaluator.setAssignmentEvaluator(assignmentEvaluator);
        assignmentTripleEvaluator.setContext(context);
        assignmentTripleEvaluator.setNow(now);
        assignmentTripleEvaluator.setPrismContext(prismContext);
        assignmentTripleEvaluator.setResult(result);
        assignmentTripleEvaluator.setSource(source);
        assignmentTripleEvaluator.setTask(task);
        
        // Normal processing. The enforcement policy requires that assigned accounts should be added, so we need to figure out
        // which assignments were added. Do a complete recompute for all the enforcement modes. We can do that because this does
        // not create deltas, it just creates the triples. So we can decide what to do later when we convert triples to deltas.
        
        // Evaluates all assignments and sorts them to triple: added, removed and untouched assignments.
        // This is where most of the assignment-level action happens.
        DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple = assignmentTripleEvaluator.processAllAssignments();
        policyRuleProcessor.addGlobalPoliciesToAssignments(context, evaluatedAssignmentTriple, task, result);
        context.setEvaluatedAssignmentTriple((DeltaSetTriple)evaluatedAssignmentTriple);
        
        if (LOGGER.isTraceEnabled()) {
        	LOGGER.trace("evaluatedAssignmentTriple:\n{}", evaluatedAssignmentTriple.debugDump());
        }
        
        // PROCESSING POLICIES

        policyRuleProcessor.processPolicies(context, evaluatedAssignmentTriple, result);
        
        boolean needToReevaluateAssignments = policyRuleProcessor.processPruning(context, evaluatedAssignmentTriple, result);
        
        if (needToReevaluateAssignments) {
        	LOGGER.debug("Re-evaluating assignments because exclusion pruning rule was triggered");
        	
        	assignmentTripleEvaluator.reset();
        	evaluatedAssignmentTriple = assignmentTripleEvaluator.processAllAssignments();
			context.setEvaluatedAssignmentTriple((DeltaSetTriple)evaluatedAssignmentTriple);

			policyRuleProcessor.addGlobalPoliciesToAssignments(context, evaluatedAssignmentTriple, task, result);

        	if (LOGGER.isTraceEnabled()) {
            	LOGGER.trace("re-evaluatedAssignmentTriple:\n{}", evaluatedAssignmentTriple.debugDump());
            }
        	
        	policyRuleProcessor.processPolicies(context, evaluatedAssignmentTriple, result);
        }

        //policyRuleProcessor.storeAssignmentPolicySituation(context, evaluatedAssignmentTriple, result);

        // PROCESSING FOCUS
        
        Map<ItemPath,DeltaSetTriple<? extends ItemValueWithOrigin<?,?>>> focusOutputTripleMap = new HashMap<>();
        collectFocusTripleFromMappings(evaluatedAssignmentTriple.getPlusSet(), focusOutputTripleMap, PlusMinusZero.PLUS);
        collectFocusTripleFromMappings(evaluatedAssignmentTriple.getMinusSet(), focusOutputTripleMap, PlusMinusZero.MINUS);
        collectFocusTripleFromMappings(evaluatedAssignmentTriple.getZeroSet(), focusOutputTripleMap, PlusMinusZero.ZERO);
        ObjectDeltaObject<F> focusOdo = focusContext.getObjectDeltaObject();
		Collection<ItemDelta<?,?>> focusDeltas = objectTemplateProcessor.computeItemDeltas(focusOutputTripleMap, null,
				focusOdo.getObjectDelta(), focusOdo.getNewObject(), focusContext.getObjectDefinition(), 
				"focus mappings in assignments of "+focusContext.getHumanReadableName());
		LOGGER.trace("Computed focus deltas: {}", focusDeltas);
		focusContext.applyProjectionWaveSecondaryDeltas(focusDeltas);
		focusContext.recompute();

		if (context.getPartialProcessingOptions().getProjection() != PartialProcessingTypeType.SKIP) {

			// PROCESSING PROJECTIONS

			// Evaluate the constructions in assignments now. These were not evaluated in the first pass of AssignmentEvaluator
			// because there may be interaction from focusMappings of some roles to outbound mappings of other roles.
			// Now we have complete focus with all the focusMappings so we can evaluate the constructions
			evaluateConstructions(context, evaluatedAssignmentTriple, task, result);

			ComplexConstructionConsumer<ResourceShadowDiscriminator, Construction<F>> consumer = new ComplexConstructionConsumer<ResourceShadowDiscriminator, Construction<F>>() {

				private boolean processOnlyExistingProjCxts;

				@Override
				public boolean before(ResourceShadowDiscriminator rat) {
					if (rat.getResourceOid() == null) {
						throw new IllegalStateException("Resource OID null in ResourceAccountType during assignment processing");
					}
					if (rat.getIntent() == null) {
						throw new IllegalStateException(
								"Account type is null in ResourceAccountType during assignment processing");
					}

					processOnlyExistingProjCxts = false;
					if (ModelExecuteOptions.isLimitPropagation(context.getOptions())) {
						if (context.getTriggeredResourceOid() != null
								&& !rat.getResourceOid().equals(context.getTriggeredResourceOid())) {
							LOGGER.trace(
									"Skipping processing construction for shadow identified by {} because of limitation to propagate changes only for resource {}",
									rat, context.getTriggeredResourceOid());
							return false;
						}

						if (SchemaConstants.CHANGE_CHANNEL_DISCOVERY.equals(QNameUtil.uriToQName(context.getChannel()))) {
							LOGGER.trace(
									"Processing of shadow identified by {} will be skipped because of limitation for discovery channel.");    // TODO is this message OK? [med]
							processOnlyExistingProjCxts = true;
						}
					}

					return true;
				}

				@Override
				public void onAssigned(ResourceShadowDiscriminator rat, String desc) {
					LensProjectionContext projectionContext = LensUtil.getOrCreateProjectionContext(context, rat);
					projectionContext.setAssigned(true);
					projectionContext.setAssignedOld(false);
					projectionContext.setLegalOld(false);
					if (projectionContext.getAssignmentPolicyEnforcementType() != AssignmentPolicyEnforcementType.NONE) {
						LOGGER.trace("Projection {} legal: assigned (valid)", desc);
						projectionContext.setLegal(true);
					}
				}

				@Override
				public void onUnchangedValid(ResourceShadowDiscriminator key, String desc) {
					LensProjectionContext projectionContext = context.findProjectionContext(key);
					if (projectionContext == null) {
						if (processOnlyExistingProjCxts) {
							return;
						}
						// The projection should exist before the change but it does not
						// This happens during reconciliation if there is an inconsistency.
						// Pretend that the assignment was just added. That should do.
						projectionContext = LensUtil.getOrCreateProjectionContext(context, key);
					}
					LOGGER.trace("Projection {} legal: unchanged (valid)", desc);
					projectionContext.setAssigned(true);
					projectionContext.setAssignedOld(true);
					if (projectionContext.getAssignmentPolicyEnforcementType() == AssignmentPolicyEnforcementType.NONE) {
						projectionContext.setLegalOld(null);
						projectionContext.setLegal(null);
					} else {
						projectionContext.setLegalOld(true);
						projectionContext.setLegal(true);
					}
				}

				@Override
				public void onUnchangedInvalid(ResourceShadowDiscriminator rat, String desc) {
					LensProjectionContext projectionContext = context.findProjectionContext(rat);
					if (projectionContext == null) {
						if (processOnlyExistingProjCxts) {
							return;
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
					if (projectionContext.getAssignmentPolicyEnforcementType() == AssignmentPolicyEnforcementType.NONE
							|| projectionContext.getAssignmentPolicyEnforcementType()
							== AssignmentPolicyEnforcementType.POSITIVE) {
						projectionContext.setLegalOld(null);
						projectionContext.setLegal(null);
					} else {
						projectionContext.setLegalOld(false);
						projectionContext.setLegal(false);
					}
				}

				@Override
				public void onUnassigned(ResourceShadowDiscriminator rat, String desc) {
					if (accountExists(context, rat)) {
						LensProjectionContext projectionContext = context.findProjectionContext(rat);
						if (projectionContext == null) {
							if (processOnlyExistingProjCxts) {
								return;
							}
							projectionContext = LensUtil.getOrCreateProjectionContext(context, rat);
						}
						projectionContext.setAssigned(false);
						projectionContext.setAssignedOld(true);
						projectionContext.setLegalOld(true);

						AssignmentPolicyEnforcementType assignmentPolicyEnforcement = projectionContext
								.getAssignmentPolicyEnforcementType();
						// TODO: check for MARK and LEGALIZE enforcement policies ....add delete laso for relative enforcemenet
						if (assignmentPolicyEnforcement == AssignmentPolicyEnforcementType.FULL
								|| assignmentPolicyEnforcement == AssignmentPolicyEnforcementType.RELATIVE) {
							LOGGER.trace("Projection {} illegal: unassigned", desc);
							projectionContext.setLegal(false);
						} else if (assignmentPolicyEnforcement == AssignmentPolicyEnforcementType.POSITIVE) {
							LOGGER.trace("Projection {} legal: unassigned, but allowed by policy ({})", desc,
									assignmentPolicyEnforcement);
							projectionContext.setLegal(true);
						} else {
							LOGGER.trace("Projection {} legal: unassigned, policy decision postponed ({})", desc,
									assignmentPolicyEnforcement);
							projectionContext.setLegal(null);
						}
					} else {

						LOGGER.trace("Projection {} nothing: unassigned (valid->invalid) but not there", desc);
						// We have to delete something that is not there. Nothing to do.
					}
				}

				@Override
				public void after(ResourceShadowDiscriminator rat, String desc,
						DeltaMapTriple<ResourceShadowDiscriminator, ConstructionPack<Construction<F>>> constructionMapTriple) {
					PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> projectionConstructionDeltaSetTriple =
							new PrismValueDeltaSetTriple<>(
									getConstructions(constructionMapTriple.getZeroMap().get(rat), true),
									getConstructions(constructionMapTriple.getPlusMap().get(rat), true),
									getConstructions(constructionMapTriple.getMinusMap().get(rat), false));
					LensProjectionContext projectionContext = context.findProjectionContext(rat);
					if (projectionContext != null) {
						// This can be null in a exotic case if we delete already deleted account
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Construction delta set triple for {}:\n{}", rat,
									projectionConstructionDeltaSetTriple.debugDump(1));
						}
						projectionContext.setConstructionDeltaSetTriple(projectionConstructionDeltaSetTriple);
						if (isForceRecon(constructionMapTriple.getZeroMap().get(rat)) || isForceRecon(
								constructionMapTriple.getPlusMap().get(rat)) || isForceRecon(
								constructionMapTriple.getMinusMap().get(rat))) {
							projectionContext.setDoReconciliation(true);
						}
					}
				}

			};

			constructionProcessor.processConstructions(context, evaluatedAssignmentTriple,
					evaluatedAssignment -> evaluatedAssignment.getConstructionTriple(),
					construction -> getConstructionMapKey(context, construction, task, result),
					consumer,
					task, result);

			removeIgnoredContexts(context);
			finishLegalDecisions(context);
		}
    }
    

	
	private <F extends FocusType> ResourceShadowDiscriminator getConstructionMapKey(LensContext<F> context, Construction<F> construction, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		String resourceOid = construction.getResource(task, result).getOid();
        String intent = construction.getIntent();
        ShadowKindType kind = construction.getKind();
        ResourceType resource = LensUtil.getResourceReadOnly(context, resourceOid, provisioningService, task, result);
        intent = LensUtil.refineProjectionIntent(kind, intent, resource, prismContext);
        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceOid, kind, intent);
        return rat;
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

        for (@SuppressWarnings("rawtypes") ItemDelta itemDelta : focusDelta.getModifications()) {
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
		ObjectDelta<F> delta = focusContext.getWaveDelta(focusContext.getLensContext().getExecutionWave());
		if (delta != null && !delta.isEmpty()) {
			return focusContext.getObjectNew().asObjectable();
		}

		if (focusContext.getObjectCurrent() != null) {
			return focusContext.getObjectCurrent().asObjectable();
		}

		return focusContext.getObjectNew().asObjectable();
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
				evaluatedAssignment.evaluateConstructions(focusOdo, context.getSystemConfiguration(), task, result);
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
            	String resourceOid = FocusTypeUtil.determineConstructionResource(evaluatedAssignment.getAssignmentType());
            	if (resourceOid == null) {
            		// This is a role assignment or something like that. Just throw the original exception for now.
            		throw ex;
            	}
            	ResourceShadowDiscriminator rad = new ResourceShadowDiscriminator(resourceOid, 
            			FocusTypeUtil.determineConstructionKind(evaluatedAssignment.getAssignmentType()), 
            			FocusTypeUtil.determineConstructionIntent(evaluatedAssignment.getAssignmentType()));
    			LensProjectionContext accCtx = context.findProjectionContext(rad);
    			if (accCtx != null) {
    				accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
    			}
    			iterator.remove();
            }
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

	@NotNull
	private Collection<PrismPropertyValue<Construction>> getConstructions(ConstructionPack accountConstructionPack, boolean validOnly) {
		if (accountConstructionPack == null) {
			return Collections.emptySet();
		}
		if (validOnly && !accountConstructionPack.hasValidAssignment()) {
			return Collections.emptySet();
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
		
			String desc = projectionContext.toHumanReadableString();
			
			if (projectionContext.isLegal() != null) {
				// already have decision
				LOGGER.trace("Projection {} legal={} (predetermined)", desc, projectionContext.isLegal());
				propagateLegalDecisionToHigherOrders(context, projectionContext);
				continue;
			}
		
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
						projectionContext.toHumanReadableString(), projectionContext.isThombstone(),
						projectionContext.getAssignmentPolicyEnforcementType(),
						projectionContext.isLegalize(), projectionContext.isLegalOld(), projectionContext.isLegal());
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

	private <F extends FocusType> void createAssignmentDelta(LensContext<F> context, LensProjectionContext accountContext) throws SchemaException{
        Class<F> focusClass = context.getFocusClass();
        ContainerDelta<AssignmentType> assignmentDelta = ContainerDelta.createDelta(FocusType.F_ASSIGNMENT, focusClass, prismContext);
		AssignmentType assignment = new AssignmentType();
		ConstructionType constructionType = new ConstructionType();
		constructionType.setResourceRef(ObjectTypeUtil.createObjectRef(accountContext.getResource()));
		assignment.setConstruction(constructionType);
		assignmentDelta.addValueToAdd(assignment.asPrismContainerValue());
		PrismContainerDefinition<AssignmentType> containerDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusClass).findContainerDefinition(FocusType.F_ASSIGNMENT);
		assignmentDelta.applyDefinition(containerDefinition);
		context.getFocusContext().swallowToProjectionWaveSecondaryDelta(assignmentDelta);
		
	}

	public <F extends ObjectType> void processOrgAssignments(LensContext<F> context, OperationResult result) throws SchemaException, PolicyViolationException {

		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			return;
		}

		Collection<PrismReferenceValue> shouldBeParentOrgRefs = new ArrayList<>();

		DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
		if (evaluatedAssignmentTriple == null) {
			return;		// could be if "assignments" step is skipped
		}
		for (EvaluatedAssignmentImpl<?> evalAssignment : evaluatedAssignmentTriple.getNonNegativeValues()) {
			if (evalAssignment.isValid()) {
				addReferences(shouldBeParentOrgRefs, evalAssignment.getOrgRefVals());
			}
		}
		setReferences(focusContext, ObjectType.F_PARENT_ORG_REF, shouldBeParentOrgRefs);
		
		ObjectDelta<F> focusPrimaryDelta = focusContext.getPrimaryDelta();
		if (focusPrimaryDelta != null) {
			ReferenceDelta parentOrgRefDelta = focusPrimaryDelta.findReferenceModification(ObjectType.F_PARENT_ORG_REF);
			if (parentOrgRefDelta != null) {
				List<PrismReferenceValue> parentOrgRefCurrentValues = null;
				PrismObject<F> objectCurrent = focusContext.getObjectCurrent();
				if (objectCurrent != null) {
					PrismReference parentOrgRefCurrent = objectCurrent.findReference(ObjectType.F_PARENT_ORG_REF);
					if (parentOrgRefCurrent != null) {
						parentOrgRefCurrentValues = parentOrgRefCurrent.getValues();
					}
				}
				try {
					
					parentOrgRefDelta.validateValues(
						(plusMinusZero,val) -> {
							switch (plusMinusZero) {
								case PLUS:
								case ZERO:
									if (!PrismReferenceValue.containsRealValue(shouldBeParentOrgRefs, val)) {
										throw new TunnelException(new PolicyViolationException("Attempt to add parentOrgRef "+val.getOid()+", but it is not allowed by assignments"));
									}
									break;
								case MINUS:
									if (PrismReferenceValue.containsRealValue(shouldBeParentOrgRefs, val)) {
										throw new TunnelException(new PolicyViolationException("Attempt to delete parentOrgRef "+val.getOid()+", but it is mandated by assignments"));
									}
									break;
							}
						}, parentOrgRefCurrentValues);
					
				} catch (TunnelException e) {
					throw (PolicyViolationException)e.getCause();
				}
			}
		}
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

    private <V extends PrismValue, D extends ItemDefinition, F extends FocusType> XMLGregorianCalendar collectFocusTripleFromMappings(
    		Collection<EvaluatedAssignmentImpl<F>> evaluatedAssignments,
    		Map<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?,?>>> outputTripleMap,
    		PlusMinusZero plusMinusZero) throws SchemaException {
		
		XMLGregorianCalendar nextRecomputeTime = null;
		
		for (EvaluatedAssignmentImpl<F> ea: evaluatedAssignments) {
			Collection<Mapping<V,D>> focusMappings = (Collection)ea.getFocusMappings();
			for (Mapping<V,D> mapping: focusMappings) {
				
				ItemPath itemPath = mapping.getOutputPath();
				DeltaSetTriple<ItemValueWithOrigin<V,D>> outputTriple = ItemValueWithOrigin.createOutputTriple(mapping);
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
				DeltaSetTriple<ItemValueWithOrigin<V,D>> mapTriple = (DeltaSetTriple<ItemValueWithOrigin<V,D>>) outputTripleMap.get(itemPath);
				if (mapTriple == null) {
					outputTripleMap.put(itemPath, outputTriple);
				} else {
					mapTriple.merge(outputTriple);
				}
			}
		}
		
		return nextRecomputeTime;
	}
    
    public <F extends ObjectType> void processMembershipAndDelegatedRefs(LensContext<F> context,
			OperationResult result) throws SchemaException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null || !FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
			return;
		}
		Collection<PrismReferenceValue> shouldBeRoleRefs = new ArrayList<>();
		Collection<PrismReferenceValue> shouldBeDelegatedRefs = new ArrayList<>();

		DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
		if (evaluatedAssignmentTriple == null) {
			return;	// could be if the "assignments" step is skipped
		}
		for (EvaluatedAssignmentImpl<?> evalAssignment : evaluatedAssignmentTriple.getNonNegativeValues()) {
			if (evalAssignment.isValid()) {
				addReferences(shouldBeRoleRefs, evalAssignment.getMembershipRefVals());
				addReferences(shouldBeDelegatedRefs, evalAssignment.getDelegationRefVals());
			}
		}

		setReferences(focusContext, FocusType.F_ROLE_MEMBERSHIP_REF, shouldBeRoleRefs);
		setReferences(focusContext, FocusType.F_DELEGATED_REF, shouldBeDelegatedRefs);
    }

	private <F extends ObjectType> void setReferences(LensFocusContext<F> focusContext, QName itemName,
			Collection<PrismReferenceValue> targetState) throws SchemaException {

		PrismObject<F> focusOld = focusContext.getObjectOld();
		if (focusOld == null) {
			if (targetState.isEmpty()) {
				return;
			}
		} else {
			PrismReference existingState = focusOld.findReference(itemName);
			if (existingState == null || existingState.isEmpty()) {
				if (targetState.isEmpty()) {
					return;
				}
			} else {
				// we don't use QNameUtil.match here, because we want to ensure we store qualified values there
				// (and newValues are all qualified)
				Comparator<PrismReferenceValue> comparator =
						(a, b) -> 2*a.getOid().compareTo(b.getOid())
								+ (Objects.equals(a.getRelation(), b.getRelation()) ? 0 : 1);
				if (MiscUtil.unorderedCollectionCompare(targetState, existingState.getValues(), comparator)) {
					return;
				}
			}
		}

		PrismReferenceDefinition itemDef = focusContext.getObjectDefinition().findItemDefinition(itemName, PrismReferenceDefinition.class);
		ReferenceDelta itemDelta = new ReferenceDelta(itemName, itemDef, focusContext.getObjectDefinition().getPrismContext());
		itemDelta.setValuesToReplace(targetState);
		focusContext.swallowToSecondaryDelta(itemDelta);
	}

	private void addReferences(Collection<PrismReferenceValue> extractedReferences, Collection<PrismReferenceValue> references) {
		for (PrismReferenceValue reference: references) {
			boolean found = false;
			for (PrismReferenceValue exVal: extractedReferences) {
				if (MiscUtil.equals(exVal.getOid(), reference.getOid())
						&& ObjectTypeUtil.relationsEquivalent(exVal.getRelation(), reference.getRelation())) {
					found = true;
					break;
				}
			}
			if (!found) {
				PrismReferenceValue ref = reference.clone(false);		// using copyObject=false instead of calling canonicalize()
				if (ref.getRelation() != null && QNameUtil.isUnqualified(ref.getRelation())) {
					ref.setRelation(new QName(SchemaConstants.NS_ORG, ref.getRelation().getLocalPart(), SchemaConstants.PREFIX_NS_ORG));
				}
				extractedReferences.add(ref);
			}
		}
	}
	
	private <F extends FocusType> AssignmentEvaluator<F> createAssignmentEvaluator(LensContext<F> context,
			XMLGregorianCalendar now) throws SchemaException {
		return new AssignmentEvaluator.Builder<F>()
				.repository(repositoryService)
				.focusOdo(context.getFocusContext().getObjectDeltaObject())
				.lensContext(context)
				.channel(context.getChannel())
				.objectResolver(objectResolver)
				.systemObjectCache(systemObjectCache)
				.prismContext(prismContext)
				.mappingFactory(mappingFactory)
				.mappingEvaluator(mappingEvaluator)
				.activationComputer(activationComputer)
				.now(now)
				.systemConfiguration(context.getSystemConfiguration())
				.build();
	}
	
}
