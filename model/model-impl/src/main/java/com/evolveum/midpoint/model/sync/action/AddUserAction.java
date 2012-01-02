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
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.ChangeType;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class AddUserAction extends BaseAction {

    private static final Trace LOGGER = TraceManager.getTrace(AddUserAction.class);

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
            SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
            OperationResult result) throws SynchronizationException {
        super.executeChanges(userOid, change, situation, shadowAfterChange, result);

        OperationResult subResult = new OperationResult(ACTION_ADD_USER);
        result.addSubresult(subResult);

        try {
            UserType user = getUser(userOid, subResult);
            if (user == null) {
                SyncContext context = new SyncContext();
                //set user template to context from action configuration
                context.setUserTemplate(getUserTemplate(subResult));

                //add account sync context for inbound processing
                AccountSyncContext accountContext = createAccountSyncContext(context, change, (AccountShadowType) shadowAfterChange);
                accountContext.setPolicyDecision(PolicyDecision.KEEP);

                //create empty user
                Schema schema = getSchemaRegistry().getObjectSchema();
                ObjectDefinition<UserType> userDefinition = schema.findObjectDefinitionByType(SchemaConstants.I_USER_TYPE);
                MidPointObject<UserType> oldUser = userDefinition.instantiate(SchemaConstants.I_USER_TYPE);
                context.setUserOld(oldUser);
                context.setUserTypeOld(user);

                //we set secondary delta to create user when executing changes
                ObjectDelta<UserType> delta = new ObjectDelta<UserType>(UserType.class, ChangeType.ADD);
                delta.setObjectToAdd(oldUser);
                context.setUserSecondaryDelta(delta);

                context.rememberResource(change.getResource());

                getSynchronizer().synchronizeUser(context, subResult);
                getExecutor().executeChanges(context, subResult);
            } else {
                LOGGER.debug("User with oid {} already exists, skipping create.",
                        new Object[]{user.getOid()});
            }
            subResult.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't perform Add User Action for shadow '{}', oid '{}'.",
                    ex, shadowAfterChange.getName(), shadowAfterChange.getOid());
            subResult.recordFatalError(
                    "Couldn't perform Add User Action for shadow '" + shadowAfterChange.getName()
                            + "', oid '" + shadowAfterChange.getOid() + "'.", ex);
            throw new SynchronizationException(ex.getMessage(), ex);
        } finally {
            subResult.recomputeStatus();
        }

        return userOid;
    }

    private UserTemplateType getUserTemplate(OperationResult result) throws ObjectNotFoundException, SchemaException {
        Element templateRef = getParameterElement(new QName(SchemaConstants.NS_C, "userTemplateRef"));
        if (templateRef == null) {
            return null;
        }

        String oid = templateRef.getAttribute("oid");
        if (StringUtils.isEmpty(oid)) {
            return null;
        }

        return getModel().getObject(UserTemplateType.class, oid, null, result);
    }
}
