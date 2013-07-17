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
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.SynchronizationIntent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

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
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescription change, ObjectTemplateType userTemplate, 
            SynchronizationSituationType situation, Task task, OperationResult result)
            		throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
        super.executeChanges(userOid, change, userTemplate, situation, task, result);

        Class<? extends ShadowType> clazz = getClassFromChange(change);
        if (!ShadowType.class.isAssignableFrom(clazz)) {
            throw new SchemaException("Couldn't synchronize shadow of type '"
                    + clazz + "', only '" + ShadowType.class.getName() + "' is supported.");
        }

        OperationResult subResult = result.createSubresult(actionName);
        if (StringUtils.isEmpty(userOid)) {
            String message = "Can't synchronize, user oid is empty or null.";
            subResult.computeStatus(message);
            throw new SchemaException(message);
        }

        UserType userType = getUser(userOid, subResult);
        if (userType == null) {
            String message = "Can't find user with oid '" + userOid + "'.";
            subResult.computeStatus(message);
            throw new ObjectNotFoundException(message);
        }

        LensContext<UserType, ShadowType> context = null;
        try {
            context = createLensContext(userType, change.getResource().asObjectable(), userTemplate, change);

            LensProjectionContext<ShadowType> accountContext = createAccountLensContext(context, change,
                    SynchronizationIntent.SYNCHRONIZE, null);
            if (accountContext == null) {
                LOGGER.warn("Couldn't create account sync context, skipping action for this change.");
                return userOid;
            }
            
        } finally {
            subResult.recomputeStatus("Couldn't update account sync context in modify user action.");
        }

        try {
            synchronizeUser(context, task, subResult);
        } finally {
            subResult.recomputeStatus();
            result.recomputeStatus();
        }

        return userOid;
    }

    private Class<? extends ShadowType> getClassFromChange(ResourceObjectShadowChangeDescription change) {
        if (change.getObjectDelta() != null) {
            return change.getObjectDelta().getObjectTypeClass();
        }

        if (change.getCurrentShadow() != null) {
            return change.getCurrentShadow().getCompileTimeClass();
        }

        return change.getOldShadow().getCompileTimeClass();
    }

    private LensContext<UserType, ShadowType> createLensContext(UserType user, ResourceType resource, ObjectTemplateType userTemplate, ResourceObjectShadowChangeDescription change) throws SchemaException {
        LOGGER.trace("Creating sync context.");

        PrismObjectDefinition<UserType> userDefinition = getPrismContext().getSchemaRegistry().findObjectDefinitionByType(
        		UserType.COMPLEX_TYPE);

        LensContext<UserType, ShadowType> context = createEmptyLensContext(change);
        LensFocusContext<UserType> focusContext = context.createFocusContext();
        PrismObject<UserType> oldUser = user.asPrismObject();
        focusContext.setObjectOld(oldUser);
        context.rememberResource(resource);
        context.setUserTemplate(userTemplate);

        return context;
    }

}
