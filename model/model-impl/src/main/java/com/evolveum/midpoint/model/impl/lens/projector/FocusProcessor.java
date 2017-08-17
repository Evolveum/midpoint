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
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.util.exception.NoFocusNameSchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.OperationalDataManager;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.CredentialsProcessor;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.OidUtil;
import com.evolveum.midpoint.task.api.Task;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCredentialType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GlobalPolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterationSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModificationPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingEvaluationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Processor to handle everything about focus: values, assignments, etc.
 * 
 * @author Radovan Semancik
 * 
 */
@Component
public class FocusProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(FocusProcessor.class);

	private PrismContainerDefinition<ActivationType> activationDefinition;
	private PrismPropertyDefinition<Integer> failedLoginsDefinition;
	
	@Autowired
    private InboundProcessor inboundProcessor;
	
	@Autowired
    private AssignmentProcessor assignmentProcessor;
	
	@Autowired
	private ObjectTemplateProcessor objectTemplateProcessor;
	
	@Autowired
	private MappingFactory mappingFactory;

	@Autowired
	private PrismContext prismContext;

	@Autowired
	private CredentialsProcessor credentialsProcessor;
	
	@Autowired
	private ModelObjectResolver modelObjectResolver;
	
	@Autowired
	private ActivationComputer activationComputer;
	
	@Autowired
	private ExpressionFactory expressionFactory;

	@Autowired
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;
	
	@Autowired
    private MappingEvaluator mappingHelper;
	
	@Autowired
    private OperationalDataManager metadataManager;

	@Autowired
	private PolicyRuleProcessor policyRuleProcessor;

	<O extends ObjectType, F extends FocusType> void processFocus(LensContext<O> context, String activityDescription,
			XMLGregorianCalendar now, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, PolicyViolationException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {

		LensFocusContext<O> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
    	
    	if (!FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
    		// We can do this only for FocusType objects.
    		return;
    	}
    	
    	processFocusFocus((LensContext<F>)context, activityDescription, now, task, result);
	}
	
	private <F extends FocusType> void processFocusFocus(LensContext<F> context, String activityDescription,
			XMLGregorianCalendar now, Task task, OperationResult result)
					throws ObjectNotFoundException,
		            SchemaException, ExpressionEvaluationException, PolicyViolationException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		ObjectTemplateType objectTemplate = context.getFocusTemplate();
		PartialProcessingOptionsType partialProcessingOptions = context.getPartialProcessingOptions();

		boolean resetOnRename = true; // This is fixed now. TODO: make it configurable
		int maxIterations = 0;
		IterationSpecificationType iterationSpecificationType = null;
		if (objectTemplate != null) {
			iterationSpecificationType = objectTemplate.getIteration();
			maxIterations = LensUtil.determineMaxIterations(iterationSpecificationType);
		}
		int iteration = focusContext.getIteration();
		String iterationToken = focusContext.getIterationToken();
		boolean wasResetIterationCounter = false;
		
		PrismObject<F> focusCurrent = focusContext.getObjectCurrent();
		if (focusCurrent != null && iterationToken == null) {
			Integer focusIteration = focusCurrent.asObjectable().getIteration();
			if (focusIteration != null) {
				iteration = focusIteration;
			}
			iterationToken = focusCurrent.asObjectable().getIterationToken();
		}
		
		while (true) {
			
			ObjectPolicyConfigurationType objectPolicyConfigurationType = focusContext.getObjectPolicyConfigurationType();
			applyObjectPolicyConstraints(focusContext, objectPolicyConfigurationType);
		
			ExpressionVariables variablesPreIteration = Utils.getDefaultExpressionVariables(focusContext.getObjectNew(), 
					null, null, null, context.getSystemConfiguration(), focusContext);
			if (iterationToken == null) {
				iterationToken = LensUtil.formatIterationToken(context, focusContext, 
						iterationSpecificationType, iteration, expressionFactory, variablesPreIteration, task, result);
			}
			
			// We have to remember the token and iteration in the context.
			// The context can be recomputed several times. But we always want
			// to use the same iterationToken if possible. If there is a random
			// part in the iterationToken expression that we need to avoid recomputing
			// the token otherwise the value can change all the time (even for the same inputs).
			// Storing the token in the secondary delta is not enough because secondary deltas can be dropped
			// if the context is re-projected.
			focusContext.setIteration(iteration);
			focusContext.setIterationToken(iterationToken);
			LOGGER.trace("Focus {} processing, iteration {}, token '{}'", focusContext.getHumanReadableName(), iteration, iterationToken);
			
			String conflictMessage;
			if (!LensUtil.evaluateIterationCondition(context, focusContext, 
					iterationSpecificationType, iteration, iterationToken, true, expressionFactory, variablesPreIteration, task, result)) {
				
				conflictMessage = "pre-iteration condition was false";
				LOGGER.debug("Skipping iteration {}, token '{}' for {} because the pre-iteration condition was false",
						iteration, iterationToken, focusContext.getHumanReadableName());
			} else {
				
				// INBOUND
				
				if (consistencyChecks) context.checkConsistence();

				LensUtil.partialExecute("inbound",
						() -> {
							// Loop through the account changes, apply inbound expressions
					        inboundProcessor.processInbound(context, now, task, result);
					        if (consistencyChecks) context.checkConsistence();
					        context.recomputeFocus();
					        LensUtil.traceContext(LOGGER, activityDescription, "inbound", false, context, false);
					        if (consistencyChecks) context.checkConsistence();
						},
						partialProcessingOptions::getInbound);
				
				
		        // ACTIVATION
		        
				LensUtil.partialExecute("focusActivation",
						() -> processActivationBeforeAssignments(context, now, result),
						partialProcessingOptions::getFocusActivation);
				
				
		        // OBJECT TEMPLATE (before assignments)
		        
				LensUtil.partialExecute("objectTemplateBeforeAssignments",
						() -> objectTemplateProcessor.processTemplate(context,
								ObjectTemplateMappingEvaluationPhaseType.BEFORE_ASSIGNMENTS, now, task, result),
						partialProcessingOptions::getObjectTemplateBeforeAssignments);
		        
		        
		        // process activation again. Object template might have changed it.
		        context.recomputeFocus();
		        LensUtil.partialExecute("focusActivation",
						() -> processActivationBeforeAssignments(context, now, result),
						partialProcessingOptions::getFocusActivation);
		        
		        // ASSIGNMENTS
		        
		        LensUtil.partialExecute("assignments",
						() -> assignmentProcessor.processAssignmentsProjections(context, now, task, result),
						partialProcessingOptions::getAssignments);

		        LensUtil.partialExecute("assignmentsOrg",
						() -> assignmentProcessor.processOrgAssignments(context, result),
						partialProcessingOptions::getAssignmentsOrg);


		        LensUtil.partialExecute("assignmentsMembershipAndDelegate",
						() -> assignmentProcessor.processMembershipAndDelegatedRefs(context, result),
						partialProcessingOptions::getAssignmentsMembershipAndDelegate);

		        context.recompute();
		        
		        LensUtil.partialExecute("assignmentsConflicts",
						() -> assignmentProcessor.checkForAssignmentConflicts(context, result),
						partialProcessingOptions::getAssignmentsConflicts);
		        
		        // OBJECT TEMPLATE (after assignments)

				LensUtil.partialExecute("objectTemplateAfterAssignments",
						() -> objectTemplateProcessor.processTemplate(context,
								ObjectTemplateMappingEvaluationPhaseType.AFTER_ASSIGNMENTS, now, task, result),
						partialProcessingOptions::getObjectTemplateBeforeAssignments);

		        context.recompute();

		        // process activation again. Second pass through object template might have changed it.
		        // We also need to apply assignment activation if needed
		        context.recomputeFocus();
		        LensUtil.partialExecute("focusActivation",
						() -> processActivationAfterAssignments(context, now, result),
						partialProcessingOptions::getFocusActivation);
		        
		        // CREDENTIALS (including PASSWORD POLICY)
				
		        LensUtil.partialExecute("focusCredentials",
						() -> credentialsProcessor.processFocusCredentials(context, now, task, result),
						partialProcessingOptions::getFocusCredentials);
		        
		        // We need to evaluate this as a last step. We need to make sure we have all the
		        // focus deltas so we can properly trigger the rules.

		        LensUtil.partialExecute("focusPolicyRules",
						() -> evaluateFocusPolicyRules(context, activityDescription, now, task, result),
						partialProcessingOptions::getFocusPolicyRules);
		        
		        // Processing done, check for success

				if (resetOnRename && !wasResetIterationCounter && willResetIterationCounter(focusContext)) {
					// Make sure this happens only the very first time during the first recompute.
					// Otherwise it will always change the token (especially if the token expression has a random part)
					// hence the focusContext.getIterationToken() == null
		        	wasResetIterationCounter = true;
		        	if (iteration != 0) {
			        	iteration = 0;
			    		iterationToken = null;
			    		LOGGER.trace("Resetting iteration counter and token because rename was detected");
			    		cleanupContext(focusContext);
			    		continue;
		        	}
		        }
				
				PrismObject<F> previewObjectNew = focusContext.getObjectNew();
				if (previewObjectNew == null) {
					// this must be delete
				} else {
			        // Explicitly check for name. The checker would check for this also. But checking it here
					// will produce better error message
					PolyStringType objectName = previewObjectNew.asObjectable().getName();
					if (objectName == null || objectName.getOrig().isEmpty()) {
						throw new NoFocusNameSchemaException("No name in new object "+objectName+" as produced by template "+objectTemplate+
								" in iteration "+iteration+", we cannot process an object without a name");
					}
				}
				
				// Check if iteration constraints are OK
				FocusConstraintsChecker<F> checker = new FocusConstraintsChecker<>();
				checker.setPrismContext(prismContext);
		        checker.setContext(context);
		        checker.setRepositoryService(cacheRepositoryService);
		        checker.check(previewObjectNew, result);
		        if (checker.isSatisfiesConstraints()) {
		        	LOGGER.trace("Current focus satisfies uniqueness constraints. Iteration {}, token '{}'", iteration, iterationToken);
		        	ExpressionVariables variablesPostIteration = Utils.getDefaultExpressionVariables(focusContext.getObjectNew(), 
		        			null, null, null, context.getSystemConfiguration(), focusContext);		        	
		        	if (LensUtil.evaluateIterationCondition(context, focusContext, 
		        			iterationSpecificationType, iteration, iterationToken, false, expressionFactory, variablesPostIteration, 
		        			task, result)) {
	    				// stop the iterations
	    				break;
	    			} else {
	    				conflictMessage = "post-iteration condition was false";
	    				LOGGER.debug("Skipping iteration {}, token '{}' for {} because the post-iteration condition was false",
								iteration, iterationToken, focusContext.getHumanReadableName());
	    			}
		        } else {
			        LOGGER.trace("Current focus does not satisfy constraints. Conflicting object: {}; iteration={}, maxIterations={}",
							checker.getConflictingObject(), iteration, maxIterations);
			        conflictMessage = checker.getMessages();
		        }
		        
				if (!wasResetIterationCounter) {
		        	wasResetIterationCounter = true;
			        if (iteration != 0) {
			        	iterationToken = null;
			        	iteration = 0;
			    		LOGGER.trace("Resetting iteration counter and token after conflict");
			    		cleanupContext(focusContext);
			    		continue;
			        }
		        }
			}
				        
	        // Next iteration
			iteration++;
	        iterationToken = null;
			LensUtil.checkMaxIterations(iteration, maxIterations, conflictMessage, focusContext.getHumanReadableName());
			cleanupContext(focusContext);
		}
		
		addIterationTokenDeltas(focusContext, iteration, iterationToken);
		if (consistencyChecks) context.checkConsistence();
		
	}

	private <F extends FocusType> void evaluateFocusPolicyRules(LensContext<F> context, String activityDescription,
			XMLGregorianCalendar now, Task task, OperationResult result) throws PolicyViolationException, SchemaException {
		triggerAssignmentFocusPolicyRules(context, activityDescription, now, task, result);
		triggerGlobalRules(context);
	}

	// TODO: should we really do this? Focus policy rules (e.g. forbidden modifications) are irrelevant in this situation,
	// TODO: i.e. if we are assigning the object into some other object [med]
	private <F extends FocusType> void triggerAssignmentFocusPolicyRules(LensContext<F> context, String activityDescription,
			XMLGregorianCalendar now, Task task, OperationResult result) throws PolicyViolationException, SchemaException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
		if (evaluatedAssignmentTriple == null) {
			return;
		}
		for (EvaluatedAssignmentImpl<?> evaluatedAssignment: evaluatedAssignmentTriple.getNonNegativeValues()) {
			Collection<EvaluatedPolicyRule> policyRules = evaluatedAssignment.getFocusPolicyRules();
			for (EvaluatedPolicyRule policyRule: policyRules) {
				triggerRule(focusContext, policyRule);
			}
		}
	}
	
	private <F extends FocusType> void triggerGlobalRules(LensContext<F> context) throws SchemaException, PolicyViolationException {
		PrismObject<SystemConfigurationType> systemConfiguration = context.getSystemConfiguration();
		if (systemConfiguration == null) {
			return;
		}
		LensFocusContext<F> focusContext = context.getFocusContext();
		
		// We need to consider object before modification here. We need to prohibit the modification, so we
		// cannot look at modified object.
		PrismObject<F> focus = focusContext.getObjectCurrent();
		if (focus == null) {
			focus = focusContext.getObjectNew();
		}
		
		for (GlobalPolicyRuleType globalPolicyRule: systemConfiguration.asObjectable().getGlobalPolicyRule()) {
			ObjectSelectorType focusSelector = globalPolicyRule.getFocusSelector();
			if (cacheRepositoryService.selectorMatches(focusSelector, focus, LOGGER, "Global policy rule "+globalPolicyRule.getName()+": ")) {
				EvaluatedPolicyRule evaluatedRule = new EvaluatedPolicyRuleImpl(globalPolicyRule, null);
				triggerRule(focusContext, evaluatedRule);
			}
		}
	}

	private <F extends FocusType> void triggerRule(LensFocusContext<F> focusContext, EvaluatedPolicyRule policyRule)
			throws PolicyViolationException, SchemaException {
		PolicyConstraintsType policyConstraints = policyRule.getPolicyConstraints();
		if (policyConstraints == null) {
			return;
		}
		for (ModificationPolicyConstraintType modificationConstraintType: policyConstraints.getModification()) {
			focusContext.addPolicyRule(policyRule);
			if (modificationConstraintMatches(focusContext, policyRule, modificationConstraintType)) {
				EvaluatedPolicyRuleTrigger<?> trigger = new EvaluatedPolicyRuleTrigger<>(PolicyConstraintKindType.MODIFICATION,
						modificationConstraintType, "Focus "+focusContext.getHumanReadableName()+" was modified");
				focusContext.triggerConstraint(policyRule, trigger);
			}
		}
	}

	private <F extends FocusType> boolean modificationConstraintMatches(LensFocusContext<F> focusContext, EvaluatedPolicyRule policyRule, 
			ModificationPolicyConstraintType modificationConstraintType) throws SchemaException {
		if (!operationMatches(focusContext, modificationConstraintType.getOperation())) {
			LOGGER.trace("Rule {} operation not applicable", policyRule.getName());
			return false;
		}
		if (modificationConstraintType.getItem().isEmpty()) {
			return focusContext.hasAnyDelta();
		}
		ObjectDelta<?> summaryDelta = ObjectDelta.union(focusContext.getPrimaryDelta(), focusContext.getSecondaryDelta());
		if (summaryDelta == null) {
			return false;
		}
		for (ItemPathType path : modificationConstraintType.getItem()) {
			if (!pathMatches(focusContext.getObjectOld(), summaryDelta, path.getItemPath())) {
				return false;
			}
		}
		return true;
	}

	private <F extends FocusType> boolean pathMatches(PrismObject<F> objectOld, ObjectDelta<?> delta, ItemPath path)
			throws SchemaException {
		if (delta.isAdd()) {
			return delta.getObjectToAdd().containsItem(path, false);
		} else if (delta.isDelete()) {
			return objectOld != null && objectOld.containsItem(path, false);
		} else {
			return delta.findItemDelta(path) != null;
		}
	}

	private <F extends FocusType> boolean operationMatches(LensFocusContext<F> focusContext, List<ChangeTypeType> operations) {
		if (operations.isEmpty()) {
			return true;
		}
		for (ChangeTypeType operation: operations) {
			if (focusContext.operationMatches(operation)) {
				return true;
			}
		}
		return false;
	}

	private <F extends FocusType> void applyObjectPolicyConstraints(LensFocusContext<F> focusContext, ObjectPolicyConfigurationType objectPolicyConfigurationType) throws SchemaException {
		if (objectPolicyConfigurationType == null) {
			return;
		}
		
		final PrismObject<F> focusNew = focusContext.getObjectNew();
		if (focusNew == null) {
			// This is delete. Nothing to do.
			return;
		}
		
		for (PropertyConstraintType propertyConstraintType: objectPolicyConfigurationType.getPropertyConstraint()) {
			ItemPath itemPath = propertyConstraintType.getPath().getItemPath();
			if (BooleanUtils.isTrue(propertyConstraintType.isOidBound())) {
				PrismProperty<Object> prop = focusNew.findProperty(itemPath);
				if (prop == null || prop.isEmpty()) {
					String newValue = focusNew.getOid();
					if (newValue == null) {
						newValue = OidUtil.generateOid();
					}
					LOGGER.trace("Generating new OID-bound value for {}: {}", itemPath, newValue);
					PrismObjectDefinition<F> focusDefinition = focusContext.getObjectDefinition();
					PrismPropertyDefinition<Object> propDef = focusDefinition.findPropertyDefinition(itemPath);
					if (propDef == null) {
						throw new SchemaException("No definition for property "+itemPath+" in "+focusDefinition+" as specified in object policy");
					}
					PropertyDelta<Object> propDelta = propDef.createEmptyDelta(itemPath);
					if (String.class.isAssignableFrom(propDef.getTypeClass())) {
						propDelta.setValueToReplace(new PrismPropertyValue<Object>(newValue, OriginType.USER_POLICY, null));
					} else if (PolyString.class.isAssignableFrom(propDef.getTypeClass())) {
						propDelta.setValueToReplace(new PrismPropertyValue<Object>(new PolyString(newValue), OriginType.USER_POLICY, null));
					} else {
						throw new SchemaException("Unsupported type "+propDef.getTypeName()+" for property "+itemPath+" in "+focusDefinition+" as specified in object policy, only string and polystring properties are supported for OID-bound mode");
					}					
					focusContext.swallowToSecondaryDelta(propDelta);
					focusContext.recompute();
				}
			}
		}
		
		// Deprecated
		if (BooleanUtils.isTrue(objectPolicyConfigurationType.isOidNameBoundMode())) {
			// Generate the name now - unless it is already present
			PolyStringType focusNewName = focusNew.asObjectable().getName();
			if (focusNewName == null) {
				String newValue = focusNew.getOid();
				if (newValue == null) {
					newValue = OidUtil.generateOid();
				}
				LOGGER.trace("Generating new name (bound to OID): {}", newValue);
				PrismObjectDefinition<F> focusDefinition = focusContext.getObjectDefinition();
				PrismPropertyDefinition<PolyString> focusNameDef = focusDefinition.findPropertyDefinition(FocusType.F_NAME);
				PropertyDelta<PolyString> nameDelta = focusNameDef.createEmptyDelta(new ItemPath(FocusType.F_NAME));
				nameDelta.setValueToReplace(new PrismPropertyValue<PolyString>(new PolyString(newValue), OriginType.USER_POLICY, null));
				focusContext.swallowToSecondaryDelta(nameDelta);
				focusContext.recompute();
			}
		}
	}

	private <F extends FocusType> boolean willResetIterationCounter(LensFocusContext<F> focusContext) throws SchemaException {
		ObjectDelta<F> focusDelta = focusContext.getDelta();
		if (focusDelta == null) {
			return false;
		}
		if (focusContext.isAdd() || focusContext.isDelete()) {
			return false;
		}
		if (focusDelta.findPropertyDelta(FocusType.F_ITERATION) != null) {
			// there was a reset already in previous projector runs
			return false;
		}
		// Check for rename
		PropertyDelta<Object> nameDelta = focusDelta.findPropertyDelta(new ItemPath(FocusType.F_NAME));
		return nameDelta != null;
	}
	
	/**
	 * Remove the intermediate results of values processing such as secondary deltas.
	 */
	private <F extends FocusType> void cleanupContext(LensFocusContext<F> focusContext) throws SchemaException {
		// We must NOT clean up activation computation. This has happened before, it will not happen again
		// and it does not depend on iteration
		LOGGER.trace("Cleaning up focus context");
		focusContext.setProjectionWaveSecondaryDelta(null);
		
		focusContext.clearIntermediateResults();
		focusContext.recompute();
	}

	private <F extends FocusType> void processActivationBeforeAssignments(LensContext<F> context, XMLGregorianCalendar now, 
			OperationResult result) 
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
		processActivationBasic(context, now, result);
	}
	
	private <F extends FocusType> void processActivationAfterAssignments(LensContext<F> context, XMLGregorianCalendar now, 
			OperationResult result) 
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
		processActivationBasic(context, now, result);
		processAssignmentActivation(context, now, result);
	}
	
	private <F extends FocusType> void processActivationBasic(LensContext<F> context, XMLGregorianCalendar now, 
			OperationResult result) 
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		
		if (focusContext.isDelete()) {
			LOGGER.trace("Skipping processing of focus activation: focus delete");
			return;
		}
		
		processActivationAdministrativeAndValidity(focusContext, now, result);
		
		if (focusContext.canRepresent(UserType.class)) {
			processActivationLockout((LensFocusContext<UserType>) focusContext, now, result);
		}
	}
		
	private <F extends FocusType> void processActivationAdministrativeAndValidity(LensFocusContext<F> focusContext, XMLGregorianCalendar now, 
			OperationResult result) 
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {	
		
		TimeIntervalStatusType validityStatusNew = null;
		TimeIntervalStatusType validityStatusCurrent = null;
		XMLGregorianCalendar validityChangeTimestamp = null;
		
		String lifecycleStateNew = null;
		String lifecycleStateCurrent = null;
		ActivationType activationNew = null;
		ActivationType activationCurrent = null;
		
		PrismObject<F> focusNew = focusContext.getObjectNew();
		if (focusNew != null) {
			F focusTypeNew = focusNew.asObjectable();
			activationNew = focusTypeNew.getActivation();
			if (activationNew != null) {
				validityStatusNew = activationComputer.getValidityStatus(activationNew, now);
				validityChangeTimestamp = activationNew.getValidityChangeTimestamp();
			}
			lifecycleStateNew = focusTypeNew.getLifecycleState();
		}
		
		PrismObject<F> focusCurrent = focusContext.getObjectCurrent();
		if (focusCurrent != null) {
			F focusCurrentType = focusCurrent.asObjectable();
			activationCurrent = focusCurrentType.getActivation();
			if (activationCurrent != null) {
				validityStatusCurrent = activationComputer.getValidityStatus(activationCurrent, validityChangeTimestamp);
			}
			lifecycleStateCurrent = focusCurrentType.getLifecycleState();
		}
		
		if (validityStatusCurrent == validityStatusNew) {
			// No change, (almost) no work
			if (validityStatusNew != null && activationNew.getValidityStatus() == null) {
				// There was no validity change. But the status is not recorded. So let's record it so it can be used in searches. 
				recordValidityDelta(focusContext, validityStatusNew, now);
			} else {
				LOGGER.trace("Skipping validity processing because there was no change ({} -> {})", validityStatusCurrent, validityStatusNew);
			}
		} else {
			LOGGER.trace("Validity change {} -> {}", validityStatusCurrent, validityStatusNew);
			recordValidityDelta(focusContext, validityStatusNew, now);
		}
		
		ActivationStatusType effectiveStatusNew = activationComputer.getEffectiveStatus(lifecycleStateNew, activationNew, validityStatusNew);
		ActivationStatusType effectiveStatusCurrent = activationComputer.getEffectiveStatus(lifecycleStateCurrent, activationCurrent, validityStatusCurrent);
		
		if (effectiveStatusCurrent == effectiveStatusNew) {
			// No change, (almost) no work
			if (effectiveStatusNew != null && (activationNew == null || activationNew.getEffectiveStatus() == null)) {
				// There was no effective status change. But the status is not recorded. So let's record it so it can be used in searches. 
				recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
			} else {
				if (focusContext.getPrimaryDelta() != null && focusContext.getPrimaryDelta().hasItemDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS)) {
					LOGGER.trace("Forcing effective status delta even though there was no change ({} -> {}) because there is explicit administrativeStatus delta", effectiveStatusCurrent, effectiveStatusNew);
					// We need this to force the change down to the projections later in the activation processor
					// some of the mappings will use effectiveStatus as a source, therefore there has to be a delta for the mapping to work correctly
					recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
				} else {
					//check computed effective status current with the saved one - e.g. there can be some inconsistencies so we need to check and force the change.. in other cases, effectvie status will be stored with 
					// incorrect value. Maybe another option is to not compute effectiveStatusCurrent if there is an existing (saved) effective status in the user.. TODO
					if (activationCurrent != null && activationCurrent.getEffectiveStatus() != null) {
						ActivationStatusType effectiveStatusSaved = activationCurrent.getEffectiveStatus();
						if (effectiveStatusSaved != effectiveStatusNew) {
							recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
						}
					}
					LOGGER.trace("Skipping effective status processing because there was no change ({} -> {})", effectiveStatusCurrent, effectiveStatusNew);
				}
			}
		} else {
			LOGGER.trace("Effective status change {} -> {}", effectiveStatusCurrent, effectiveStatusNew);
			recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
		}
		
		
	}
	
	private <F extends FocusType> void processActivationLockout(LensFocusContext<UserType> focusContext, XMLGregorianCalendar now, 
			OperationResult result) 
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {	
		
		ObjectDelta<UserType> focusPrimaryDelta = focusContext.getPrimaryDelta();
		if (focusPrimaryDelta != null) {
			PropertyDelta<LockoutStatusType> lockoutStatusDelta = focusContext.getPrimaryDelta().findPropertyDelta(SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS);
			if (lockoutStatusDelta != null) {
				if (lockoutStatusDelta.isAdd()) {
					for (PrismPropertyValue<LockoutStatusType> pval: lockoutStatusDelta.getValuesToAdd()) {
						if (pval.getValue() == LockoutStatusType.LOCKED) {
							throw new SchemaException("Lockout status cannot be changed to LOCKED value");
						}
					}
				} else if (lockoutStatusDelta.isReplace()) {
					for (PrismPropertyValue<LockoutStatusType> pval: lockoutStatusDelta.getValuesToReplace()) {
						if (pval.getValue() == LockoutStatusType.LOCKED) {
							throw new SchemaException("Lockout status cannot be changed to LOCKED value");
						}
					}
				}
			}
		}
		
		ActivationType activationNew = null;
		ActivationType activationCurrent = null;
		
		LockoutStatusType lockoutStatusNew = null;
		LockoutStatusType lockoutStatusCurrent = null;
		
		PrismObject<UserType> focusNew = focusContext.getObjectNew();
		if (focusNew != null) {
			activationNew = focusNew.asObjectable().getActivation();
			if (activationNew != null) {
				lockoutStatusNew = activationNew.getLockoutStatus();
			}
		}
		
		PrismObject<UserType> focusCurrent = focusContext.getObjectCurrent();
		if (focusCurrent != null) {
			activationCurrent = focusCurrent.asObjectable().getActivation();
			if (activationCurrent != null) {
				lockoutStatusCurrent = activationCurrent.getLockoutStatus();
			}
		}
		
		if (lockoutStatusNew == lockoutStatusCurrent) {
			// No change, (almost) no work
			LOGGER.trace("Skipping lockout processing because there was no change ({} -> {})", lockoutStatusCurrent, lockoutStatusNew);
			return;
		}
		
		LOGGER.trace("Lockout change {} -> {}", lockoutStatusCurrent, lockoutStatusNew);
		
		if (lockoutStatusNew == LockoutStatusType.NORMAL) {
			
			CredentialsType credentialsTypeNew = focusNew.asObjectable().getCredentials();
			if (credentialsTypeNew != null) {
				resetFailedLogins(focusContext, credentialsTypeNew.getPassword(), SchemaConstants.PATH_CREDENTIALS_PASSWORD_FAILED_LOGINS);
				resetFailedLogins(focusContext, credentialsTypeNew.getNonce(), SchemaConstants.PATH_CREDENTIALS_NONCE_FAILED_LOGINS);
				resetFailedLogins(focusContext, credentialsTypeNew.getSecurityQuestions(), SchemaConstants.PATH_CREDENTIALS_SECURITY_QUESTIONS_FAILED_LOGINS);
			}
			
			if (activationNew != null && activationNew.getLockoutExpirationTimestamp() != null) {
				PrismContainerDefinition<ActivationType> activationDefinition = getActivationDefinition();
				PrismPropertyDefinition<XMLGregorianCalendar> lockoutExpirationTimestampDef = activationDefinition.findPropertyDefinition(ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP);
				PropertyDelta<XMLGregorianCalendar> lockoutExpirationTimestampDelta 
						= lockoutExpirationTimestampDef.createEmptyDelta(new ItemPath(UserType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP));
				lockoutExpirationTimestampDelta.setValueToReplace();
				focusContext.swallowToProjectionWaveSecondaryDelta(lockoutExpirationTimestampDelta);
			}
		}
		
	}
	
	private void resetFailedLogins(LensFocusContext<UserType> focusContext, AbstractCredentialType credentialTypeNew, ItemPath path) throws SchemaException{
		if (credentialTypeNew != null) {
			Integer failedLogins = credentialTypeNew.getFailedLogins();
			if (failedLogins != null && failedLogins != 0) {
				PrismPropertyDefinition<Integer> failedLoginsDef = getFailedLoginsDefinition();
				PropertyDelta<Integer> failedLoginsDelta = failedLoginsDef.createEmptyDelta(path);
				failedLoginsDelta.setValueToReplace(new PrismPropertyValue<Integer>(0, OriginType.USER_POLICY, null));
				focusContext.swallowToProjectionWaveSecondaryDelta(failedLoginsDelta);
			}
		}
	}
	
	private <F extends ObjectType> void recordValidityDelta(LensFocusContext<F> focusContext, TimeIntervalStatusType validityStatusNew,
			XMLGregorianCalendar now) throws SchemaException {
		PrismContainerDefinition<ActivationType> activationDefinition = getActivationDefinition();
		
		PrismPropertyDefinition<TimeIntervalStatusType> validityStatusDef = activationDefinition.findPropertyDefinition(ActivationType.F_VALIDITY_STATUS);
		PropertyDelta<TimeIntervalStatusType> validityStatusDelta 
				= validityStatusDef.createEmptyDelta(new ItemPath(UserType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS));
		if (validityStatusNew == null) {
			validityStatusDelta.setValueToReplace();
		} else {
			validityStatusDelta.setValueToReplace(new PrismPropertyValue<>(validityStatusNew, OriginType.USER_POLICY, null));
		}
		focusContext.swallowToProjectionWaveSecondaryDelta(validityStatusDelta);
		
		PrismPropertyDefinition<XMLGregorianCalendar> validityChangeTimestampDef = activationDefinition.findPropertyDefinition(ActivationType.F_VALIDITY_CHANGE_TIMESTAMP);
		PropertyDelta<XMLGregorianCalendar> validityChangeTimestampDelta 
				= validityChangeTimestampDef.createEmptyDelta(new ItemPath(UserType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP));
		validityChangeTimestampDelta.setValueToReplace(new PrismPropertyValue<>(now, OriginType.USER_POLICY, null));
		focusContext.swallowToProjectionWaveSecondaryDelta(validityChangeTimestampDelta);
	}
	
	private <F extends ObjectType> void recordEffectiveStatusDelta(LensFocusContext<F> focusContext, 
			ActivationStatusType effectiveStatusNew, XMLGregorianCalendar now)
			throws SchemaException {
		PrismContainerDefinition<ActivationType> activationDefinition = getActivationDefinition();
		
		// We always want explicit delta for effective status even if there is no real change
		// we want to propagate enable/disable events to all the resources, even if we are enabling
		// already enabled user (some resources may be disabled)
		// This may produce duplicate delta, but that does not matter too much. The duplicate delta
		// will be filtered out later.
		PrismPropertyDefinition<ActivationStatusType> effectiveStatusDef = activationDefinition.findPropertyDefinition(ActivationType.F_EFFECTIVE_STATUS);
		PropertyDelta<ActivationStatusType> effectiveStatusDelta 
				= effectiveStatusDef.createEmptyDelta(new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
		effectiveStatusDelta.setValueToReplace(new PrismPropertyValue<ActivationStatusType>(effectiveStatusNew, OriginType.USER_POLICY, null));
		if (!focusContext.alreadyHasDelta(effectiveStatusDelta)){
			focusContext.swallowToProjectionWaveSecondaryDelta(effectiveStatusDelta);
		}
		
		// It is not enough to check alreadyHasDelta(). The change may happen in previous waves
		// and the secondary delta may no longer be here. When it comes to disableTimestamp we even
		// cannot rely on natural filtering of already executed deltas as the timestamp here may
		// be off by several milliseconds. So explicitly check for the change here. 
		PrismObject<F> objectCurrent = focusContext.getObjectCurrent();
		if (objectCurrent != null) {
			PrismProperty<ActivationStatusType> effectiveStatusPropCurrent = objectCurrent.findProperty(SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS);
			if (effectiveStatusPropCurrent != null && effectiveStatusNew.equals(effectiveStatusPropCurrent.getRealValue())) {
				LOGGER.trace("Skipping setting disableTimestamp because there was no change");
				return;
			}
		}
				
		PropertyDelta<XMLGregorianCalendar> timestampDelta = LensUtil.createActivationTimestampDelta(effectiveStatusNew, now, activationDefinition, OriginType.USER_POLICY);
		if (!focusContext.alreadyHasDelta(timestampDelta)) {
			focusContext.swallowToProjectionWaveSecondaryDelta(timestampDelta);
		}
	}
	
	
	private PrismContainerDefinition<ActivationType> getActivationDefinition() {
		if (activationDefinition == null) {
			ComplexTypeDefinition focusDefinition = prismContext.getSchemaRegistry().findComplexTypeDefinition(FocusType.COMPLEX_TYPE);
			activationDefinition = focusDefinition.findContainerDefinition(FocusType.F_ACTIVATION);
		}
		return activationDefinition;
	}
	
	private PrismPropertyDefinition<Integer> getFailedLoginsDefinition() {
		if (failedLoginsDefinition == null) {
			PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
			failedLoginsDefinition = userDef.findPropertyDefinition(SchemaConstants.PATH_CREDENTIALS_PASSWORD_FAILED_LOGINS);
		}
		return failedLoginsDefinition;
	}
	
	/**
	 * Adds deltas for iteration and iterationToken to the focus if needed.
	 */
	private <F extends FocusType> void addIterationTokenDeltas(LensFocusContext<F> focusContext, int iteration, String iterationToken) throws SchemaException {
		PrismObject<F> objectCurrent = focusContext.getObjectCurrent();
		if (objectCurrent != null) {
			Integer iterationOld = objectCurrent.asObjectable().getIteration();
			String iterationTokenOld = objectCurrent.asObjectable().getIterationToken();
			if (iterationOld != null && iterationOld == iteration &&
					iterationTokenOld != null && iterationTokenOld.equals(iterationToken)) {
				// Already stored
				return;
			}
		}
		PrismObjectDefinition<F> objDef = focusContext.getObjectDefinition();
		
		PrismPropertyValue<Integer> iterationVal = new PrismPropertyValue<Integer>(iteration);
		iterationVal.setOriginType(OriginType.USER_POLICY);
		PropertyDelta<Integer> iterationDelta = PropertyDelta.createReplaceDelta(objDef, 
				FocusType.F_ITERATION, iterationVal);
		focusContext.swallowToSecondaryDelta(iterationDelta);
		
		PrismPropertyValue<String> iterationTokenVal = new PrismPropertyValue<String>(iterationToken);
		iterationTokenVal.setOriginType(OriginType.USER_POLICY);
		PropertyDelta<String> iterationTokenDelta = PropertyDelta.createReplaceDelta(objDef, 
				FocusType.F_ITERATION_TOKEN, iterationTokenVal);
		focusContext.swallowToSecondaryDelta(iterationTokenDelta);
		
	}
	
	private <F extends FocusType> void processAssignmentActivation(LensContext<F> context, XMLGregorianCalendar now, 
			OperationResult result) throws SchemaException {
		DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
		if (evaluatedAssignmentTriple == null) {
			// Code path that should not normally happen. But is used in some tests and may
			// happen during partial processing.
			return;
		}
		// We care only about existing assignments here. New assignments will be taken care of in the executor
		// (OperationalDataProcessor). And why care about deleted assignments?
		Collection<EvaluatedAssignmentImpl<?>> zeroSet = evaluatedAssignmentTriple.getZeroSet();
		if (zeroSet == null) {
			return;
		}
		LensFocusContext<F> focusContext = context.getFocusContext();
		for (EvaluatedAssignmentImpl<?> evaluatedAssignment: zeroSet) {
			AssignmentType assignmentType = evaluatedAssignment.getAssignmentType();
			ActivationType currentActivationType = assignmentType.getActivation();
			ActivationStatusType expectedEffectiveStatus = activationComputer.getEffectiveStatus(assignmentType.getLifecycleState(), currentActivationType);
			if (currentActivationType == null) {
				PrismContainerDefinition<ActivationType> activationDef = focusContext.getObjectDefinition().findContainerDefinition(SchemaConstants.PATH_ASSIGNMENT_ACTIVATION);
				ContainerDelta<ActivationType> activationDelta = activationDef.createEmptyDelta(
						new ItemPath(
								new NameItemPathSegment(FocusType.F_ASSIGNMENT), new IdItemPathSegment(assignmentType.getId()),
								new NameItemPathSegment(AssignmentType.F_ACTIVATION)
							));
				ActivationType newActivationType = new ActivationType();
				activationDelta.setValuesToReplace(newActivationType.asPrismContainerValue());
				newActivationType.setEffectiveStatus(expectedEffectiveStatus);
				focusContext.swallowToSecondaryDelta(activationDelta);
			} else {
				ActivationStatusType currentEffectiveStatus = currentActivationType.getEffectiveStatus();
				if (!expectedEffectiveStatus.equals(currentEffectiveStatus)) {
					PrismPropertyDefinition<ActivationStatusType> effectiveStatusPropertyDef = focusContext.getObjectDefinition().findPropertyDefinition(SchemaConstants.PATH_ASSIGNMENT_ACTIVATION_EFFECTIVE_STATUS);
					PropertyDelta<ActivationStatusType> effectiveStatusDelta = effectiveStatusPropertyDef.createEmptyDelta(
							new ItemPath(
								new NameItemPathSegment(FocusType.F_ASSIGNMENT), new IdItemPathSegment(assignmentType.getId()),
								new NameItemPathSegment(AssignmentType.F_ACTIVATION),
								new NameItemPathSegment(ActivationType.F_EFFECTIVE_STATUS)
							));
					effectiveStatusDelta.setValueToReplace(new PrismPropertyValue<ActivationStatusType>(expectedEffectiveStatus));
					focusContext.swallowToSecondaryDelta(effectiveStatusDelta);
				}
			}
		}
	}
	

}
