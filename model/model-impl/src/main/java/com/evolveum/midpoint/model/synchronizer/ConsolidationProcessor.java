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
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.delta.PropertyDelta;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * This processor consolidate delta set triples acquired from account sync context and transforms them to
 * property deltas. It considers also property deltas from sync, which already happened.
 *
 * @author lazyman
 */
@Component
public class ConsolidationProcessor {

    public static final String PROCESS_CONSOLIDATION = ConsolidationProcessor.class.getName() + ".consolidateValues";
    private static final Trace LOGGER = TraceManager.getTrace(ReconciliationProcessor.class);
    @Autowired(required = true)
    private SchemaRegistry schemaRegistry;

    /**
     * Converts delta set triples to a secondary account deltas.
     */
    void consolidateValues(SyncContext context, OperationResult result) throws SchemaException,
            ExpressionEvaluationException {
        //todo filter changes which were already in account sync delta

        for (AccountSyncContext accCtx : context.getAccountContexts()) {
            //account was deleted, no changes are needed.
            if (wasAccountDeleted(accCtx)) {
                dropAllAccountDelta(accCtx);
                continue;
            }

            PolicyDecision policyDecision = accCtx.getPolicyDecision();

            if (policyDecision == PolicyDecision.ADD) {
                consolidateValuesAddAccount(context, accCtx, result);
            } else if (policyDecision == PolicyDecision.KEEP) {
                consolidateValuesModifyAccount(context, accCtx, result);
            } else if (policyDecision == PolicyDecision.DELETE) {
                consolidateValuesDeleteAccount(context, accCtx, result);
            } else {
                // This is either UNLINK or null, both are in fact the same as KEEP
                consolidateValuesModifyAccount(context, accCtx, result);
            }
        }
    }

    private void dropAllAccountDelta(AccountSyncContext accContext) {
        accContext.setAccountPrimaryDelta(null);
        accContext.setAccountSecondaryDelta(null);
    }

    private boolean wasAccountDeleted(AccountSyncContext accContext) {
        ObjectDelta<AccountShadowType> delta = accContext.getAccountSyncDelta();
        if (delta != null && ChangeType.DELETE.equals(delta.getChangeType())) {
            return true;
        }

        return false;
    }

