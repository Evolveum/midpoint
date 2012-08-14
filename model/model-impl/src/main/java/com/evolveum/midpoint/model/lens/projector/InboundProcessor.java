/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.model.lens.projector;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.common.valueconstruction.ValueConstructionFactory;
import com.evolveum.midpoint.model.controller.Filter;
import com.evolveum.midpoint.model.controller.FilterManager;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PropertyPathSegment;
import com.evolveum.midpoint.prism.SourceType;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Processor that takes changes from synchronization and updates user attributes if necessary
 * (by creating secondary user object delta {@link ObjectDelta}).
 *
 * @author lazyman
 * @author Radovan Semancik
 */
@Component
public class InboundProcessor {

    public static final String PROCESS_INBOUND_HANDLING = InboundProcessor.class.getName() + ".processInbound";
    private static final Trace LOGGER = TraceManager.getTrace(InboundProcessor.class);

    @Autowired(required = true)
    private PrismContext prismContext;
    @Autowired(required = true)
    private FilterManager<Filter> filterManager;
    @Autowired(required = true)
    private ValueConstructionFactory valueConstructionFactory;

    <F extends ObjectType, P extends ObjectType> void processInbound(LensContext<F,P> context, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
    	LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
    	if (focusContext.getObjectTypeClass() != UserType.class) {
    		// We can do this only for user.
    		return;
    	}
    	LensContext<UserType,AccountShadowType> usContext = (LensContext<UserType,AccountShadowType>) context;
    	LensFocusContext<UserType> userContext = (LensFocusContext<UserType>)focusContext;
    	
    	OperationResult subResult = result.createSubresult(PROCESS_INBOUND_HANDLING);

        ObjectDelta<UserType> userSecondaryDelta = userContext.getWaveSecondaryDelta();

        if (userSecondaryDelta != null && ChangeType.DELETE.equals(userSecondaryDelta.getChangeType())) {
            //we don't need to do inbound if we are deleting this user
            return;
        }

        try {
            for (LensProjectionContext<AccountShadowType> accountContext : usContext.getProjectionContexts()) {
            	ResourceShadowDiscriminator rat = accountContext.getResourceShadowDiscriminator();
            	
            	ObjectDelta<AccountShadowType> aPrioriDelta = getAPrioriDelta(context, accountContext);
            	
            	if (!accountContext.isDoReconciliation() && aPrioriDelta == null) {
            		LOGGER.trace("Skipping processing of inbound expressions for account {}: no reconciliation and no a priori delta", rat);
            		continue;
            	}
//                LOGGER.trace("Processing inbound expressions for account {} starting", rat);

                RefinedAccountDefinition accountDefinition = accountContext.getRefinedAccountDefinition();
                if (accountDefinition == null) {
                    LOGGER.error("Definition for account type {} not found in the context, but it " +
                            "should be there, dumping context:\n{}", rat, context.dump());
                    throw new IllegalStateException("Definition for account type " + rat
                            + " not found in the context, but it should be there");
                }

                ObjectDelta<AccountShadowType> accountDelta = accountContext.getDelta();
                if (accountDelta != null && ChangeType.DELETE.equals(accountDelta.getChangeType())) {
                    //we don't need to do inbound if account was deleted
                    continue;
                }

                processInboundExpressionsForAccount(usContext, accountContext, accountDefinition, aPrioriDelta, result);
            }

        } finally {
            subResult.computeStatus();
        }
    }

    private void processInboundExpressionsForAccount(LensContext<UserType,AccountShadowType> context, 
    		LensProjectionContext<AccountShadowType> accContext,
            RefinedAccountDefinition accountDefinition, ObjectDelta<AccountShadowType> aPrioriDelta, OperationResult result)
    		throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
    	
        if (aPrioriDelta == null && accContext.getObjectOld() == null) {
            LOGGER.debug("Nothing to process in inbound, both a priori delta and account old were null.");
            return;
        }

        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getWaveSecondaryDelta();
        
