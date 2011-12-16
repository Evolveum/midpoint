/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.model.synchronizer;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.controller.Filter;
import com.evolveum.midpoint.model.controller.FilterManager;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author lazyman
 */
@Component
public class InboundProcessor {

    public static final String PROCESS_INBOUND_HANDLING = InboundProcessor.class.getName() + ".processInbound";
    private static final Trace LOGGER = TraceManager.getTrace(InboundProcessor.class);

    @Autowired(required = true)
    private SchemaRegistry schemaRegistry;

    @Autowired(required = true)
    private FilterManager<Filter> filterManager;

    void processInbound(SyncContext context, OperationResult result) throws SchemaException {
        OperationResult subResult = result.createSubresult(PROCESS_INBOUND_HANDLING);

        try {
            for (AccountSyncContext accountContext : context.getAccountContexts()) {
                ResourceAccountType rat = accountContext.getResourceAccountType();
                LOGGER.trace("Processing inbound expressions for account {} starting", rat);

                RefinedAccountDefinition accountDefinition = context.getRefinedAccountDefinition(rat, schemaRegistry);
                if (accountDefinition == null) {
                    LOGGER.error("Definition for account type {} not found in the context, but it should be there, dumping context:\n{}", rat, context.dump());
                    throw new IllegalStateException("Definition for account type " + rat + " not found in the context, but it should be there");
                }

//                ObjectDelta<AccountShadowType> accountDelta = accountContext.getAccountDelta();
//                for (QName name : accountDefinition.getNamesOfAttributesWithInboundExpressions()) {
//                    PropertyDelta propertyDelta = accountDelta.getPropertyDelta(name);
//                    System.out.println(propertyDelta);
//                    RefinedAttributeDefinition attrDef = accountDefinition.getAttributeDefinition(name);
//                }
            }
        } finally {
            subResult.computeStatus();
        }
    }
}
