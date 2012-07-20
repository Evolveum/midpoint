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
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class AddUserAction extends BaseAction {

    private static final Trace LOGGER = TraceManager.getTrace(AddUserAction.class);

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescription change,
            SynchronizationSituationType situation, AuditEventRecord auditRecord, Task task, OperationResult result) throws SynchronizationException, SchemaException {
        super.executeChanges(userOid, change, situation, auditRecord, task, result);

        OperationResult subResult = result.createSubresult(ACTION_ADD_USER);

        SyncContext context = new SyncContext(getPrismContext());
        try {
            UserType user = getUser(userOid, subResult);
            if (user == null) {
                //set user template to context from action configuration
                context.setUserTemplate(getUserTemplate(subResult));
                if (context.getUserTemplate() != null) {
                    LOGGER.debug("Using user template {}", context.getUserTemplate().getName());
                } else {
                    LOGGER.debug("User template not defined.");
                }

                //add account sync context for inbound processing
                AccountSyncContext accountContext = createAccountSyncContext(context, change, PolicyDecision.KEEP, null);
                if (accountContext == null) {
                    LOGGER.warn("Couldn't create account sync context, skipping action for this change.");
                    return userOid;
                }

                //create empty user
                PrismObjectDefinition<UserType> userDefinition = getPrismContext().getSchemaRegistry().
                				findObjectDefinitionByType(SchemaConstants.I_USER_TYPE);
                PrismObject<UserType> oldUser = userDefinition.instantiate(SchemaConstants.I_USER_TYPE);
//                context.setUserOld(oldUser);
//                context.setUserTypeOld(user);

                //we set secondary delta to create user when executing changes
                ObjectDelta<UserType> delta = new ObjectDelta<UserType>(UserType.class, ChangeType.ADD);
                delta.setObjectToAdd(oldUser);
                context.setUserSecondaryDelta(delta, 0);

                context.rememberResource(change.getResource().asObjectable());
            } else {
                LOGGER.debug("User with oid {} already exists, skipping create.",
                        new Object[]{user.getOid()});
            }
        } catch (Exception ex) {
            ResourceObjectShadowType shadowAfterChange = getAccountShadowFromChange(change);

            LoggingUtils.logException(LOGGER, "Couldn't perform Add User Action for shadow '{}', oid '{}'.",
                    ex, shadowAfterChange.getName(), shadowAfterChange.getOid());
            subResult.recordFatalError("Couldn't perform Add User Action for shadow '" + shadowAfterChange.getName()
                    + "', oid '" + shadowAfterChange.getOid() + "'.", ex);

            throw new SynchronizationException(ex.getMessage(), ex);
        } finally {
            subResult.recomputeStatus();
        }

        try {
            synchronizeUser(context, task, subResult);

            userOid = context.getUserSecondaryDelta().getOid();
        
        } finally {
            subResult.recomputeStatus();
            result.recomputeStatus();
            
            auditRecord.clearTimestamp();
            auditRecord.setEventType(AuditEventType.ADD_OBJECT);
        	auditRecord.setEventStage(AuditEventStage.EXECUTION);
        	auditRecord.setResult(result);
        	auditRecord.clearDeltas();
        	auditRecord.addDeltas(context.getAllChanges());
        	getAuditService().audit(auditRecord, task);
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

        return getModel().getObjectResolver().getObject(UserTemplateType.class, oid, null, result);
    }
}
