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

import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.schema.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * @author lazyman
 */
@Component
public class ReconciliationProcessor {

    public static final String PROCESS_RECONCILIATION = ReconciliationProcessor.class.getName() + ".processReconciliation";
    private static final Trace LOGGER = TraceManager.getTrace(ReconciliationProcessor.class);

    void processReconciliation(SyncContext context, OperationResult result) {
        OperationResult subResult = result.createSubresult(PROCESS_RECONCILIATION);

        try {
            for (AccountSyncContext accContext : context.getAccountContexts()) {
                if (!accContext.isDoReconciliation()) {
                    continue;
                }

                if (accContext.getAccountOld() == null) {
                    LOGGER.warn("Can't do reconciliation. Account context doesn't contain old version of account.");
                    return;
                }

                Map<QName, DeltaSetTriple<ValueConstruction>> map = accContext.getAttributeValueDeltaSetTripleMap();
                if (map.isEmpty()) {
                    return;
                }

                MidPointObject<AccountShadowType> oldAccount = accContext.getAccountOld();

                //todo implement this

            }
        } finally {
            subResult.computeStatus();
        }
    }
}
