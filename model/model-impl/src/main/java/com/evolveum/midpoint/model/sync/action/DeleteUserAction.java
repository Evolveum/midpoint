/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.sync.action;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author lazyman
 */
public class DeleteUserAction extends BaseAction {

    private static final Trace LOGGER = TraceManager.getTrace(DeleteUserAction.class);

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescription change, ObjectTemplateType userTemplate,
            SynchronizationSituationType situation, AuditEventRecord auditRecord, Task task, 
            OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
        super.executeChanges(userOid, change, userTemplate, situation, auditRecord, task, result);

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

        LensContext<UserType, ShadowType> context = createEmptyLensContext(change);
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
            LensProjectionContext<ShadowType> accContext = createAccountLensContext(context, change, null, null);
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
