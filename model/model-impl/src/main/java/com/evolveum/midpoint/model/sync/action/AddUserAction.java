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

import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;

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
                user = new ObjectFactory().createUserType();

                SyncContext context = new SyncContext();
                ObjectDefinition<UserType> userDefinition = getSchemaRegistry().getObjectSchema().findObjectDefinitionByType(
                        SchemaConstants.I_USER_TYPE);
                MidPointObject<UserType> oldUser = userDefinition.instantiate(SchemaConstants.I_USER_TYPE);
                oldUser.setObjectType(user);
                context.setUserOld(oldUser);
                context.setUserTypeOld(user);
                context.rememberResource(change.getResource());

                getSynchronizer().synchronizeUser(context, subResult);
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
}
