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
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.util.Utils;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.delta.PropertyDelta;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author semancik
 */
@Component
public class UserSynchronizer {

    private static final Trace LOGGER = TraceManager.getTrace(UserSynchronizer.class);

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @Autowired(required = true)
    private ProvisioningService provisioningService;

    @Autowired(required = true)
    private UserPolicyProcessor userPolicyProcessor;

    @Autowired(required = true)
    private AssignmentProcessor assignmentProcessor;

    @Autowired(required = true)
    private InboundProcessor inboundProcessor;

    @Autowired(required = true)
    private OutboundProcessor outboundProcessor;

    @Autowired(required = true)
    private ReconciliationProcessor reconciliationProcessor;

    @Autowired(required = true)
    private CredentialsProcessor credentialsProcessor;

    @Autowired(required = true)
    private ActivationProcessor activationProcessor;

    @Autowired(required = true)
    private SchemaRegistry schemaRegistry;

    public void synchronizeUser(SyncContext context, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {

        loadUser(context, result);
        loadFromSystemConfig(context, result);
        context.recomputeUserNew();

        loadAccountRefs(context, result);
        context.recomputeUserNew();

        // Check reconcile flag in account sync context and set accountOld
        // variable if it's not set (from provisioning)
        checkAccountContextReconciliation(context, result);

        traceContext("Context after LOAD and recompute:\n{}", context);

        // Loop through the account changes, apply inbound expressions
        inboundProcessor.processInbound(context, result);
        context.recomputeUserNew();
        traceContext("Context after INBOUND and recompute:\n{}", context);

        userPolicyProcessor.processUserPolicy(context, result);
        context.recomputeUserNew();
        traceContext("Context after USER POLICY and recompute:\n{}", context);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("User delta:\n{}", context.getUserDelta().dump());
        }

        assignmentProcessor.processAssignments(context, result);
        context.recomputeNew();
        traceContext("Context after ASSIGNMENTS and recompute:\n{}", context);

        outboundProcessor.processOutbound(context, result);
        context.recomputeNew();
        traceContext("Context after OUTBOUND and recompute:\n{}", context);

        reconciliationProcessor.processReconciliation(context, result);
        context.recomputeNew();
        traceContext("Context after RECONCILIATION and recompute:\n{}", context);

        consolidateValues(context, result);
        context.recomputeNew();
        traceContext("Context after CONSOLIDATION and recompute:\n{}", context);

        credentialsProcessor.processCredentials(context, result);
        context.recomputeNew();
        traceContext("Context after CREDENTIALS and recompute:\n{}", context);

        activationProcessor.processActivation(context, result);
        context.recomputeNew();
        traceContext("Context after ACTIVATION and recompute:\n{}", context);
    }

