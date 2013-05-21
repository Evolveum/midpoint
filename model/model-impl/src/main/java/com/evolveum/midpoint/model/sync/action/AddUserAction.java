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

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.SynchronizationIntent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
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
public class AddUserAction extends BaseAction {

    private static final Trace LOGGER = TraceManager.getTrace(AddUserAction.class);

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescription change, ObjectTemplateType userTemplate, 
            SynchronizationSituationType situation, AuditEventRecord auditRecord, Task task, OperationResult result) 
    			throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
        super.executeChanges(userOid, change, userTemplate, situation, auditRecord, task, result);

        OperationResult subResult = result.createSubresult(ACTION_ADD_USER);

        LensContext<UserType, ShadowType> context = createEmptyLensContext(change);
        LensFocusContext<UserType> focusContext = context.createFocusContext();
        try {
        	
            UserType user = getUser(userOid, subResult);
            if (user == null) {
                //set user template to context from action configuration
            	ObjectTemplateType ot = getUserTemplate(subResult);
            	if (ot != null){
            		context.setUserTemplate(ot);
            	} else{
            		context.setUserTemplate(userTemplate);
            	}
				
                if (context.getUserTemplate() != null) {
                    LOGGER.debug("Using user template {}", context.getUserTemplate().getName());
                } else {
                    LOGGER.debug("User template not defined.");
                }

                //add account sync context for inbound processing
                LensProjectionContext<ShadowType> accountContext = createAccountLensContext(context, change, 
                		SynchronizationIntent.KEEP, null);
                if (accountContext == null) {
                    LOGGER.warn("Couldn't create account sync context, skipping action for this change.");
                    return userOid;
                }

                //create empty user
                PrismObjectDefinition<UserType> userDefinition = getPrismContext().getSchemaRegistry().
                				findObjectDefinitionByType(UserType.COMPLEX_TYPE);
                PrismObject<UserType> oldUser = userDefinition.instantiate(UserType.COMPLEX_TYPE);
//                context.setUserOld(oldUser);
//                context.setUserTypeOld(user);

                //we set secondary delta to create user when executing changes
                ObjectDelta<UserType> delta = new ObjectDelta<UserType>(UserType.class, ChangeType.ADD, getPrismContext());
                delta.setObjectToAdd(oldUser);
                focusContext.setSecondaryDelta(delta, 0);

                context.rememberResource(change.getResource().asObjectable());
            } else {
                LOGGER.debug("User with oid {} already exists, skipping create.",
                        new Object[]{user.getOid()});
            }
        } catch (RuntimeException ex) {
            PrismObject<ShadowType> shadowAfterChange = getAccountShadowFromChange(change);

            LoggingUtils.logException(LOGGER, "Couldn't perform Add User Action for shadow '{}', oid '{}'.",
                    ex, shadowAfterChange.getName(), shadowAfterChange.getOid());
            subResult.recordFatalError("Couldn't perform Add User Action for shadow '" + shadowAfterChange.getName()
                    + "', oid '" + shadowAfterChange.getOid() + "'.", ex);

            throw ex;
        } finally {
            subResult.recomputeStatus();
        }

        try {
            synchronizeUser(context, task, subResult);

            userOid = focusContext.getSecondaryDelta().getOid();
        
        } finally {
            subResult.recomputeStatus();
            result.recomputeStatus();            
        }

        return userOid;
    }

    private ObjectTemplateType getUserTemplate(OperationResult result) throws ObjectNotFoundException, SchemaException {
        Element templateRef = getParameterElement(new QName(SchemaConstants.NS_C, "userTemplateRef"));
        if (templateRef == null) {
            return null;
        }

        String oid = templateRef.getAttribute("oid");
        if (StringUtils.isEmpty(oid)) {
            return null;
        }

        return getModel().getObjectResolver().getObjectSimple(ObjectTemplateType.class, oid, null, result);
    }
}