    private ObjectDelta<AccountShadowType> consolidateValuesToModifyDelta(SyncContext context,
            AccountSyncContext accCtx,
            boolean addUnchangedValues, OperationResult result) throws SchemaException, ExpressionEvaluationException {

        Map<QName, DeltaSetTriple<ValueConstruction>> attributeValueDeltaMap = accCtx.getAttributeValueDeltaSetTripleMap();
        ResourceAccountType rat = accCtx.getResourceAccountType();
        ObjectDelta<AccountShadowType> objectDelta = new ObjectDelta<AccountShadowType>(AccountShadowType.class, ChangeType.MODIFY);
        objectDelta.setOid(accCtx.getOid());

        RefinedAccountDefinition rAccount = context.getRefinedAccountDefinition(rat, schemaRegistry);
        if (rAccount == null) {
            LOGGER.error("Definition for account type {} not found in the context, but it should be there, dumping context:\n{}", rat, context.dump());
            throw new IllegalStateException("Definition for account type " + rat + " not found in the context, but it should be there");
        }

        PropertyPath parentPath = new PropertyPath(SchemaConstants.I_ATTRIBUTES);

        for (Map.Entry<QName, DeltaSetTriple<ValueConstruction>> entry : attributeValueDeltaMap.entrySet()) {
            QName attributeName = entry.getKey();
            DeltaSetTriple<ValueConstruction> triple = entry.getValue();

            PropertyDelta propDelta = null;

            LOGGER.trace("Consolidating (modify) account {}, attribute {}", rat, attributeName);

            PropertyContainer attributesPropertyContainer = null;
            if (accCtx.getAccountNew() != null) {
                attributesPropertyContainer = accCtx.getAccountNew().findPropertyContainer(SchemaConstants.I_ATTRIBUTES);
            }

            Collection<PropertyValue<Object>> allValues = collectAllValues(triple);
            for (PropertyValue<Object> value : allValues) {
                Collection<PropertyValue<ValueConstruction>> zeroConstructions =
                        collectValueConstructionsFromSet(value, triple.getZeroSet());
                if (!zeroConstructions.isEmpty() && !addUnchangedValues) {
                    // Value unchanged, nothing to do
                    LOGGER.trace("Value {} unchanged, doing nothing", value);
                    continue;
                }
                Collection<PropertyValue<ValueConstruction>> plusConstructions =
                        collectValueConstructionsFromSet(value, triple.getPlusSet());
                Collection<PropertyValue<ValueConstruction>> minusConstructions =
                        collectValueConstructionsFromSet(value, triple.getMinusSet());
                if (!plusConstructions.isEmpty() && !minusConstructions.isEmpty()) {
                    // Value added and removed. Ergo no change.
                    LOGGER.trace("Value {} added and removed, doing nothing", value);
                    continue;
                }
                if (propDelta == null) {
                    propDelta = new PropertyDelta(parentPath, attributeName);
                }

                boolean initialOnly = true;
                ValueConstruction exclusiveVc = null;
                Collection<PropertyValue<ValueConstruction>> constructionsToAdd = null;
                if (addUnchangedValues) {
                    constructionsToAdd = MiscUtil.union(zeroConstructions, plusConstructions);
                } else {
                    constructionsToAdd = plusConstructions;
                }

                if (!constructionsToAdd.isEmpty()) {
                    for (PropertyValue<ValueConstruction> propertyValue : constructionsToAdd) {
                        ValueConstruction vc = propertyValue.getValue();
                        if (!vc.isInitial()) {
                            initialOnly = false;
                        }
                        if (vc.isExclusive()) {
                            if (exclusiveVc == null) {
                                exclusiveVc = vc;
                            } else {
                                String message = "Exclusion conflict in account " + rat + ", attribute " + attributeName +
                                        ", conflicting constructions: " + exclusiveVc + " and " + vc;
                                LOGGER.error(message);
                                throw new ExpressionEvaluationException(message);
                            }
                        }
                    }
                    if (initialOnly) {
                        if (attributesPropertyContainer != null) {
                            Property attributeNew = attributesPropertyContainer.findProperty(attributeName);
                            if (attributeNew != null && !attributeNew.isEmpty()) {
                                // There is already a value, skip this
                                LOGGER.trace("Value {} is initial and the attribute already has a value, skipping it", value);
                                continue;
                            }
                        }
                    }
                    LOGGER.trace("Value {} added", value);
                    propDelta.addValueToAdd(value);
                }

                if (!minusConstructions.isEmpty()) {
                    LOGGER.trace("Value {} deleted", value);
                    propDelta.addValueToDelete(value);
                }
            }

            propDelta = consolidateWithSync(accCtx, propDelta);

            if (propDelta != null) {
                objectDelta.addModification(propDelta);
            }
        }

        return objectDelta;
    }

    private void consolidateValuesAddAccount(SyncContext context, AccountSyncContext accCtx,
            OperationResult result) throws SchemaException, ExpressionEvaluationException {

        ObjectDelta<AccountShadowType> modifyDelta = consolidateValuesToModifyDelta(context, accCtx, true, result);
        ObjectDelta<AccountShadowType> accountSecondaryDelta = accCtx.getAccountSecondaryDelta();
        if (accountSecondaryDelta != null) {
            accountSecondaryDelta.merge(modifyDelta);
        } else {
            if (accCtx.getAccountPrimaryDelta() == null) {
                ObjectDelta<AccountShadowType> addDelta = new ObjectDelta<AccountShadowType>(AccountShadowType.class, ChangeType.ADD);
                RefinedAccountDefinition rAccount = context.getRefinedAccountDefinition(accCtx.getResourceAccountType(), schemaRegistry);

                if (rAccount == null) {
                    LOGGER.error("Definition for account type {} not found in the context, but it should be there, dumping context:\n{}", accCtx.getResourceAccountType(), context.dump());
                    throw new IllegalStateException("Definition for account type " + accCtx.getResourceAccountType() + " not found in the context, but it should be there");
                }
                AccountShadowType newAccountType = rAccount.createBlankShadow();
                ObjectDefinition<AccountShadowType> accountTypeDefinition = rAccount.getObjectDefinition();
                MidPointObject<AccountShadowType> newAccount = accountTypeDefinition.parseObjectType(newAccountType);
                addDelta.setObjectToAdd(newAccount);

                addDelta.merge(modifyDelta);
                accCtx.setAccountSecondaryDelta(addDelta);
            } else {
                accCtx.setAccountSecondaryDelta(modifyDelta);
            }
        }
    }

