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

import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.common.valueconstruction.ValueConstructionFactory;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.delta.PropertyDelta;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.DebugUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author semancik
 */
@Component
public class AssignmentProcessor {

    @Autowired(required = true)
    private RepositoryService repositoryService;

    @Autowired(required = true)
    private ObjectResolver objectResolver;

    @Autowired(required = true)
    private SchemaRegistry schemaRegistry;

    @Autowired(required = true)
    private ValueConstructionFactory valueConstructionFactory;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentProcessor.class);

    public void processAssignments(SyncContext context, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException {

        AccountSynchronizationSettingsType accountSynchronizationSettings = context.getAccountSynchronizationSettings();
        if (accountSynchronizationSettings != null) {
            AssignmentPolicyEnforcementType assignmentPolicyEnforcement = accountSynchronizationSettings.getAssignmentPolicyEnforcement();
            if (assignmentPolicyEnforcement == AssignmentPolicyEnforcementType.NONE) {
                // No assignment processing
                LOGGER.trace("Assignment enforcement policy set to NONE, skipping assignment processing");

                // But mark all accounts as assigned, so they will be synchronized as expected
                for (AccountSyncContext accCtx : context.getAccountContexts()) {
                    accCtx.setAssigned(true);
                }

                return;
            }
        }

        Collection<PropertyValue<AssignmentType>> assignmentsOld = new HashSet<PropertyValue<AssignmentType>>();
        if (context.getUserOld() != null) {
            Property assignmentProperty = context.getUserOld().findProperty(UserType.F_ASSIGNMENT);
            if (assignmentProperty != null) {
            	assignmentsOld.addAll(assignmentProperty.getValues(AssignmentType.class));
            }
        } else if (context.getUserTypeOld() != null) {
        	// TODO: legacy code, should be removed after complete switch to our objects
            List<AssignmentType> assignments = context.getUserTypeOld().getAssignment();
            for (AssignmentType assignment : assignments) {
                assignmentsOld.add(new PropertyValue<AssignmentType>(assignment));
            }
        }

        PropertyDelta assignmentDelta = context.getAssignmentDelta();

        LOGGER.trace("Assignment delta {}", assignmentDelta.dump());

        // TODO: preprocess assignment delta. If it is replace, then we need to convert it to: delete all existing assignments, add all new assignments
        Collection<PropertyValue<AssignmentType>> changedAssignments = assignmentDelta.getValues(AssignmentType.class);

        AssignmentEvaluator assignmentEvaluator = new AssignmentEvaluator();
        assignmentEvaluator.setRepository(repositoryService);
        assignmentEvaluator.setUser(context.getUserNew());
        assignmentEvaluator.setObjectResolver(objectResolver);
        assignmentEvaluator.setSchemaRegistry(schemaRegistry);
        assignmentEvaluator.setValueConstructionFactory(valueConstructionFactory);

        Map<ResourceAccountType, Collection<PropertyValue<AccountConstruction>>> zeroAccountMap = new HashMap<ResourceAccountType, Collection<PropertyValue<AccountConstruction>>>();
        Map<ResourceAccountType, Collection<PropertyValue<AccountConstruction>>> plusAccountMap = new HashMap<ResourceAccountType, Collection<PropertyValue<AccountConstruction>>>();
        Map<ResourceAccountType, Collection<PropertyValue<AccountConstruction>>> minusAccountMap = new HashMap<ResourceAccountType, Collection<PropertyValue<AccountConstruction>>>();

        LOGGER.trace("Old assignments {}", DebugUtil.prettyPrint(assignmentsOld));
        LOGGER.trace("Changed assignments {}", DebugUtil.prettyPrint(changedAssignments));

        ObjectType source = context.getUserTypeOld();
        if (source == null) {
            source = context.getUserNew().getOrParseObjectType();
        }

        Collection<AssignmentType> newAssignments = new HashSet<AssignmentType>();
        Collection<PropertyValue<AssignmentType>> allAssignments = MiscUtil.union(assignmentsOld, changedAssignments);
        for (PropertyValue<AssignmentType> propertyValue : allAssignments) {
            AssignmentType assignmentType = propertyValue.getValue();

            LOGGER.trace("Processing assignment {}", DebugUtil.prettyPrint(assignmentType));

            Assignment evaluatedAssignment = assignmentEvaluator.evaluate(assignmentType, source, result);

            if (assignmentsOld.contains(propertyValue)) {
                // TODO: remember old state
            }

            context.rememberResources(evaluatedAssignment.getResources(result));

            // Sort assignments to sets: unchanged (zero), added (plus), removed (minus)
            if (changedAssignments.contains(propertyValue)) {
                // There was some change

                if (assignmentDelta.isValueToAdd(propertyValue)) {
                    collectToAccountMap(plusAccountMap, evaluatedAssignment, result);
                }
                if (assignmentDelta.isValueToDelete(propertyValue)) {
                    collectToAccountMap(minusAccountMap, evaluatedAssignment, result);
                }

            } else {
                // No change in assignment
                collectToAccountMap(zeroAccountMap, evaluatedAssignment, result);
                newAssignments.add(assignmentType);
            }
        }

        if (LOGGER.isTraceEnabled()) {
            // Dump the maps
            LOGGER.trace("Account maps:\nZERO:\n{}\nPLUS:\n{}\nMINUS:\n{}\n", new Object[]{dumpAccountMap(zeroAccountMap),
                    dumpAccountMap(plusAccountMap), dumpAccountMap(minusAccountMap)});
        }

        Collection<ResourceAccountType> allAccountTypes = MiscUtil.union(zeroAccountMap.keySet(), plusAccountMap.keySet(), minusAccountMap.keySet());
        for (ResourceAccountType rat : allAccountTypes) {

            if (rat.getResourceOid() == null) {
                throw new IllegalStateException("Resource OID null in ResourceAccountType during assignment processing");
            }
            if (rat.getAccountType() == null) {
                throw new IllegalStateException("Account type is null in ResourceAccountType during assignment processing");
            }

            DeltaSetTriple<AccountConstruction> accountDeltaSetTriple = new DeltaSetTriple<AccountConstruction>(zeroAccountMap.get(rat),
                    plusAccountMap.get(rat), minusAccountMap.get(rat));

            Map<QName, DeltaSetTriple<ValueConstruction>> attributeValueDeltaMap = computeAttributeValueDeltaMap(accountDeltaSetTriple);
            LOGGER.trace("Account {}: accountDeltaSetTriple=\n{}", rat, accountDeltaSetTriple.dump());
            LOGGER.trace("Account {}: attributeValueDeltaMap=\n{}: ", rat, attributeValueDeltaMap);

            if (zeroAccountMap.containsKey(rat)) {
                AccountSyncContext accountSyncContext = context.getAccountSyncContext(rat);
                if (accountSyncContext == null) {
                	// The account should exist before the change but it does not
                	// This happens during reconciliation if there is an inconsistency. Pretend that the assignment was just added. That should do.
                	processAccountAssign(context, rat, accountDeltaSetTriple, attributeValueDeltaMap, result);
                    context.getAccountSyncContext(rat).setAssigned(true);
                } else {
                	// The account existed before the change and should still exist
	                accountSyncContext.setAssigned(true);
	                processAccountKeep(context, rat, accountDeltaSetTriple, attributeValueDeltaMap, result);
                }

            } else if (plusAccountMap.containsKey(rat) && minusAccountMap.containsKey(rat)) {
                context.getAccountSyncContext(rat).setAssigned(true);
                // Account was removed and added in the same operation, therefore keep its original state
                // TODO
                throw new UnsupportedOperationException("add+delete of account is not supported yet");
                //continue;

            } else if (plusAccountMap.containsKey(rat)) {
                // Account added
                processAccountAssign(context, rat, accountDeltaSetTriple, attributeValueDeltaMap, result);
                context.getAccountSyncContext(rat).setAssigned(true);

            } else if (minusAccountMap.containsKey(rat)) {
                context.getAccountSyncContext(rat).setAssigned(false);
                // Account removed
                processAccountUnassign(context, rat, accountDeltaSetTriple, attributeValueDeltaMap, result);

            } else {
                throw new IllegalStateException("Account " + rat + " went looney");
            }

            context.getAccountSyncContext(rat).addToAttributeValueDeltaSetTripleMap(attributeValueDeltaMap);

        }
    }

    private void collectToAccountMap(
            Map<ResourceAccountType, Collection<PropertyValue<AccountConstruction>>> accountMap,
            Assignment evaluatedAssignment, OperationResult result) throws ObjectNotFoundException, SchemaException {
        for (AccountConstruction accountConstruction : evaluatedAssignment.getAccountConstructions()) {
            String resourceOid = accountConstruction.getResource(result).getOid();
            String accountType = accountConstruction.getAccountType();
            ResourceAccountType rat = new ResourceAccountType(resourceOid, accountType);
            Collection<PropertyValue<AccountConstruction>> constructions = null;
            if (accountMap.containsKey(rat)) {
                constructions = accountMap.get(rat);
            } else {
                constructions = new HashSet<PropertyValue<AccountConstruction>>();
                accountMap.put(rat, constructions);
            }
            constructions.add(new PropertyValue<AccountConstruction>(accountConstruction));
        }
    }

    private String dumpAccountMap(Map<ResourceAccountType, Collection<PropertyValue<AccountConstruction>>> accountMap) {
        StringBuilder sb = new StringBuilder();
        Set<Entry<ResourceAccountType, Collection<PropertyValue<AccountConstruction>>>> entrySet = accountMap.entrySet();
        Iterator<Entry<ResourceAccountType, Collection<PropertyValue<AccountConstruction>>>> i = entrySet.iterator();
        while (i.hasNext()) {
            Entry<ResourceAccountType, Collection<PropertyValue<AccountConstruction>>> entry = i.next();
            sb.append(entry.getKey()).append(": ");
            sb.append(DebugUtil.prettyPrint(entry.getValue()));
            if (i.hasNext()) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private void processAccountAssign(SyncContext context, ResourceAccountType rat,
            DeltaSetTriple<AccountConstruction> accountDeltaSetTriple,
            Map<QName, DeltaSetTriple<ValueConstruction>> attributeValueDeltaMap,
            OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

        AccountSyncContext accountSyncContext = context.getAccountSyncContext(rat);
        if (accountSyncContext == null) {
            accountSyncContext = context.createAccountSyncContext(rat);
        }
        if (accountSyncContext.getPolicyDecision() == null) {
            accountSyncContext.setPolicyDecision(PolicyDecision.ADD);
        }

    }

    private void processAccountKeep(SyncContext context,
            ResourceAccountType rat, DeltaSetTriple<AccountConstruction> accountDeltaSetTriple,
            Map<QName, DeltaSetTriple<ValueConstruction>> attributeValueDeltaMap, OperationResult result) throws
            SchemaException {

        AccountSyncContext accountSyncContext = context.getAccountSyncContext(rat);
        if (accountSyncContext.getPolicyDecision() == null) {
            accountSyncContext.setPolicyDecision(PolicyDecision.KEEP);
        }

    }


    private void processAccountUnassign(SyncContext context, ResourceAccountType rat,
            DeltaSetTriple<AccountConstruction> accountDeltaSetTriple,
            Map<QName, DeltaSetTriple<ValueConstruction>> attributeValueDeltaMap, OperationResult result) {

        AccountSyncContext accountSyncContext = context.getAccountSyncContext(rat);
        if (accountSyncContext.getPolicyDecision() == null) {
            accountSyncContext.setPolicyDecision(PolicyDecision.DELETE);
        }

    }


    private Map<QName, DeltaSetTriple<ValueConstruction>> computeAttributeValueDeltaMap(
            DeltaSetTriple<AccountConstruction> accountDeltaSetTriple) {

        Map<QName, DeltaSetTriple<ValueConstruction>> attrMap = new HashMap<QName, DeltaSetTriple<ValueConstruction>>();

        for (PropertyValue<AccountConstruction> propertyValue : accountDeltaSetTriple.union()) {
            AccountConstruction ac = propertyValue.getValue();
            for (ValueConstruction attrConstr : ac.getAttributeConstructions()) {

                Property output = attrConstr.getOutput();
                QName attrName = output.getName();
                DeltaSetTriple<ValueConstruction> valueTriple = attrMap.get(attrName);
                if (valueTriple == null) {
                    valueTriple = new DeltaSetTriple<ValueConstruction>();
                    attrMap.put(attrName, valueTriple);
                }
                valueTriple.distributeAs(new PropertyValue<ValueConstruction>(attrConstr), accountDeltaSetTriple, propertyValue);

            }
        }
        return attrMap;
    }

}
