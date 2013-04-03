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
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.RewindException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import org.apache.commons.lang.StringUtils;

/**
 * @author lazyman
 */
public class DeleteUserAction extends BaseAction {

    private static final Trace LOGGER = TraceManager.getTrace(DeleteUserAction.class);

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescription change,
            SynchronizationSituationType situation, AuditEventRecord auditRecord, Task task, 
            OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, RewindException {
        super.executeChanges(userOid, change, situation, auditRecord, task, result);

        OperationResult subResult = result.createSubresult(ACTION_DELETE_USER);

        if (StringUtils.isEmpty(userOid)) {
            String message = "Can't delete user, user oid is empty or null.";
            subResult.computeStatus(message);
            throw new SchemaException(message);
        }

        UserType userType = getUser(userOid, subResult);
        if (userType == null) {
            String message = "Can't find user with oid '" + userOid + "'.";
            subResult.computeStatus(message);
            throw new ObjectNotFoundException(message);
        }

        LensContext<UserType, ResourceObjectShadowType> context = createEmptyLensContext(change);
        LensFocusContext<UserType> focusContext = context.createFocusContext();
        try {
            context.rememberResource(change.getResource().asObjectable());

            //set old user
            PrismObjectDefinition<UserType> userDefinition = getPrismContext().getSchemaRegistry().findObjectDefinitionByType(UserType.COMPLEX_TYPE);
            PrismObject<UserType> oldUser = userType.asPrismObject();
            focusContext.setObjectOld(oldUser);
            //set object delta with delete
            ObjectDelta<UserType> userDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.DELETE, getPrismContext());
            userDelta.setOid(oldUser.getOid());
            focusContext.setPrimaryDelta(userDelta);

            //create account context for this change
            LensProjectionContext<ResourceObjectShadowType> accContext = createAccountLensContext(context, change, null, null);
            if (accContext == null) {
                LOGGER.warn("Couldn't create account sync context, skipping action for this change.");
                return userOid;
            }
        } catch (RuntimeException ex) {
            LoggingUtils.logException(LOGGER, "Couldn't delete user {}", ex, userType.getName());
            throw new SystemException("Couldn't delete user '" + userType.getName()
                    + "', reason: " + ex.getMessage(), ex);
        } finally {
            subResult.recomputeStatus("Couldn't create sync context to delete user '" + userType.getName() + "'.");
        }

        try {
            synchronizeUser(context, task, subResult);
            userOid = null;
        } finally {
            subResult.recomputeStatus("Couldn't delete user '" + userType.getName() + "'.");
            result.recomputeStatus();
        }

        return userOid;
    }
}
