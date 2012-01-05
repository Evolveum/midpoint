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
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.delta.PropertyDelta;
import com.evolveum.midpoint.schema.processor.ChangeType;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author lazyman
 */
@Component
public class ReconciliationProcessor {

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
     */
    void processReconciliation(SyncContext context, OperationResult result) {
        //todo remove this if clause
        if (1 == 1) {
            return;
        }
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
                ObjectDelta<AccountShadowType> delta1 = compareObjectZero(oldAccount, map);
            }
        } finally {
            subResult.computeStatus();
        }
    }

    private ObjectDelta<AccountShadowType> compareObjectZero(MidPointObject<AccountShadowType> account,
            Map<QName, DeltaSetTriple<ValueConstruction>> triples) {

        ObjectDelta<AccountShadowType> delta = new ObjectDelta<AccountShadowType>(AccountShadowType.class, ChangeType.MODIFY);

        Set<Map.Entry<QName, DeltaSetTriple<ValueConstruction>>> usedTriples =
                new HashSet<Map.Entry<QName, DeltaSetTriple<ValueConstruction>>>();

        PropertyDelta propertyDelta;
        Set<Property> properties = account.getProperties();
        for (Property property : properties) {
            DeltaSetTriple<ValueConstruction> triple = triples.get(property.getName());
            if (triple != null) {
                Collection<PropertyValue<ValueConstruction>> zeroSet = triple.getZeroSet();
                for (PropertyValue<ValueConstruction> zero : zeroSet) {
                    Property zeroProperty = zero.getValue().getOutput();
                    if (zeroProperty == null) {
                        //zero property is null, so property should be empty, or we have to create property delta delete
                        //todo implement
                        continue;
                    }

                    propertyDelta = zeroProperty.compareRealValuesTo(property);
                }
            } else {
                //property in account should not be there (it's not in triples)
                //todo do something here
            }

        }

        return null;
    }
}