    private void consolidateValuesModifyAccount(SyncContext context, AccountSyncContext accCtx,
            OperationResult result) throws SchemaException, ExpressionEvaluationException {

        ObjectDelta<AccountShadowType> modifyDelta = consolidateValuesToModifyDelta(context, accCtx, false, result);
        ObjectDelta<AccountShadowType> accountSecondaryDelta = accCtx.getAccountSecondaryDelta();
        if (accountSecondaryDelta != null) {
            if (!modifyDelta.isEmpty()) {
                accountSecondaryDelta.merge(modifyDelta);
            }
        } else {
            accCtx.setAccountSecondaryDelta(modifyDelta);
        }
    }

    private void consolidateValuesDeleteAccount(SyncContext context, AccountSyncContext accCtx,
            OperationResult result) {
        ObjectDelta<AccountShadowType> deleteDelta = new ObjectDelta<AccountShadowType>(AccountShadowType.class, ChangeType.DELETE);
        deleteDelta.setOid(accCtx.getOid());
        accCtx.setAccountSecondaryDelta(deleteDelta);
    }

    /**
     * This method checks {@link com.evolveum.midpoint.schema.delta.PropertyDelta} created during consolidation with
     * account sync deltas. If changes from property delta are in account sync deltas than they must be removed,
     * because they already had been applied (they came from sync, already happened).
     *
     * @param accCtx current account sync context
     * @param delta  new delta created during consolidation process
     * @return method return updated delta, or null if delta was empty after filtering (removing unnecessary values).
     */
    private PropertyDelta consolidateWithSync(AccountSyncContext accCtx, PropertyDelta delta) {
        if (delta == null) {
            return null;
        }

        ObjectDelta<AccountShadowType> syncDelta = accCtx.getAccountSyncDelta();
        if (syncDelta == null) {
            return consolidateWithSyncAbsolute(accCtx, delta);
        }

        PropertyDelta alreadyDoneDelta = syncDelta.getPropertyDelta(delta.getPath());
        if (alreadyDoneDelta == null) {
            return delta;
        }

        cleanupValues(delta.getValuesToAdd(), alreadyDoneDelta);
        cleanupValues(delta.getValuesToDelete(), alreadyDoneDelta);

        if (delta.getValues(Object.class).isEmpty()) {
            return null;
        }

        return delta;
    }

    /**
     * This method consolidate property delta against account absolute state which came from sync (not as delta)
     *
     * @param accCtx
     * @param delta
     * @return method return updated delta, or null if delta was empty after filtering (removing unnecessary values).
     */
    private PropertyDelta consolidateWithSyncAbsolute(AccountSyncContext accCtx, PropertyDelta delta) {
        if (delta == null || accCtx.getAccountOld() == null) {
            return delta;
        }

        MidPointObject<AccountShadowType> absoluteAccountState = accCtx.getAccountOld();
        Property absoluteProperty = absoluteAccountState.findProperty(delta.getPath());
        if (absoluteProperty == null) {
            return delta;
        }

        cleanupAbsoluteValues(delta.getValuesToAdd(), true, absoluteProperty);
        cleanupAbsoluteValues(delta.getValuesToDelete(), false, absoluteProperty);

        if (delta.getValues(Object.class).isEmpty()) {
            return null;
        }

        return delta;
    }

