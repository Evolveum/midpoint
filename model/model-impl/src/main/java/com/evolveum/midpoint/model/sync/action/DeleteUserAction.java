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

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.ChangeType;
import com.evolveum.midpoint.schema.processor.PrismObject;
import com.evolveum.midpoint.schema.processor.PrismObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.apache.commons.lang.StringUtils;

/**
 * @author lazyman
 */
public class DeleteUserAction extends BaseAction {

    private static final Trace LOGGER = TraceManager.getTrace(DeleteUserAction.class);

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescription change,
            SynchronizationSituationType situation, AuditEventRecord auditRecord, Task task, 
            OperationResult result) throws SynchronizationException {
        super.executeChanges(userOid, change, situation, auditRecord, task, result);

        OperationResult subResult = result.createSubresult(ACTION_DELETE_USER);

        if (StringUtils.isEmpty(userOid)) {
            String message = "Can't delete user, user oid is empty or null.";
            subResult.computeStatus(message);
            throw new SynchronizationException(message);
        }

        UserType userType = getUser(userOid, subResult);
        if (userType == null) {
            String message = "Can't find user with oid '" + userOid + "'.";
            subResult.computeStatus(message);
            throw new SynchronizationException(message);
        }

        SyncContext context = new SyncContext();
        try {
            context.rememberResource(change.getResource());

            //set old user
            Schema schema = getSchemaRegistry().getObjectSchema();
            PrismObjectDefinition<UserType> userDefinition = schema.findObjectDefinitionByType(SchemaConstants.I_USER_TYPE);
            PrismObject<UserType> oldUser = userDefinition.parseObjectType(userType);
            context.setUserOld(oldUser);
            context.setUserTypeOld(userType);
            //set object delta with delete
            ObjectDelta<UserType> userDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.DELETE);
            userDelta.setOid(oldUser.getOid());
            context.setUserSecondaryDelta(userDelta);

            //create account context for this change
            AccountSyncContext accContext = createAccountSyncContext(context, change, null, null);
            if (accContext == null) {
                LOGGER.warn("Couldn't create account sync context, skipping action for this change.");
                return userOid;
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't delete user {}", ex, userType.getName());
            throw new SynchronizationException("Couldn't delete user '" + userType.getName()
                    + "', reason: " + ex.getMessage(), ex);
        } finally {
            subResult.recomputeStatus("Couldn't create sync context to delete user '" + userType.getName() + "'.");
        }

        try {
            synchronizeUser(context, subResult);
            executeChanges(context, subResult);

            userOid = null;
        } finally {
            subResult.recomputeStatus("Couldn't delete user '" + userType.getName() + "'.");
            result.recomputeStatus();
            
            auditRecord.clearTimestamp();
            auditRecord.setEventType(AuditEventType.DELETE_OBJECT);
        	auditRecord.setEventStage(AuditEventStage.EXECUTION);
        	auditRecord.setResult(result);
        	auditRecord.clearDeltas();
        	auditRecord.addDeltas(context.getAllChanges());
        	getAuditService().audit(auditRecord, task);
        }

        return userOid;
    }
}
