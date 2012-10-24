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

import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.PropertyValueWithOrigin;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author lazyman
 * @author Radovan Semancik
 */
@Component
public class ReconciliationProcessor {

	@Autowired(required=true)
	PrismContext prismContext;
	
    public static final String PROCESS_RECONCILIATION = ReconciliationProcessor.class.getName() + ".processReconciliation";
    private static final Trace LOGGER = TraceManager.getTrace(ReconciliationProcessor.class);

    /**
     * we have two options:
     * <ul>
     * <li>we take account before change (oldAccount) from account sync context, no deltas and we compare
     * it with everything that is in zero delta set triples</li>
     * <li>we take account before change (oldAccount) from account sync context, we apply deltas and then
     * we compare it with everything that is in zero and plus delta set triples</li>
     * </ul>
     *
     * @param context
     * @param result
     * @throws SchemaException 
     */
    <F extends ObjectType, P extends ObjectType> void processReconciliation(LensContext<F,P> context, LensProjectionContext<P> projectionContext, OperationResult result) throws SchemaException {
    	LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
    	if (focusContext.getObjectTypeClass() != UserType.class) {
    		// We can do this only for user.
    		return;
    	}
    	processReconciliationUser((LensContext<UserType,AccountShadowType>) context, (LensProjectionContext<AccountShadowType>)projectionContext, result);
    }
    
    void processReconciliationUser(LensContext<UserType,AccountShadowType> context, LensProjectionContext<AccountShadowType> accContext, OperationResult result) throws SchemaException {
    
    OperationResult subResult = result.createSubresult(PROCESS_RECONCILIATION);    
        
        try {
            if (!accContext.isDoReconciliation()) {
            	return;
            }
            
            SynchronizationPolicyDecision policyDecision = accContext.getSynchronizationPolicyDecision();
            if (policyDecision != null && 
            		(policyDecision == SynchronizationPolicyDecision.DELETE || policyDecision == SynchronizationPolicyDecision.UNLINK)) {
            	return;
            }

            if (accContext.getObjectOld() == null) {
                LOGGER.warn("Can't do reconciliation. Account context doesn't contain old version of account.");
                return;
            }
            
            LOGGER.trace("Attribute reconciliation processing ACCOUNT {}",accContext.getResourceShadowDiscriminator());

            Map<QName, DeltaSetTriple<PropertyValueWithOrigin>> squeezedAttributes = accContext.getSqueezedAttributes();
            
//                Map<QName, PrismValueDeltaSetTriple<ValueConstruction<?>>> tripleMap = accContext.getAttributeValueDeltaSetTripleMap();
            
            if (squeezedAttributes.isEmpty()) {
            	return;
            }

            RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(accContext.getResource(), prismContext);
            RefinedAccountDefinition accountDefinition = refinedSchema.getAccountDefinition(accContext.getResourceShadowDiscriminator().getIntent());
            
            reconcileAccount(accContext, squeezedAttributes, accountDefinition);
        } catch (RuntimeException e) {
        	subResult.recordFatalError(e);
        	throw e;
        } catch (SchemaException e) {
        	subResult.recordFatalError(e);
        	throw e;
		} finally {
            subResult.computeStatus();
        }
    }