    /**
     * Method removes values from property delta values list (first parameter).
     *
     * @param values   collection with {@link PropertyValue} objects to add or delete (from {@link PropertyDelta}
     * @param adding   if true we removing {@link PropertyValue} from {@link Collection} values parameter if they
     *                 already are in {@link Property} parameter. Otherwise we're removing {@link PropertyValue}
     *                 from {@link Collection} values parameter if they already are not in {@link Property} parameter.
     * @param property property with absolute state
     */
    private void cleanupAbsoluteValues(Collection<PropertyValue<Object>> values, boolean adding, Property property) {
        if (values == null) {
            return;
        }

        Iterator<PropertyValue<Object>> iterator = values.iterator();
        while (iterator.hasNext()) {
            PropertyValue<Object> value = iterator.next();
            if (adding && property.hasRealValue(value)) {
                iterator.remove();
            }

            if (!adding && !property.hasRealValue(value)) {
                iterator.remove();
            }
        }
    }

    /**
     * Simple util method which checks property values against already done delta from sync. See method
     * {@link ConsolidationProcessor#consolidateWithSync(com.evolveum.midpoint.model.AccountSyncContext,
     * com.evolveum.midpoint.schema.delta.PropertyDelta)}.
     *
     * @param values           collection which has to be filtered
     * @param alreadyDoneDelta already applied delta from sync
     */
    private void cleanupValues(Collection<PropertyValue<Object>> values, PropertyDelta alreadyDoneDelta) {
        if (values == null) {
            return;
        }

        Iterator<PropertyValue<Object>> iterator = values.iterator();
        while (iterator.hasNext()) {
            PropertyValue<Object> valueToAdd = iterator.next();
            if (alreadyDoneDelta.isRealValueToAdd(valueToAdd)) {
                iterator.remove();
            }
        }
    }

    private Collection<PropertyValue<Object>> collectAllValues(DeltaSetTriple<ValueConstruction> triple) {
        Collection<PropertyValue<Object>> allValues = new HashSet<PropertyValue<Object>>();
        collectAllValuesFromSet(allValues, triple.getZeroSet());
        collectAllValuesFromSet(allValues, triple.getPlusSet());
        collectAllValuesFromSet(allValues, triple.getMinusSet());
        return allValues;
    }

    private void collectAllValuesFromSet(Collection<PropertyValue<Object>> allValues,
            Collection<PropertyValue<ValueConstruction>> set) {
        if (set == null) {
            return;
        }
        for (PropertyValue<ValueConstruction> valConstr : set) {
            collectAllValuesFromValueConstruction(allValues, valConstr);
        }
    }

    private void collectAllValuesFromValueConstruction(Collection<PropertyValue<Object>> allValues,
            PropertyValue<ValueConstruction> valConstr) {
        Property output = valConstr.getValue().getOutput();
        if (output == null) {
            return;
        }
        allValues.addAll(output.getValues());
    }

    private boolean isValueInSet(Object value, Collection<PropertyValue<ValueConstruction>> set) {
        // Stupid implementation, but easy to write. TODO: optimize
        Collection<PropertyValue<Object>> allValues = new HashSet<PropertyValue<Object>>();
        collectAllValuesFromSet(allValues, set);
        return allValues.contains(value);
    }

    private Collection<PropertyValue<ValueConstruction>> collectValueConstructionsFromSet(Object value,
            Collection<PropertyValue<ValueConstruction>> set) {
        Collection<PropertyValue<ValueConstruction>> contructions = new HashSet<PropertyValue<ValueConstruction>>();
        for (PropertyValue<ValueConstruction> valConstr : set) {
            Property output = valConstr.getValue().getOutput();
            if (output == null) {
                continue;
            }
            if (output.getValues().contains(value)) {
                contructions.add(valConstr);
            }
        }
        return contructions;
    }
}
