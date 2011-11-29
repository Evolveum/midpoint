/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.synchronizer;

import java.util.Map;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.common.valueconstruction.ValueConstructionFactory;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.DeltaSetTriple;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.ChangeType;
import com.evolveum.midpoint.schema.processor.ObjectDelta;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;

/**
 * @author semancik
 *
 */
@Component
public class OutboundProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(OutboundProcessor.class);
	
	@Autowired(required=true)
	private SchemaRegistry schemaRegistry;
	
	@Autowired(required=true)
	private ValueConstructionFactory valueConstructionFactory;

	void processOutbound(SyncContext context, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		
		for (AccountSyncContext accCtx: context.getAccountContexts()) {
			
			ResourceAccountType rat = accCtx.getResourceAccountType();
			ObjectDelta<AccountShadowType> accountDelta = accCtx.getAccountDelta();
			if (accountDelta.getChangeType() == ChangeType.DELETE) {
				LOGGER.trace("Processing outbound expressions for account {} skipped, no account delta", rat);
				// No point in evaluating outbound
				continue;
			}
			
			LOGGER.trace("Processing outbound expressions for account {} starting", rat);
			
			RefinedAccountDefinition rAccount = context.getRefinedAccountDefinition(rat, schemaRegistry);
			if (rAccount == null) {
				LOGGER.error("Definition for account type {} not found in the context, but it should be there, dumping context:\n{}",rat,context.dump());
				throw new IllegalStateException("Definition for account type "+rat+" not found in the context, but it should be there");
			}
			
			Map<QName, DeltaSetTriple<ValueConstruction>> attributeValueDeltaSetTripleMap = accCtx.getAttributeValueDeltaSetTripleMap();
			
			for (QName attributeName : rAccount.getNamesOfAttributesWithOutboundExpressions()) {
				DeltaSetTriple<ValueConstruction> attrDeltaTriple = attributeValueDeltaSetTripleMap.get(attributeName);
				RefinedAttributeDefinition refinedAttributeDefinition = rAccount.getAttributeDefinition(attributeName);
				
				// TODO: check access 
				
				ValueConstruction evaluatedOutboundAccountConstruction = evaluateOutboundAccountConstruction(context, refinedAttributeDefinition, rAccount, result);
				LOGGER.trace("Processing outbound expressions for account {}, attribute {}, result:\n{}", 
						new Object[]{rat, attributeName, evaluatedOutboundAccountConstruction == null ? null : evaluatedOutboundAccountConstruction.dump()});
				if (evaluatedOutboundAccountConstruction != null) {
					if (attrDeltaTriple == null) {
						attrDeltaTriple = new DeltaSetTriple<ValueConstruction>();
						attributeValueDeltaSetTripleMap.put(attributeName, attrDeltaTriple);
					}
					// TODO: zero set or plus set?
					attrDeltaTriple.getPlusSet().add(evaluatedOutboundAccountConstruction);
				}
			}
		
		}
		
	}
	
	private ValueConstruction evaluateOutboundAccountConstruction(SyncContext context, RefinedAttributeDefinition refinedAttributeDefinition, 
			RefinedAccountDefinition refinedAccountDefinition, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
		ValueConstructionType outboundValueConstructionType = refinedAttributeDefinition.getOutboundValueConstructionType();
		if (outboundValueConstructionType == null) {
			return null;
		}
		
		ValueConstruction valueConstruction = valueConstructionFactory.createValueConstruction(outboundValueConstructionType, refinedAttributeDefinition, 
				"outbound expression for "+refinedAttributeDefinition.getName()+" in "+ObjectTypeUtil.toShortString(refinedAccountDefinition.getResourceType()));
		
		// FIXME: should be userNew, but that is not yet filled in
		valueConstruction.addVariableDefinition(ExpressionConstants.VAR_USER, context.getUserOld());
		// TODO: variables
		
		valueConstruction.evaluate(result);
		
		return valueConstruction;
	}

}
