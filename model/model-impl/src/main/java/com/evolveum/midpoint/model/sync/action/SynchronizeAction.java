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
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.SynchronizationIntent;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 * @author Radovan Semancik
 */
public class SynchronizeAction extends BaseAction {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizeAction.class);
    /**
     * Action name for operation result
     */
    private final String actionName;

    public SynchronizeAction() {
        this(ACTION_SYNCHRONIZE);
    }

    public SynchronizeAction(String actionName) {
        Validate.notEmpty(actionName, "Action name must not be null or empty.");
        this.actionName = actionName;
    }

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescription change,
            SynchronizationSituationType situation, AuditEventRecord auditRecord, Task task, 
            OperationResult result) throws SynchronizationException, SchemaException {
        super.executeChanges(userOid, change, situation, auditRecord, task, result);

        Class<? extends ResourceObjectShadowType> clazz = getClassFromChange(change);
        if (!AccountShadowType.class.isAssignableFrom(clazz)) {
            throw new SynchronizationException("Couldn't synchronize shadow of type '"
                    + clazz + "', only '" + AccountShadowType.class.getName() + "' is supported.");
        }

        OperationResult subResult = result.createSubresult(actionName);
        if (StringUtils.isEmpty(userOid)) {
            String message = "Can't synchronize, user oid is empty or null.";
            subResult.computeStatus(message);
            throw new SynchronizationException(message);
        }

        UserType userType = getUser(userOid, subResult);
        if (userType == null) {
            String message = "Can't find user with oid '" + userOid + "'.";
            subResult.computeStatus(message);
            throw new SynchronizationException(message);
        }

        LensContext<UserType, AccountShadowType> context = null;
        try {
            context = createLensContext(userType, change.getResource().asObjectable(), change);

            LensProjectionContext<AccountShadowType> accountContext = createAccountLensContext(context, change,
                    SynchronizationIntent.SYNCHRONIZE, null);
            if (accountContext == null) {
                LOGGER.warn("Couldn't create account sync context, skipping action for this change.");
                return userOid;
            }
            
        } catch (Exception ex) {
            throw new SynchronizationException("Couldn't update account sync context in modify user action.", ex);
        } finally {
            subResult.recomputeStatus("Couldn't update account sync context in modify user action.");
        }

        try {
            synchronizeUser(context, task, subResult);
        } finally {
            subResult.recomputeStatus();
            result.recomputeStatus();
            
//            auditRecord.clearTimestamp();
//            auditRecord.setEventType(AuditEventType.MODIFY_OBJECT);
//        	auditRecord.setEventStage(AuditEventStage.EXECUTION);
//        	auditRecord.setResult(result);
//        	auditRecord.clearDeltas();
//        	auditRecord.addDeltas(context.getAllChanges());
//        	getAuditService().audit(auditRecord, task);
        }

        return userOid;
    }

    private Class<? extends ResourceObjectShadowType> getClassFromChange(ResourceObjectShadowChangeDescription change) {
        if (change.getObjectDelta() != null) {
            return change.getObjectDelta().getObjectTypeClass();
        }

        if (change.getCurrentShadow() != null) {
            return change.getCurrentShadow().getCompileTimeClass();
        }

        return change.getOldShadow().getCompileTimeClass();
    }

    private LensContext<UserType, AccountShadowType> createLensContext(UserType user, ResourceType resource, ResourceObjectShadowChangeDescription change) throws SchemaException {
        LOGGER.trace("Creating sync context.");

        PrismObjectDefinition<UserType> userDefinition = getPrismContext().getSchemaRegistry().findObjectDefinitionByType(
                SchemaConstants.I_USER_TYPE);

        LensContext<UserType, AccountShadowType> context = createEmptyLensContext(change);
        LensFocusContext<UserType> focusContext = context.createFocusContext();
        PrismObject<UserType> oldUser = user.asPrismObject();
        focusContext.setObjectOld(oldUser);
        context.rememberResource(resource);

        return context;
    }

}
