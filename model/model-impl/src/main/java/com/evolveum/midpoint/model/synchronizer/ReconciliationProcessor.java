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

package com.evolveum.midpoint.model.synchronizer;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.SchemaRegistry;
import com.evolveum.midpoint.prism.SourceType;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
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
	SchemaRegistry schemaRegistry;
	
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
    void processReconciliation(SyncContext context, OperationResult result) throws SchemaException {
        OperationResult subResult = result.createSubresult(PROCESS_RECONCILIATION);

        try {
            for (AccountSyncContext accContext : context.getAccountContexts()) {
                if (!accContext.isDoReconciliation()) {
                    continue;
                }

                if (accContext.getAccountOld() == null) {
                    LOGGER.warn("Can't do reconciliation. Account context doesn't contain old version of account.");
                    continue;
                }
                
                LOGGER.trace("Attribute reconciliation processing ACCOUNT {}",accContext.getResourceAccountType());

                Map<QName, DeltaSetTriple<ValueConstruction>> tripleMap = accContext.getAttributeValueDeltaSetTripleMap();
                if (tripleMap.isEmpty()) {
                	continue;
                }

                RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(accContext.getResource(), schemaRegistry);
                RefinedAccountDefinition accountDefinition = refinedSchema.getAccountDefinition(accContext.getResourceAccountType().getAccountType());
                
                reconcileAccount(accContext, tripleMap, accountDefinition);
            }
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

    private void reconcileAccount(AccountSyncContext accCtx,
            Map<QName, DeltaSetTriple<ValueConstruction>> tripleMap, RefinedAccountDefinition accountDefinition) {

    	PrismObject<AccountShadowType> account = accCtx.getAccountNew();

        PrismContainer attributesContainer = account.findPropertyContainer(AccountShadowType.F_ATTRIBUTES);
        Collection<QName> attributeNames = MiscUtil.union(tripleMap.keySet(),attributesContainer.getPropertyNames());

        for (QName attrName: attributeNames) {
        	//LOGGER.trace("Attribute reconciliation processing attribute {}",attrName);
        	RefinedAttributeDefinition attributeDefinition = accountDefinition.getAttributeDefinition(attrName);
        	
        	DeltaSetTriple<ValueConstruction> triple = tripleMap.get(attrName);
        	Collection<PrismPropertyValue<ValueConstruction>> shouldBePValues = null;
        	if (triple == null) {
        		shouldBePValues = new HashSet<PrismPropertyValue<ValueConstruction>>();
        	} else {
        		shouldBePValues = triple.getNonNegativeValues();
        	}
        	
        	PrismProperty attribute = attributesContainer.findProperty(attrName);
        	Set<PrismPropertyValue<Object>> arePValues = null;
        	if (attribute != null) {
        		arePValues = attribute.getValues(Object.class);
        	} else {
        		arePValues = new HashSet<PrismPropertyValue<Object>>();
        	}
        	
        	// Too loud :-)
        	//LOGGER.trace("SHOULD BE:\n{}\nIS:\n{}",shouldBePValues,arePValues);
        	
        	for (PrismPropertyValue<ValueConstruction> shouldBePValue: shouldBePValues) {
        		ValueConstruction shouldBeVc = shouldBePValue.getValue();
        		if (shouldBeVc == null) {
        			continue;
        		}
        		if (shouldBeVc.isInitial() && !arePValues.isEmpty()) {
        			// "initial" value and the attribute already has a value. Skip it.
        			continue;
        		}
        		PrismProperty shoudlBeProperty = shouldBeVc.getOutput();
        		for (PrismPropertyValue<Object> shouldBePPValue: shoudlBeProperty.getValues()) {
        			Object shouldBeValue = shouldBePPValue.getValue();
        			// Make sure this value is in the values
        			if (!isInValues(shouldBeValue, arePValues)) {
        				recordDelta(accCtx, attrName, ChangeType.ADD, shouldBeValue);
        			}
        		}
        	}
        	
        	if (!attributeDefinition.isTolerant()) {
        		for (PrismPropertyValue<Object> isPValue: arePValues) {
        			if (!isInValues(isPValue.getValue(), shouldBePValues)) {
        				recordDelta(accCtx, attrName, ChangeType.DELETE, isPValue.getValue());
        			}
        		}
        	}
        }
    }

	private void recordDelta(AccountSyncContext accCtx, QName attrName, ChangeType changeType, Object value) {
		LOGGER.trace("Reconciliation will {} value of attribute {}: {}", new Object[]{changeType, attrName, value});
		
		PropertyDelta attrDelta = new PropertyDelta(SchemaConstants.PATH_ATTRIBUTES, attrName);
		PrismPropertyValue<Object> pValue = new PrismPropertyValue<Object>(value, SourceType.RECONCILIATION, null);
		if (changeType == ChangeType.ADD) {
			attrDelta.addValueToAdd(pValue);
		} else if (changeType == ChangeType.DELETE) {
			attrDelta.addValueToDelete(pValue);
		} else {
			throw new IllegalArgumentException("Unknown change type "+changeType);
		}
		
		accCtx.addToSecondaryDelta(attrDelta);
	}

	private boolean isInValues(Object shouldBeValue, Set<PrismPropertyValue<Object>> arePValues) {
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
	
	private boolean isInValues(Object value, Collection<PrismPropertyValue<ValueConstruction>> shouldBePValues) {
		for (PrismPropertyValue<ValueConstruction> shouldBePValue: shouldBePValues) {
    		ValueConstruction shouldBeVc = shouldBePValue.getValue();
    		PrismProperty shoudlBeProperty = shouldBeVc.getOutput();
    		for (PrismPropertyValue<Object> shouldBePPValue: shoudlBeProperty.getValues()) {
    			Object shouldBeValue = shouldBePPValue.getValue();
    			if (shouldBeValue.equals(value)) {
    				return true;
    			}
    		}
    	}
		return false;
	}

}
