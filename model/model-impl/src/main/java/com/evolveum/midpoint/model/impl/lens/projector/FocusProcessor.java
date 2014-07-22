/*
 * Copyright (c) 2010-2014 Evolveum
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

import static com.evolveum.midpoint.common.InternalsConfig.consistencyChecks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.BooleanUtils;
import org.apache.xpath.FoundIndex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.OidUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenerateExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterationSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingEvaluationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
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
	
	@Autowired(required = true)
    private InboundProcessor inboundProcessor;
	
	@Autowired(required = true)
    private AssignmentProcessor assignmentProcessor;
	
	@Autowired(required = true)
	private ObjectTemplateProcessor objectTemplateProcessor;
	
	@Autowired(required = true)
	private MappingFactory mappingFactory;

	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired(required = true)
	private PasswordPolicyProcessor passwordPolicyProcessor;
	
	@Autowired(required = true)
	private ModelObjectResolver modelObjectResolver;
	
	@Autowired(required = true)
	private ActivationComputer activationComputer;
	
	@Autowired(required = true)
	private ExpressionFactory expressionFactory;

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;
	
	@Autowired(required = true)
    private MappingEvaluationHelper mappingHelper;

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
    	
    	LensContext<F> fContext = (LensContext<F>) context;
    	LensFocusContext<F> fFocusContext = fContext.getFocusContext();
    	
    	processFocusFocus((LensContext<F>)context, activityDescription, now, task, result);
	}
	
	private <F extends FocusType> void processFocusFocus(LensContext<F> context, String activityDescription,
			XMLGregorianCalendar now, Task task, OperationResult result)
					throws ObjectNotFoundException,
		            SchemaException, ExpressionEvaluationException, PolicyViolationException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
    	
		LensFocusContext<F> focusContext = context.getFocusContext();
		ObjectTemplateType objectTemplate = context.getFocusTemplate();

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
		
			ExpressionVariables variables = Utils.getDefaultExpressionVariables(focusContext.getObjectNew(), null, null, null, context.getSystemConfiguration());
			if (iterationToken == null) {
				iterationToken = LensUtil.formatIterationToken(context, focusContext, 
						iterationSpecificationType, iteration, expressionFactory, variables, task, result);
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
			LOGGER.trace("Focus {} processing, iteration {}, token '{}'", new Object[]{focusContext.getHumanReadableName(), iteration, iterationToken});
			
			String conflictMessage;
			if (!LensUtil.evaluateIterationCondition(context, focusContext, 
					iterationSpecificationType, iteration, iterationToken, true, expressionFactory, variables, task, result)) {
				
				conflictMessage = "pre-iteration condition was false";
				LOGGER.debug("Skipping iteration {}, token '{}' for {} because the pre-iteration condition was false",
						new Object[]{iteration, iterationToken, focusContext.getHumanReadableName()});
			} else {
				
				// INBOUND
				
				if (consistencyChecks) context.checkConsistence();
		        // Loop through the account changes, apply inbound expressions
		        inboundProcessor.processInbound(context, now, task, result);
		        if (consistencyChecks) context.checkConsistence();
		        context.recomputeFocus();
		        LensUtil.traceContext(LOGGER, activityDescription, "inbound", false, context, false);
		        if (consistencyChecks) context.checkConsistence();
				
				
		        // ACTIVATION
		        
		        processActivation(context, now, result);
				
				
		        // OBJECT TEMPLATE (before assignments)
		        
		        objectTemplateProcessor.processTemplate(context, ObjectTemplateMappingEvaluationPhaseType.BEFORE_ASSIGNMENTS,
		        		now, task, result);
		        
		        
		        // ASSIGNMENTS
		        
		        assignmentProcessor.processAssignmentsProjections(context, now, task, result);
		        assignmentProcessor.processOrgAssignments(context, result);
		        context.recompute();
		        
		        assignmentProcessor.checkForAssignmentConflicts(context, result);
		        
		        
		        // OBJECT TEMPLATE (after assignments)
		        
		        objectTemplateProcessor.processTemplate(context, ObjectTemplateMappingEvaluationPhaseType.AFTER_ASSIGNMENTS,
		        		now, task, result);
		        context.recompute();
		        
		        // PASSWORD POLICY
				
		        passwordPolicyProcessor.processPasswordPolicy(focusContext, context, result);
		        
		        
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
						throw new SchemaException("No name in new object "+objectName+" as produced by template "+objectTemplate+
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
		        	
		        	if (LensUtil.evaluateIterationCondition(context, focusContext, 
		        			iterationSpecificationType, iteration, iterationToken, false, expressionFactory, variables, 
		        			task, result)) {
	    				// stop the iterations
	    				break;
	    			} else {
	    				conflictMessage = "post-iteration condition was false";
	    				LOGGER.debug("Skipping iteration {}, token '{}' for {} because the post-iteration condition was false",
	    						new Object[]{iteration, iterationToken, focusContext.getHumanReadableName()});
	    			}
		        } else {
			        LOGGER.trace("Current focus does not satisfy constraints. Conflicting object: {}; iteration={}, maxIterations={}",
			        		new Object[]{checker.getConflictingObject(), iteration, maxIterations});
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
	        if (iteration > maxIterations) {
	        	StringBuilder sb = new StringBuilder();
	        	if (iteration == 1) {
	        		sb.append("Error processing ");
	        	} else {
	        		sb.append("Too many iterations ("+iteration+") for ");
	        	}
	        	sb.append(focusContext.getHumanReadableName());
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
	        cleanupContext(focusContext);
		}
		
		addIterationTokenDeltas(focusContext, iteration, iterationToken);
		if (consistencyChecks) context.checkConsistence();
		
	}
	
	private <F extends FocusType> void applyObjectPolicyConstraints(LensFocusContext<F> focusContext, ObjectPolicyConfigurationType objectPolicyConfigurationType) throws SchemaException {
		if (objectPolicyConfigurationType == null) {
			return;
		}
		
		PrismObject<F> focusNew = focusContext.getObjectNew();
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
			if (focusNew != null) {
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

	private <F extends FocusType> void processActivation(LensContext<F> context, XMLGregorianCalendar now, 
			OperationResult result) 
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		
		if (focusContext.isDelete()) {
			LOGGER.trace("Skipping processing of focus activation: focus delete");
			return;
		}
		
		TimeIntervalStatusType validityStatusNew = null;
		TimeIntervalStatusType validityStatusOld = null;
		XMLGregorianCalendar validityChangeTimestamp = null;
		
		ActivationType activationNew = null;
		ActivationType activationOld = null;
		
		PrismObject<F> focusNew = focusContext.getObjectNew();
		if (focusNew != null) {
			activationNew = focusNew.asObjectable().getActivation();
			if (activationNew != null) {
				validityStatusNew = activationComputer.getValidityStatus(activationNew, now);
				validityChangeTimestamp = activationNew.getValidityChangeTimestamp();
			}
		}
		
		PrismObject<F> focusOld = focusContext.getObjectOld();
		if (focusOld != null) {
			activationOld = focusOld.asObjectable().getActivation();
			if (activationOld != null) {
				validityStatusOld = activationComputer.getValidityStatus(activationOld, validityChangeTimestamp);
			}
		}
		
		if (validityStatusOld == validityStatusNew) {
			// No change, (almost) no work
			if (validityStatusNew != null && activationNew.getValidityStatus() == null) {
				// There was no validity change. But the status is not recorded. So let's record it so it can be used in searches. 
				recordValidityDelta(focusContext, validityStatusNew, now);
			} else {
				LOGGER.trace("Skipping validity processing because there was no change ({} -> {})", validityStatusOld, validityStatusNew);
			}
		} else {
			LOGGER.trace("Validity change {} -> {}", validityStatusOld, validityStatusNew);
			recordValidityDelta(focusContext, validityStatusNew, now);
		}
		
		ActivationStatusType effectiveStatusNew = activationComputer.getEffectiveStatus(activationNew, validityStatusNew, ActivationStatusType.DISABLED);
		ActivationStatusType effectiveStatusOld = activationComputer.getEffectiveStatus(activationOld, validityStatusOld, ActivationStatusType.DISABLED);
		
		if (effectiveStatusOld == effectiveStatusNew) {
			// No change, (almost) no work
			if (effectiveStatusNew != null && (activationNew == null || activationNew.getEffectiveStatus() == null)) {
				// There was no effective status change. But the status is not recorded. So let's record it so it can be used in searches. 
				recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
			} else {
				if (focusContext.getPrimaryDelta() != null && focusContext.getPrimaryDelta().hasItemDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS)) {
					LOGGER.trace("Forcing effective status delta even though there was no change ({} -> {}) because there is explicit administrativeStatus delta", effectiveStatusOld, effectiveStatusNew);
					// We need this to force the change down to the projections later in the activation processor
					// some of the mappings will use effectiveStatus as a source, therefore there has to be a delta for the mapping to work correctly
					recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
				} else {
					LOGGER.trace("Skipping effective status processing because there was no change ({} -> {})", effectiveStatusOld, effectiveStatusNew);
				}
			}
		} else {
			LOGGER.trace("Effective status change {} -> {}", effectiveStatusOld, effectiveStatusNew);
			recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
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
			validityStatusDelta.setValueToReplace(new PrismPropertyValue<TimeIntervalStatusType>(validityStatusNew, OriginType.USER_POLICY, null));
		}
		focusContext.swallowToProjectionWaveSecondaryDelta(validityStatusDelta);
		
		PrismPropertyDefinition<XMLGregorianCalendar> validityChangeTimestampDef = activationDefinition.findPropertyDefinition(ActivationType.F_VALIDITY_CHANGE_TIMESTAMP);
		PropertyDelta<XMLGregorianCalendar> validityChangeTimestampDelta 
				= validityChangeTimestampDef.createEmptyDelta(new ItemPath(UserType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP));
		validityChangeTimestampDelta.setValueToReplace(new PrismPropertyValue<XMLGregorianCalendar>(now, OriginType.USER_POLICY, null));
		focusContext.swallowToProjectionWaveSecondaryDelta(validityChangeTimestampDelta);
	}
	
	private <F extends ObjectType> void recordEffectiveStatusDelta(LensFocusContext<F> focusContext, 
			ActivationStatusType effectiveStatusNew, XMLGregorianCalendar now)
			throws SchemaException {
		PrismContainerDefinition<ActivationType> activationDefinition = getActivationDefinition();
		
		PrismPropertyDefinition<ActivationStatusType> effectiveStatusDef = activationDefinition.findPropertyDefinition(ActivationType.F_EFFECTIVE_STATUS);
		PropertyDelta<ActivationStatusType> effectiveStatusDelta 
				= effectiveStatusDef.createEmptyDelta(new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
		effectiveStatusDelta.setValueToReplace(new PrismPropertyValue<ActivationStatusType>(effectiveStatusNew, OriginType.USER_POLICY, null));
		focusContext.swallowToProjectionWaveSecondaryDelta(effectiveStatusDelta);
		
		PropertyDelta<XMLGregorianCalendar> timestampDelta = LensUtil.createActivationTimestampDelta(effectiveStatusNew, now, activationDefinition, OriginType.USER_POLICY);
		focusContext.swallowToProjectionWaveSecondaryDelta(timestampDelta);
	}
	
	
	private PrismContainerDefinition<ActivationType> getActivationDefinition() {
		if (activationDefinition == null) {
			ComplexTypeDefinition focusDefinition = prismContext.getSchemaRegistry().findComplexTypeDefinition(FocusType.COMPLEX_TYPE);
			activationDefinition = focusDefinition.findContainerDefinition(FocusType.F_ACTIVATION);
		}
		return activationDefinition;
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

}
