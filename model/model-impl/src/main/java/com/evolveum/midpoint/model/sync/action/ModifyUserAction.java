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

package com.evolveum.midpoint.model.sync.action;

import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.ActivationDecision;
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.commons.lang.StringUtils;
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
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescription change,
            SynchronizationSituationType situation, OperationResult result) throws SynchronizationException {
        super.executeChanges(userOid, change, situation, result);

        Class<? extends ResourceObjectShadowType> clazz = getClassFromChange(change);
        if (!AccountShadowType.class.isAssignableFrom(clazz)) {
            throw new SynchronizationException("Couldn't synchronize shadow of type '"
                    + clazz + "', only '" + AccountShadowType.class.getName() + "' is supported.");
        }

        OperationResult subResult = result.createSubresult(actionName);
        if (StringUtils.isEmpty(userOid)) {
            String message = "Can't modify user, user oid is empty or null.";
            subResult.computeStatus(message);
            throw new SynchronizationException(message);
        }

        UserType userType = getUser(userOid, subResult);
        if (userType == null) {
            String message = "Can't find user with oid '" + userOid + "'.";
            subResult.computeStatus(message);
            throw new SynchronizationException(message);
        }

        SyncContext context = null;
        try {
            context = createSyncContext(userType, change.getResource());

            AccountSyncContext accountContext = createAccountSyncContext(context, change);
            if (accountContext == null) {
                LOGGER.warn("Couldn't create account sync context, skipping action for this change.");
                return userOid;
            }

            accountContext.setPolicyDecision(getPolicyDecision());
            accountContext.setActivationDecision(getAccountActivationDecision());
            accountContext.setDoReconciliation(determineAttributeReconciliation(change));

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

	private boolean determineAttributeReconciliation(ResourceObjectShadowChangeDescription change) {
		Boolean reconcileAttributes = change.getResource().getSynchronization().isReconcileAttributes();
		if (reconcileAttributes == null) {
			// "Automatic mode", do reconciliation only if the complete current shadow was provided
			reconcileAttributes = change.getCurrentShadow() != null;
			LOGGER.trace("Attribute reconciliation automatic mode: {}",reconcileAttributes);
		} else {
			LOGGER.trace("Attribute reconciliation manual mode: {}",reconcileAttributes);
		}
		return reconcileAttributes;
	}

	private Class<? extends ResourceObjectShadowType> getClassFromChange(ResourceObjectShadowChangeDescription change) {
        if (change.getObjectDelta() != null) {
            return change.getObjectDelta().getObjectTypeClass();
        }

        if (change.getCurrentShadow() != null) {
            return change.getCurrentShadow().getClass();
        }

        return change.getOldShadow().getClass();
    }

    private SyncContext createSyncContext(UserType user, ResourceType resource) throws SchemaException {
        Schema schema = getSchemaRegistry().getObjectSchema();
        ObjectDefinition<UserType> userDefinition = schema.findObjectDefinitionByType(
                SchemaConstants.I_USER_TYPE);

        SyncContext context = new SyncContext();
        MidPointObject<UserType> oldUser = userDefinition.parseObjectType(user);
        context.setUserOld(oldUser);
        context.setActivationDecision(getUserActivationDecision());
        context.rememberResource(resource);

        return context;
    }
}