    private void reconcileAccount(LensProjectionContext<AccountShadowType> accCtx,
            Map<QName, DeltaSetTriple<PropertyValueWithOrigin>> squeezedAttributes, RefinedAccountDefinition accountDefinition) throws SchemaException {

    	PrismObject<AccountShadowType> account = accCtx.getObjectNew();

        PrismContainer attributesContainer = account.findContainer(AccountShadowType.F_ATTRIBUTES);
        Collection<QName> attributeNames = MiscUtil.union(squeezedAttributes.keySet(),attributesContainer.getValue().getPropertyNames());

        for (QName attrName: attributeNames) {
        	//LOGGER.trace("Attribute reconciliation processing attribute {}",attrName);
        	RefinedAttributeDefinition attributeDefinition = accountDefinition.getAttributeDefinition(attrName);
        	if (attributeDefinition == null) {
        		throw new SchemaException("No definition for attribute "+attrName+" in "+accCtx.getResourceShadowDiscriminator());
        	}
        	
        	DeltaSetTriple<PropertyValueWithOrigin> pvwoTriple = squeezedAttributes.get(attrName);
        	Collection<PropertyValueWithOrigin> shouldBePValues = null;
        	if (pvwoTriple == null) {
        		shouldBePValues = new ArrayList<PropertyValueWithOrigin>();
        	} else {
        		shouldBePValues = pvwoTriple.getNonNegativeValues();
        	}
        	
        	boolean hasNonInitialShouldBePValue = false;
        	for (PropertyValueWithOrigin shouldBePValue: shouldBePValues) {
        		if (shouldBePValue.getMapping() != null && shouldBePValue.getMapping().getStrength() == MappingStrengthType.STRONG) {
        			hasNonInitialShouldBePValue = true;
        			break;
        		}
        	}
        	
        	PrismProperty attribute = attributesContainer.findProperty(attrName);
        	Collection<PrismPropertyValue<Object>> arePValues = null;
        	if (attribute != null) {
        		arePValues = attribute.getValues(Object.class);
        	} else {
        		arePValues = new HashSet<PrismPropertyValue<Object>>();
        	}
        	
        	// Too loud :-)
        	//LOGGER.trace("SHOULD BE:\n{}\nIS:\n{}",shouldBePValues,arePValues);
        	
        	for (PropertyValueWithOrigin shouldBePvwo: shouldBePValues) {
        		Mapping<?> shouldBeMapping = shouldBePvwo.getMapping();
        		if (shouldBeMapping == null) {
        			continue;
        		}
        		if (shouldBeMapping.getStrength() == MappingStrengthType.WEAK && (!arePValues.isEmpty() || hasNonInitialShouldBePValue)) {
        			// "initial" value and the attribute already has a value. Skip it.
        			continue;
        		}
        		Object shouldBeRealValue = shouldBePvwo.getPropertyValue().getValue();
        		if (!isInValues(shouldBeRealValue, arePValues)) {
        			recordDelta(accCtx, attributeDefinition, ChangeType.ADD, shouldBeRealValue, shouldBePvwo.getAccountConstruction().getSource());
        		}
        		
        	}
        	
        	if (!attributeDefinition.isTolerant()) {
        		for (PrismPropertyValue<Object> isPValue: arePValues) {
        			if (!isInPvwoValues(isPValue.getValue(), shouldBePValues)) {
        				recordDelta(accCtx, attributeDefinition, ChangeType.DELETE, isPValue.getValue(), null);
        			}
        		}
        	}
        }
    }

	private void recordDelta(LensProjectionContext<AccountShadowType> accCtx, ResourceAttributeDefinition attrDef, ChangeType changeType, Object value, ObjectType originObject) throws SchemaException {
		LOGGER.trace("Reconciliation will {} value of attribute {}: {}", new Object[]{changeType, attrDef, value});
		
		PropertyDelta attrDelta = new PropertyDelta(SchemaConstants.PATH_ATTRIBUTES, attrDef.getName(), attrDef);
		PrismPropertyValue<Object> pValue = new PrismPropertyValue<Object>(value, OriginType.RECONCILIATION, originObject);
		if (changeType == ChangeType.ADD) {
			attrDelta.addValueToAdd(pValue);
		} else if (changeType == ChangeType.DELETE) {
			attrDelta.addValueToDelete(pValue);
		} else {
			throw new IllegalArgumentException("Unknown change type "+changeType);
		}
		
		accCtx.addToSecondaryDelta(attrDelta);
	}

	private boolean isInValues(Object shouldBeValue, Collection<PrismPropertyValue<Object>> arePValues) {
		if (arePValues == null || arePValues.isEmpty()) {
			return false;
		}
		for (PrismPropertyValue<Object> isPValue: arePValues) {
			if (isPValue.getValue().equals(shouldBeValue)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isInPvwoValues(Object value, Collection<PropertyValueWithOrigin> shouldBePvwos) {
		for (PropertyValueWithOrigin shouldBePvwo: shouldBePvwos) {
			PrismPropertyValue<?> shouldBePPValue = shouldBePvwo.getPropertyValue();
			Object shouldBeValue = shouldBePPValue.getValue();
			if (shouldBeValue.equals(value)) {
				return true;
			}
    	}
		return false;
	}

}