        PrismObject<AccountShadowType> oldAccount = accContext.getObjectOld();
        for (QName name : accountDefinition.getNamesOfAttributesWithInboundExpressions()) {
            PropertyDelta<?> accountAttributeDelta = null;
            if (aPrioriDelta != null) {
                accountAttributeDelta = aPrioriDelta.findPropertyDelta(new PropertyPath(SchemaConstants.I_ATTRIBUTES), name);
                if (accountAttributeDelta == null) {
                    LOGGER.trace("Skipping inbound for {} in {}: Account a priori delta exists, but doesn't have change for processed property.",
                    		name, accContext.getResourceShadowDiscriminator());
                    continue;
                }
            }

            RefinedAttributeDefinition attrDef = accountDefinition.getAttributeDefinition(name);
            List<ValueAssignmentType> inboundJaxbTypes = attrDef.getInboundAssignmentTypes();
            LOGGER.trace("Processing inbound for {} in {}; ({} expressions)", new Object[]{
            		DebugUtil.prettyPrint(name), accContext.getResourceShadowDiscriminator(), (inboundJaxbTypes != null ? inboundJaxbTypes.size() : 0)});

            for (ValueAssignmentType inboundJaxbType : inboundJaxbTypes) {
                if (checkInitialSkip(inboundJaxbType, context.getFocusContext().getObjectNew())) {
                    LOGGER.debug("Skipping because of initial flag.");
                    continue;
                }

                PropertyDelta<?> userPropertyDelta = null;
                if (aPrioriDelta != null) {
                    LOGGER.debug("Processing inbound from a priori delta.");
                    userPropertyDelta = evaluateInboundExpressionFromDelta(inboundJaxbType, accountAttributeDelta, 
                    		context.getFocusContext().getObjectNew(), accContext.getResource(), result);
                } else if (oldAccount != null) {
                	if (!accContext.isFullShadow()) {
                		throw new SystemException("Attept to execute inbound expression on account shadow (not full account)");
                	}
                    LOGGER.debug("Processing inbound from account sync absolute state (oldAccount).");
                    PrismProperty<?> oldAccountProperty = oldAccount.findProperty(new PropertyPath(AccountShadowType.F_ATTRIBUTES, name));
                    userPropertyDelta = evaluateInboundExpressionFromAbsolute(inboundJaxbType, oldAccountProperty, context.getFocusContext().getObjectNew(),
                    		accContext.getResource(), result);
                }

                if (userPropertyDelta != null && !userPropertyDelta.isEmpty()) {
                    LOGGER.trace("Created delta (from inbound expression) \n{}", new Object[]{userPropertyDelta.debugDump(3)});
                    context.getFocusContext().swallowToWaveSecondaryDelta(userPropertyDelta);
                    context.recomputeFocus();
                } else {
                    LOGGER.trace("Created delta (from inbound expression) was null or empty.");
                }
            }
        }
        processSpecialPropertyInbound(accountDefinition.getCredentialsInbound(), SchemaConstants.PATH_PASSWORD_VALUE,
        		context.getFocusContext().getObjectNew(), accContext, accountDefinition, context, result);
        processSpecialPropertyInbound(accountDefinition.getActivationInbound(), SchemaConstants.PATH_ACTIVATION_ENABLE,
        		context.getFocusContext().getObjectNew(), accContext, accountDefinition, context, result);
    }

    /**
	 * A priori delta is a delta that was executed in a previous "step". That means it is either delta from a previous
	 * wave or a sync delta (in wave 0).
	 */
	private <F extends ObjectType, P extends ObjectType> ObjectDelta<AccountShadowType> getAPrioriDelta(LensContext<F,P> context, LensProjectionContext<AccountShadowType> accountContext) throws SchemaException {
		int wave = context.getWave();
		if (wave == 0) {
			return accountContext.getSyncDelta();
		}
		if (wave == accountContext.getWave() + 1) {
			// If this resource was processed in a previous wave ....
			return accountContext.getDelta();
		}
		return null;
	}

	private boolean checkInitialSkip(ValueAssignmentType inbound, PrismObject<UserType> newUser) {
        ValueConstructionType valueConstruction = inbound.getSource();
        if (valueConstruction == null) {
            return false;
        }

        boolean initial = valueConstruction.isInitial() == null ? false : valueConstruction.isInitial();
        PrismProperty<?> property = newUser.findProperty(createUserPropertyPath(inbound));
        if (initial && property != null && !property.isEmpty()) {
            return true;
        }

        return false;
    }

    private <T> PropertyDelta<T> evaluateInboundExpressionFromAbsolute(ValueAssignmentType inbound, PrismProperty<T> oldAccountProperty,
            PrismObject<UserType> newUser, ResourceType resource, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        List<ValueFilterType> filters = inbound.getValueFilter();
        
        if (oldAccountProperty != null && oldAccountProperty.hasRaw()) {
        	throw new SystemException("Property "+oldAccountProperty+" has raw parsing state, such property cannot be used in inbound expressions");
        }
        
        
        PropertyPath targetUserPropertyPath = createUserPropertyPath(inbound);
        PrismProperty<T> targetUserProperty = null;
        if (newUser != null) {
        	targetUserProperty = newUser.findProperty(targetUserPropertyPath);
        }
        PrismPropertyDefinition targetPropertyDef = newUser.getDefinition().findPropertyDefinition(targetUserPropertyPath);
        if (targetPropertyDef == null) {
        	throw new SchemaException("No definition for user property "+targetUserPropertyPath+", cannot process inbound expression in "+resource);
        }
        
        PrismProperty<T> sourceProperty = null;
        // Try to process source
        if (oldAccountProperty != null) {
        	ValueConstructionType sourceValueConstructionType = inbound.getSource();
        	ValueConstruction<PrismPropertyValue<T>> valueConstruction = null;
        	if (sourceValueConstructionType != null) {
        		valueConstruction = valueConstructionFactory.createValueConstruction(sourceValueConstructionType, targetPropertyDef, 
	        			"inbound expression for "+oldAccountProperty.getName());
        	} else {
        		valueConstruction = valueConstructionFactory.createDefaultValueConstruction(targetPropertyDef, 
	        			"inbound expression for "+oldAccountProperty.getName());
        	}
        	valueConstruction.setInput(oldAccountProperty);
        	valueConstruction.setInputDelta(null);
        	valueConstruction.setOutputDefinition(targetPropertyDef);
        	valueConstruction.addVariableDefinition(ExpressionConstants.VAR_USER, newUser);
        	// Add variables
        	valueConstruction.evaluate(result);
        	PrismValueDeltaSetTriple<PrismPropertyValue<T>> triple = valueConstruction.getOutputTriple();
        	sourceProperty = (PrismProperty<T>) valueConstruction.getOutput();
        }
        
        PropertyDelta<T> delta = null;
        if (targetUserProperty != null) {
            LOGGER.trace("Simple property comparing user property {} to computed property {} ",
                    new Object[]{targetUserProperty, sourceProperty});
            //simple property comparing if user property exists
            delta = targetUserProperty.diff(sourceProperty, targetUserPropertyPath);
            if (delta != null) {
	            delta.setName(targetUserPropertyPath.last().getName());
	            delta.setParentPath(targetUserPropertyPath.allExceptLast());
            }
        } else {
            if (sourceProperty != null) {
                LOGGER.trace("Adding user property because inbound say so (account doesn't contain that value)");
                //if user property doesn't exist we have to add it (as delta), because inbound say so
                delta = PropertyDelta.createDelta(targetUserPropertyPath, newUser.getDefinition());
                delta.addValuesToAdd(sourceProperty.getClonedValues());
            }
            //we don't have to create delta, because everything is alright
            LOGGER.trace("We don't have to create delta, everything is alright.");
        }

        return delta;
    }
    
    private <T> PropertyDelta<T> evaluateInboundExpressionFromDelta(ValueAssignmentType inbound, PropertyDelta<T> accountAttributeDelta,
            PrismObject<UserType> newUser, ResourceType resource, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        List<ValueFilterType> filters = inbound.getValueFilter();

        PropertyPath targetUserPropertyPath = createUserPropertyPath(inbound);
        PrismProperty<T> targetUserProperty = (PrismProperty<T>) newUser.findProperty(targetUserPropertyPath);        
        PrismPropertyDefinition targetPropertyDef = newUser.getDefinition().findPropertyDefinition(targetUserPropertyPath);
        if (targetPropertyDef == null) {
        	throw new SchemaException("No definition for user property "+targetUserPropertyPath+", cannot process inbound expression in "+resource);
        }
        PropertyDelta<T> delta = new PropertyDelta<T>(targetUserPropertyPath, targetPropertyDef);
        delta.checkConsistence();
        
        PrismValueDeltaSetTriple<PrismPropertyValue<T>> triple = null;
        
        ValueConstructionType sourceValueConstructionType = inbound.getSource();
    	ValueConstruction<PrismPropertyValue<T>> valueConstruction = null;
    	if (sourceValueConstructionType != null) {
    		valueConstruction = valueConstructionFactory.createValueConstruction(sourceValueConstructionType, targetPropertyDef, 
        			"inbound expression for "+accountAttributeDelta.getName());
    	} else {
    		valueConstruction = valueConstructionFactory.createDefaultValueConstruction(targetPropertyDef, 
        			"inbound expression for "+accountAttributeDelta.getName());
    	}
    	valueConstruction.setInput(null);
    	valueConstruction.setInputDelta(accountAttributeDelta);
    	valueConstruction.setOutputDefinition(targetPropertyDef);
    	valueConstruction.addVariableDefinition(ExpressionConstants.VAR_USER, newUser);
    	// Add variables
    	valueConstruction.evaluate(result);
    	triple = valueConstruction.getOutputTriple();
        
        if (triple.getPlusSet() != null) {
            LOGGER.trace("Checking account sync property delta values to add");
            for (PrismPropertyValue<T> value : triple.getPlusSet()) {
                PrismPropertyValue<T> filteredValue = filterValue(value, filters);

                if (targetUserProperty != null && targetUserProperty.hasRealValue(filteredValue)) {
                    continue;
                }

                //if property is not multi value replace existing attribute
                if (targetUserProperty != null && !targetUserProperty.getDefinition().isMultiValue() && !targetUserProperty.isEmpty()) {
                    Collection<PrismPropertyValue<T>> replace = new ArrayList<PrismPropertyValue<T>>();
                    replace.add(filteredValue);
                    delta.setValuesToReplace(replace);
                    delta.checkConsistence();
                } else {
                    delta.addValueToAdd(filteredValue);
                    delta.checkConsistence();
                }
            }
        }
        if (triple.getMinusSet() != null) {
            LOGGER.trace("Checking account sync property delta values to delete");
            for (PrismPropertyValue<T> value : triple.getMinusSet()) {
                PrismPropertyValue<T> filteredValue = filterValue(value, filters);

                if (targetUserProperty == null || targetUserProperty.hasRealValue(filteredValue)) {
                    delta.addValueToDelete(filteredValue);
                    delta.checkConsistence();
                }
            }
        }

        //if nothing changes was generated return null
        return delta.getValues(Object.class).isEmpty() ? null : delta;
    }

    private PropertyPath createUserPropertyPath(ValueAssignmentType inbound) {
        PropertyPath path = new XPathHolder(inbound.getTarget()).toPropertyPath();
        List<PropertyPathSegment> segments = path.getSegments();
        if (!segments.isEmpty() && SchemaConstants.I_USER.equals(segments.get(0).getName())) {
            segments.remove(0);
        }

        return path;
    }

    private <T> PrismPropertyValue<T> filterValue(PrismPropertyValue<T> propertyValue, List<ValueFilterType> filters) {
        PrismPropertyValue<T> filteredValue = propertyValue.clone();
        filteredValue.setType(SourceType.INBOUND);

        if (filters == null || filters.isEmpty()) {
            return filteredValue;
        }

        for (ValueFilterType filter : filters) {
            Filter filterInstance = filterManager.getFilterInstance(filter.getType(), filter.getAny());
            filterInstance.apply(filteredValue);
        }

        return filteredValue;
    }

    /**
     * Processing for special (fixed-schema) properties such as credentials and activation. 
     */
    private void processSpecialPropertyInbound(ValueAssignmentType inbound, PropertyPath path,
            PrismObject<UserType> newUser, LensProjectionContext<AccountShadowType> accContext, RefinedAccountDefinition accountDefinition,
            LensContext<UserType,AccountShadowType> context, OperationResult opResult) throws SchemaException {
        if (inbound == null || newUser == null) {
            return;
        }

        ValueConstructionType valueConstruction = inbound.getSource();
        boolean initial = valueConstruction.isInitial() == null ? false : valueConstruction.isInitial();

        PrismProperty<?> property = newUser.findOrCreateProperty(path);
        if (initial && !property.isEmpty()) {
            //inbound will be constructed only if initial == false or initial == true and value doesn't exist
            return;
        }
        
        ObjectDelta<UserType> userPrimaryDelta = context.getFocusContext().getPrimaryDelta();
        if (userPrimaryDelta != null) {
        	PropertyDelta primaryPropDelta = userPrimaryDelta.findPropertyDelta(path);
        	if (primaryPropDelta != null && primaryPropDelta.isReplace()) {
        		// Replace primary delta overrides any inbound
        		return;
        	}
        }

        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getWaveSecondaryDelta();
        if (userSecondaryDelta != null) {
	        PropertyDelta<?> delta = userSecondaryDelta.findPropertyDelta(path);
	        if (delta != null) {
	            //remove delta if exists, it will be handled by inbound
	            userSecondaryDelta.getModifications().remove(delta);
	        }
        }

        if (accContext.getObjectNew() == null) {
            accContext.recompute();
            if (accContext.getObjectNew() == null) {
                // Still null? something must be really wrong here.
                String message = "Recomputing account " + accContext.getResourceShadowDiscriminator()
                        + " results in null new account. Something must be really broken.";
                LOGGER.error(message);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Account context:\n{}", accContext.dump());
                }
                throw new SystemException(message);
            }
        }

        PrismProperty input = accContext.getObjectNew().findProperty(path);
        PrismProperty result;
        try {
        	// TODO: is the parentPath correct (null)?
            ValueConstruction construction = valueConstructionFactory.createValueConstruction(
                    valueConstruction, property.getDefinition(), "Inbound value construction");
            construction.setInput(input);
            construction.evaluate(opResult);
            result = (PrismProperty) construction.getOutput();
        } catch (SchemaException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SchemaException(ex.getMessage(), ex);
        }

        PropertyDelta<?> delta = property.diff(result, path);
        if (delta != null && !delta.isEmpty()) {
        	delta.setParentPath(path.allExceptLast());
        	context.getFocusContext().swallowToWaveSecondaryDelta(delta);
        }
    }
}