    private void checkAccountContextReconciliation(SyncContext context, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException {

        OperationResult subResult = result.createSubresult(UserSynchronizer.class + ".checkAccountContextReconciliation");
        try {
            for (AccountSyncContext accContext : context.getAccountContexts()) {
                if (!accContext.isDoReconciliation() || accContext.getAccountOld() != null) {
                    continue;
                }

                AccountShadowType account = provisioningService.getObject(AccountShadowType.class, accContext.getOid(),
                        null, subResult);
                ResourceType resource = Utils.getResource(account, provisioningService, result);
                ObjectDefinition<AccountShadowType> definition = RefinedResourceSchema.getRefinedSchema(
                        resource, schemaRegistry).getObjectDefinition(account);

                MidPointObject<AccountShadowType> object = definition.instantiate(SchemaConstants.I_ACCOUNT_SHADOW_TYPE);
                object.setOid(account.getOid());
                object.setObjectType(account);
                accContext.setAccountOld(object);
            }
        } finally {
            subResult.computeStatus();
        }
    }

    private void traceContext(String message, SyncContext context) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(message, context.dump());
        }
    }

    private void loadUser(SyncContext context, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (context.getUserOld() != null) {
            // already loaded
            return;
        }
        ObjectDelta<UserType> userPrimaryDelta = context.getUserPrimaryDelta();
        if (userPrimaryDelta == null) {
            // no change to user
            // TODO: where to get OID from????
            throw new UnsupportedOperationException("TODO");
        }
        if (userPrimaryDelta.getChangeType() == ChangeType.ADD) {
            // null oldUser is OK
            return;
        }
        String userOid = userPrimaryDelta.getOid();

        UserType userType = cacheRepositoryService.getObject(UserType.class, userOid, null, result);
        context.setUserTypeOld(userType);
        Schema commonSchema = schemaRegistry.getObjectSchema();
        MidPointObject<UserType> user = commonSchema.parseObjectType(userType);
        context.setUserOld(user);
    }

    private void loadAccountRefs(SyncContext context, OperationResult result) throws ObjectNotFoundException,
            SchemaException, CommunicationException {
        PolicyDecision policyDecision = null;
        if (context.getUserPrimaryDelta() != null && context.getUserPrimaryDelta().getChangeType() == ChangeType.DELETE) {
            // If user is deleted, all accounts should also be deleted
            policyDecision = PolicyDecision.DELETE;
        }

        MidPointObject<UserType> userNew = context.getUserNew();
        if (userNew != null) {
            UserType userTypeNew = userNew.getOrParseObjectType();
            loadAccountRefsFromUser(context, userTypeNew, policyDecision, result);
        }

        UserType userTypeOld = context.getUserTypeOld();
        if (userTypeOld != null) {
            // Accounts that are not in userNew but are in userOld are to be unlinked
            if (policyDecision == null) {
                policyDecision = PolicyDecision.UNLINK;
            }
            loadAccountRefsFromUser(context, userTypeOld, policyDecision, result);
        }

    }

    /**
     * Does not overwrite existing account contexts, just adds new ones.
     */
    private void loadAccountRefsFromUser(SyncContext context, UserType userType, PolicyDecision policyDecision,
            OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException {
        for (ObjectReferenceType accountRef : userType.getAccountRef()) {
            String oid = accountRef.getOid();
            // Fetching from repository instead of provisioning so we avoid reading in a full account
            AccountShadowType accountType = cacheRepositoryService.getObject(AccountShadowType.class, oid, null, result);
            String resourceOid = ResourceObjectShadowUtil.getResourceOid(accountType);
            ResourceAccountType rat = new ResourceAccountType(resourceOid, accountType.getAccountType());
            AccountSyncContext accountSyncContext = context.getAccountSyncContext(rat);
            if (accountSyncContext == null) {
                ResourceType resource = context.getResource(rat);
                if (resource == null) {
                    // Fetching from provisioning to take advantage of caching and pre-parsed schema
                    resource = provisioningService.getObject(ResourceType.class, resourceOid, null, result);
                    context.rememberResource(resource);
                }
                accountSyncContext = context.createAccountSyncContext(rat);
                if (accountSyncContext.getPolicyDecision() == null) {
                    accountSyncContext.setPolicyDecision(policyDecision);
                }
            }
            accountSyncContext.setOid(oid);
        }
    }

    private void loadFromSystemConfig(SyncContext context, OperationResult result) throws ObjectNotFoundException,
            SchemaException {
        SystemConfigurationType systemConfigurationType = cacheRepositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, result);
        if (systemConfigurationType == null) {
            // throw new SystemException("System configuration object is null (should not happen!)");
            // This should not happen, but it happens in tests. And it is a convenient short cut. Tolerate it for now.
            LOGGER.warn("System configuration object is null (should not happen!)");
            return;
        }

        if (context.getUserTemplate() == null) {
            UserTemplateType defaultUserTemplate = systemConfigurationType.getDefaultUserTemplate();
            context.setUserTemplate(defaultUserTemplate);
        }

        if (context.getAccountSynchronizationSettings() == null) {
            AccountSynchronizationSettingsType globalAccountSynchronizationSettings = systemConfigurationType.getGlobalAccountSynchronizationSettings();
            LOGGER.trace("Applying globalAccountSynchronizationSettings to context: {}", globalAccountSynchronizationSettings);
            context.setAccountSynchronizationSettings(globalAccountSynchronizationSettings);
        }
    }


    /**
     * Converts delta set triples to a secondary account deltas.
     */
    private void consolidateValues(SyncContext context, OperationResult result) throws SchemaException,
            ExpressionEvaluationException {
        //todo filter changes which were already in account sync delta

        for (AccountSyncContext accCtx : context.getAccountContexts()) {

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
            accountSecondaryDelta.merge(modifyDelta);
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

        for (Entry<QName, DeltaSetTriple<ValueConstruction>> entry : attributeValueDeltaMap.entrySet()) {
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

            if (propDelta != null) {
                objectDelta.addModification(propDelta);
            }

        }

        return objectDelta;
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
