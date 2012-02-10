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
import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.common.valueconstruction.ValueConstructionFactory;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.SchemaRegistry;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
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
    private SchemaRegistry schemaRegistry;

    @Autowired(required = true)
    private ValueConstructionFactory valueConstructionFactory;

    void processOutbound(SyncContext context, OperationResult result) throws SchemaException,
            ExpressionEvaluationException, ObjectNotFoundException {

        for (AccountSyncContext accCtx : context.getAccountContexts()) {

            ResourceAccountType rat = accCtx.getResourceAccountType();
            ObjectDelta<AccountShadowType> accountDelta = accCtx.getAccountDelta();

            if (accountDelta != null && accountDelta.getChangeType() == ChangeType.DELETE) {
                LOGGER.trace("Processing outbound expressions for account {} skipped, DELETE account delta", rat);
                // No point in evaluating outbound
                continue;
            }

            LOGGER.trace("Processing outbound expressions for account {} starting", rat);

            RefinedAccountDefinition rAccount = context.getRefinedAccountDefinition(rat, schemaRegistry);
            if (rAccount == null) {
                LOGGER.error("Definition for account type {} not found in the context, but it should be there, dumping context:\n{}", rat, context.dump());
                throw new IllegalStateException("Definition for account type " + rat + " not found in the context, but it should be there");
            }

            Map<QName, DeltaSetTriple<ValueConstruction>> attributeValueDeltaSetTripleMap = accCtx.getAttributeValueDeltaSetTripleMap();

            for (QName attributeName : rAccount.getNamesOfAttributesWithOutboundExpressions()) {
                DeltaSetTriple<ValueConstruction> attrDeltaTriple = attributeValueDeltaSetTripleMap.get(attributeName);
                RefinedAttributeDefinition refinedAttributeDefinition = rAccount.getAttributeDefinition(attributeName);

                // TODO: check access

                PrismObject<UserType> user = context.getUserOld();
                if (user == null) {
                    user = context.getUserNew();
                }
                ValueConstruction evaluatedOutboundValueConstructionOld = evaluateOutboundValueConstruction(user, refinedAttributeDefinition, rAccount, result);
                ValueConstruction evaluatedOutboundValueConstructionNew = evaluateOutboundValueConstruction(context.getUserNew(), refinedAttributeDefinition, rAccount, result);

                LOGGER.trace("Processing outbound expressions for account {}, attribute {}\nOLD:\n{}\nNEW\n{}",
                        new Object[]{rat, attributeName,
                                evaluatedOutboundValueConstructionOld == null ? null : evaluatedOutboundValueConstructionOld.dump(),
                                evaluatedOutboundValueConstructionNew == null ? null : evaluatedOutboundValueConstructionNew.dump(),});

                if (evaluatedOutboundValueConstructionNew != null) {

                    if (attrDeltaTriple == null) {
                        attrDeltaTriple = new DeltaSetTriple<ValueConstruction>();
                        attributeValueDeltaSetTripleMap.put(attributeName, attrDeltaTriple);
                    }

                    if (accountDelta != null && accountDelta.getChangeType() == ChangeType.ADD) {
                        // Special behavior for add. In case the account is added we don't care how the expression output has
                        // changed. We will add all the values
                        attrDeltaTriple.getPlusSet().add(new PrismPropertyValue<ValueConstruction>(evaluatedOutboundValueConstructionNew));

                    } else {
                        // Diff new and old values, distributed the deltas accordingly

                        Collection<PrismPropertyValue<Object>> valuesOld = evaluatedOutboundValueConstructionOld.getOutput().getValues();
                        Collection<PrismPropertyValue<Object>> valuesNew = evaluatedOutboundValueConstructionNew.getOutput().getValues();
                        DeltaSetTriple<Object> valueDeltaTriple = DeltaSetTriple.diff(valuesOld, valuesNew);

                        // Clonning the value construction is necessary, as we need three of them
                        evaluatedOutboundValueConstructionNew.getOutput().getValues().clear();
                        ValueConstruction plusValueConstruction = evaluatedOutboundValueConstructionNew;
                        ValueConstruction minusValueConstruction = evaluatedOutboundValueConstructionNew.clone();
                        ValueConstruction zeroValueConstruction = evaluatedOutboundValueConstructionNew.clone();

                        plusValueConstruction.getOutput().getValues().addAll(valueDeltaTriple.getPlusSet());
                        attrDeltaTriple.getPlusSet().add(new PrismPropertyValue<ValueConstruction>(plusValueConstruction));

                        minusValueConstruction.getOutput().getValues().addAll(valueDeltaTriple.getMinusSet());
                        attrDeltaTriple.getMinusSet().add(new PrismPropertyValue<ValueConstruction>(minusValueConstruction));

                        zeroValueConstruction.getOutput().getValues().addAll(valueDeltaTriple.getZeroSet());
                        attrDeltaTriple.getZeroSet().add(new PrismPropertyValue<ValueConstruction>(zeroValueConstruction));
                    }

                }
            }

        }

    }

    private ValueConstruction evaluateOutboundValueConstruction(PrismObject<UserType> user,
            RefinedAttributeDefinition refinedAttributeDefinition,
            RefinedAccountDefinition refinedAccountDefinition, OperationResult result) throws
            ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

        ValueConstructionType outboundValueConstructionType = refinedAttributeDefinition.getOutboundValueConstructionType();
        if (outboundValueConstructionType == null) {
            return null;
        }

        // TODO: is the parentPath correct (null)?
        ValueConstruction valueConstruction = valueConstructionFactory.createValueConstruction(outboundValueConstructionType, 
        		refinedAttributeDefinition, null,
                "outbound expression for " + refinedAttributeDefinition.getName() + " in " + ObjectTypeUtil.toShortString(refinedAccountDefinition.getResourceType()));

        valueConstruction.addVariableDefinition(ExpressionConstants.VAR_USER, user);
        valueConstruction.setRootNode(user);
        // TODO: other variables?

        valueConstruction.evaluate(result);

        return valueConstruction;
    }

}
