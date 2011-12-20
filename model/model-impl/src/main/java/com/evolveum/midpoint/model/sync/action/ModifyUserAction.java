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

package com.evolveum.midpoint.model.sync.action;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.ActivationDecision;
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.ChangeType;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public class ModifyUserAction extends BaseAction {

    private static final Trace LOGGER = TraceManager.getTrace(ModifyUserAction.class);
    private final String actionName;
    private PolicyDecision policyDecision;
    private ActivationDecision userActivationDecision;
    private ActivationDecision accountActivationDecision;

    public ModifyUserAction() {
        this(PolicyDecision.KEEP, ACTION_MODIFY_USER);
    }

    public ModifyUserAction(PolicyDecision policyDecision, String actionName) {
        Validate.notEmpty(actionName, "Action name must not be null or empty.");

        this.policyDecision = policyDecision;
        this.actionName = actionName;
    }

    protected void setAccountActivationDecision(ActivationDecision decision) {
        this.accountActivationDecision = decision;
    }

    protected void setUserActivationDecision(ActivationDecision decision) {
        this.userActivationDecision = decision;
    }

    protected PolicyDecision getPolicyDecision() {
        return policyDecision;
    }

    protected ActivationDecision getUserActivationDecision() {
        return userActivationDecision;
    }

    protected ActivationDecision getAccountActivationDecision() {
        return accountActivationDecision;
    }

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
                                 SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
                                 OperationResult result) throws SynchronizationException {
        super.executeChanges(userOid, change, situation, shadowAfterChange, result);

        if (!(shadowAfterChange instanceof AccountShadowType)) {
            throw new SynchronizationException("Couldn't synchronize shadow of type '"
                    + shadowAfterChange.getClass().getName() + "', only '"
                    + AccountShadowType.class.getName() + "' is supported.");
        }

        OperationResult subResult = result.createSubresult(actionName);
        result.addSubresult(subResult);

        UserType userType = getUser(userOid, subResult);
        if (userType == null) {
            String message = "Can't find user with oid '" + userOid + "'.";
            subResult.computeStatus(message);
            throw new SynchronizationException(message);
        }

        SyncContext context = createSyncContext(userType, change.getResource());
        try {
            createAccountSyncContext(context, change, (AccountShadowType) shadowAfterChange);

            getSynchronizer().synchronizeUser(context, subResult);
        } catch (Exception ex) {
            throw new SynchronizationException("Couldn't update account sync context in modify user action.", ex);
        } finally {
            subResult.recomputeStatus("Couldn't update account sync context in modify user action.");
        }

        try {
            getExecutor().executeChanges(context, subResult);
        } catch (Exception ex) {
            throw new SynchronizationException("Couldn't execute modify user action.", ex);
        } finally {
            subResult.recomputeStatus("Couldn't execute modify user action.");
        }

        return userOid;
    }

    private SyncContext createSyncContext(UserType user, ResourceType resource) {
        SyncContext context = new SyncContext();
        ObjectDefinition<UserType> userDefinition = getSchemaRegistry().getCommonSchema().findObjectDefinitionByType(
                SchemaConstants.I_USER_TYPE);
        MidPointObject<UserType> oldUser = userDefinition.instantiate(SchemaConstants.I_USER_TYPE);
        oldUser.setObjectType(user);
        context.setUserOld(oldUser);
        context.setUserTypeOld(user);
        context.rememberResource(resource);

        if (getUserActivationDecision() != null) {
            //todo enable or disable user
        }

        return context;
    }

    private AccountSyncContext createAccountSyncContext(SyncContext context,
                                                        ResourceObjectShadowChangeDescriptionType change,
                                                        AccountShadowType account) throws SchemaException {
        ResourceType resource = change.getResource();

        ResourceAccountType resourceAccount = new ResourceAccountType(resource.getOid(), account.getAccountType());
        AccountSyncContext accountContext = context.createAccountSyncContext(resourceAccount);
        accountContext.setResource(resource);
        accountContext.setPolicyDecision(getPolicyDecision());
        accountContext.setActivationDecision(getAccountActivationDecision());
        accountContext.setOid(account.getOid());

        ObjectDefinition<AccountShadowType> definition;
        definition = RefinedResourceSchema.getRefinedSchema(resource, getSchemaRegistry()).getObjectDefinition(account);

        ObjectDelta<AccountShadowType> delta = createObjectDelta(change.getObjectChange(), definition);
        accountContext.setAccountPrimaryDelta(delta);

        return accountContext;
    }

    private ObjectDelta<AccountShadowType> createObjectDelta(ObjectChangeType change,
                                                             ObjectDefinition<AccountShadowType> definition) throws SchemaException {

        ObjectDelta<AccountShadowType> account = null;
        if (change instanceof ObjectChangeAdditionType) {
            ObjectChangeAdditionType addition = (ObjectChangeAdditionType) change;

            account = new ObjectDelta<AccountShadowType>(AccountShadowType.class, ChangeType.ADD);
            MidPointObject<AccountShadowType> object = new MidPointObject<AccountShadowType>(SchemaConstants.I_ACCOUNT_SHADOW_TYPE);
            object.setObjectType((AccountShadowType) addition.getObject());
            account.setObjectToAdd(object);
        } else if (change instanceof ObjectChangeDeletionType) {
            ObjectChangeDeletionType deletion = (ObjectChangeDeletionType) change;

            account = new ObjectDelta<AccountShadowType>(AccountShadowType.class, ChangeType.DELETE);
            account.setOid(deletion.getOid());
        } else if (change instanceof ObjectChangeModificationType) {
            ObjectChangeModificationType modificationChange = (ObjectChangeModificationType) change;
            ObjectModificationType modification = modificationChange.getObjectModification();
            account = ObjectDelta.createDelta(modification, definition);
        }

        if (account == null) {
            throw new IllegalArgumentException("Unknown object change type instance '"
                    + change.getClass() + "',it's not add, delete nor modify.");
        }

        return account;
    }
}
