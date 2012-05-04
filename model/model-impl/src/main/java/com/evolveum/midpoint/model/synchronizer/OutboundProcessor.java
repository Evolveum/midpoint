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
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.common.valueconstruction.ObjectDeltaObject;
import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.common.valueconstruction.ValueConstructionFactory;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Map;

/**
 * @author semancik
 */
@Component
public class OutboundProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(OutboundProcessor.class);

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private ValueConstructionFactory valueConstructionFactory;

    void processOutbound(SyncContext context, AccountSyncContext accCtx, OperationResult result) throws SchemaException,
            ExpressionEvaluationException, ObjectNotFoundException {

        ResourceAccountType rat = accCtx.getResourceAccountType();
        ObjectDelta<AccountShadowType> accountDelta = accCtx.getAccountDelta();

        if (accountDelta != null && accountDelta.getChangeType() == ChangeType.DELETE) {
            LOGGER.trace("Processing outbound expressions for account {} skipped, DELETE account delta", rat);
            // No point in evaluating outbound
            return;
        }

        LOGGER.trace("Processing outbound expressions for account {} starting", rat);

        RefinedAccountDefinition rAccount = context.getRefinedAccountDefinition(rat);
        if (rAccount == null) {
            LOGGER.error("Definition for account type {} not found in the context, but it should be there, dumping context:\n{}", rat, context.dump());
            throw new IllegalStateException("Definition for account type " + rat + " not found in the context, but it should be there");
        }
        
        ObjectDeltaObject<UserType> userOdo = context.getUserObjectDeltaObject();
        
        AccountConstruction outboundAccountConstruction = new AccountConstruction(null, accCtx.getResource());

        for (QName attributeName : rAccount.getNamesOfAttributesWithOutboundExpressions()) {
			RefinedAttributeDefinition refinedAttributeDefinition = rAccount.getAttributeDefinition(attributeName);
						
			ValueConstructionType outboundValueConstructionType = refinedAttributeDefinition.getOutboundValueConstructionType();
			if (outboundValueConstructionType == null) {
			    continue;
			}
			
			// TODO: check access
			
			ValueConstruction<? extends PrismPropertyValue<?>> valueConstruction = valueConstructionFactory.createValueConstruction(outboundValueConstructionType, 
					refinedAttributeDefinition,
			        "outbound expression for " + DebugUtil.prettyPrint(refinedAttributeDefinition.getName())
			        + " in " + ObjectTypeUtil.toShortString(rAccount.getResourceType()));
			
			valueConstruction.addVariableDefinition(ExpressionConstants.VAR_USER, userOdo);
			valueConstruction.addVariableDefinition(ExpressionConstants.VAR_ITERATION, accCtx.getIteration());
			valueConstruction.addVariableDefinition(ExpressionConstants.VAR_ITERATION_TOKEN, accCtx.getIterationToken());
			valueConstruction.setRootNode(userOdo);
			// TODO: other variables?
			
			// Set condition masks. There are used as a brakes to avoid evaluating to nonsense values in case user is not present
			// (e.g. in old values in ADD situations and new values in DELETE situations).
			if (userOdo.getOldObject() == null) {
				valueConstruction.setConditionMaskOld(false);
			}
			if (userOdo.getNewObject() == null) {
				valueConstruction.setConditionMaskNew(false);
			}
			
			valueConstruction.evaluate(result);
			
			outboundAccountConstruction.addAttributeConstruction(valueConstruction);
        }
        
        accCtx.setOutboundAccountConstruction(outboundAccountConstruction);
    }
}
