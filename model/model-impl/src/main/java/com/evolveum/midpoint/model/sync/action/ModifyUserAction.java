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
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.SynchronizationIntent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
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
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author lazyman
 */
public class ModifyUserAction extends BaseAction {

    private static final Trace LOGGER = TraceManager.getTrace(ModifyUserAction.class);
    /**
     * Action name for operation result
     */
    private final String actionName;
    /**
     * Decision regarding the user. If set to null user activation won't be changed. If set to
     * {@link ActivationDecision#DISABLE} ({@link ActivationDecision#ENABLE}) user will be disabled (enabled),
     */
    private ActivationDecision userActivationDecision;
    /**
     * Decision regarding the account. If set to null account activation won't be changed. If set to
     * {@link ActivationDecision#DISABLE} ({@link ActivationDecision#ENABLE}) account will be disabled (enabled),
     */
    private ActivationDecision accountActivationDecision;
    /**
     * Decision regarding account state see {@link SynchronizationPolicyDecision}.
     */
    private SynchronizationIntent accountSynchronizationIntent;

    public ModifyUserAction() {
        this(SynchronizationIntent.KEEP, ACTION_MODIFY_USER);
    }

    public ModifyUserAction(SynchronizationIntent accountSyncIntent, String actionName) {
        Validate.notEmpty(actionName, "Action name must not be null or empty.");
        this.accountSynchronizationIntent = accountSyncIntent;
        this.actionName = actionName;
    }

    protected void setAccountActivationDecision(ActivationDecision decision) {
        this.accountActivationDecision = decision;
    }

    protected void setUserActivationDecision(ActivationDecision decision) {
        this.userActivationDecision = decision;
    }

    protected SynchronizationIntent getAccountSynchronizationIntent() {
        return accountSynchronizationIntent;
    }

    protected ActivationDecision getUserActivationDecision() {
        return userActivationDecision;
    }

    protected ActivationDecision getAccountActivationDecision() {
        return accountActivationDecision;
    }

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescription change, ObjectTemplateType userTemplate,
            SynchronizationSituationType situation, AuditEventRecord auditRecord, Task task, 
            OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
        super.executeChanges(userOid, change, userTemplate, situation, auditRecord, task, result);

        OperationResult subResult = result.createSubresult(actionName);
        if (StringUtils.isEmpty(userOid)) {
            String message = "Can't modify user, user oid is empty or null.";
            subResult.recordFatalError(message);
            throw new SchemaException(message);
        }

        UserType userType = getUser(userOid, subResult);
        if (userType == null) {
            String message = "Can't find user with oid '" + userOid + "'.";
            subResult.recordFatalError(message);
            throw new ObjectNotFoundException(message);
        }

        LensContext<UserType, ShadowType> context = null;
        LensProjectionContext<ShadowType> accountContext = null;
        try {
            context = createSyncContext(userType, change.getResource().asObjectable(), userTemplate, change);
            accountContext = createAccountLensContext(context, change,
                    getAccountSynchronizationIntent(), getAccountActivationDecision());
            if (accountContext == null) {
                LOGGER.warn("Couldn't create account sync context, skipping action for this change.");
                return userOid;
            }
        } catch (RuntimeException ex) {
        	subResult.recordFatalError("Couldn't update account sync context in modify user action: "+ex.getMessage(), ex);
            throw new SystemException("Couldn't update account sync context in modify user action.", ex);
        }
        
        updateContextBeforeSync(context, accountContext);

        try {
            synchronizeUser(context, task, subResult);
        } finally {
            subResult.recomputeStatus();
            result.recomputeStatus();
        }

        return userOid;
    }

    /**
	 * A chance to update the context before a sync is executed. For use in subclasses.
	 */
	protected void updateContextBeforeSync(LensContext<UserType, ShadowType> context, 
			LensProjectionContext<ShadowType> accountContext) {
		// Nothing to do here
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

    private LensContext<UserType, ShadowType> createSyncContext(UserType user, ResourceType resource, ObjectTemplateType userTemplate, ResourceObjectShadowChangeDescription change) throws SchemaException {
        LOGGER.trace("Creating sync context.");

        PrismObjectDefinition<UserType> userDefinition = getPrismContext().getSchemaRegistry().findObjectDefinitionByType(
        		UserType.COMPLEX_TYPE);

        LensContext<UserType, ShadowType> context = createEmptyLensContext(change);
        LensFocusContext<UserType> focusContext = context.createFocusContext();
        PrismObject<UserType> oldUser = user.asPrismObject();
        focusContext.setObjectOld(oldUser);
        context.rememberResource(resource);
        context.setUserTemplate(userTemplate);

        //check and update activation if necessary
        if (userActivationDecision == null) {
            LOGGER.trace("User activation decision not defined, skipping activation check.");
            return context;
        }

        PrismProperty enable = oldUser.findOrCreateProperty(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        LOGGER.trace("User activation defined, activation property found {}", enable);

        PrismPropertyValue<ActivationStatusType> value = enable.getValue(ActivationStatusType.class);
        if (value != null) {
            ActivationStatusType status = value.getValue();
            if (status == null) {
                createActivationPropertyDelta(context, userActivationDecision, null);
            }

            Boolean isEnabled = ActivationStatusType.ENABLED == status ? Boolean.TRUE : Boolean.FALSE;
            
            if ((isEnabled && ActivationDecision.DISABLE.equals(userActivationDecision))
                    || (!isEnabled && ActivationDecision.ENABLE.equals(userActivationDecision))) {

                createActivationPropertyDelta(context, userActivationDecision, status);
            }
        } else {
            createActivationPropertyDelta(context, userActivationDecision, null);
        }

        return context;
    }

    private void createActivationPropertyDelta(LensContext<UserType, ShadowType> context, ActivationDecision activationDecision,
            ActivationStatusType oldValue) {

    	LensFocusContext<UserType> focusContext = context.getFocusContext();
        ObjectDelta<UserType> userDelta = focusContext.getSecondaryDelta(0);
        if (userDelta == null) {
            userDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.MODIFY, getPrismContext());
            userDelta.setOid(focusContext.getObjectOld().getOid());
            focusContext.setSecondaryDelta(userDelta, 0);
        }

        createActivationPropertyDelta(userDelta, activationDecision, oldValue);
    }
}
