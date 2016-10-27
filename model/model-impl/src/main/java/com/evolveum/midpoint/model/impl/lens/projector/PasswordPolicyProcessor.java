/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.policy.PasswordPolicyUtils;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensObjectDeltaOperation;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordHistoryEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;


@Component
public class PasswordPolicyProcessor {
	
	private static final Trace LOGGER = TraceManager.getTrace(PasswordPolicyProcessor.class);
	
	@Autowired(required = true)
	Protector protector;
	
	@Autowired(required = true)
	ModelObjectResolver resolver;

	
	
	<F extends FocusType> void processPasswordPolicy(LensFocusContext<F> focusContext, 
			LensContext<F> context, Task task, OperationResult result)
			throws PolicyViolationException, SchemaException {
		
		if (!UserType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
			LOGGER.trace("Skipping processing password policies because focus is not user");
			return;
		}
		
//		PrismProperty<PasswordType> password = getPassword(focusContext);
		ObjectDelta userDelta = focusContext.getDelta();

		if (userDelta == null) {
			LOGGER.trace("Skipping processing password policies. User delta not specified.");
			return;
		}
		
		if (userDelta.isDelete()) {
			LOGGER.trace("Skipping processing password policies. User will be deleted.");
			return;
		}

		PrismProperty<ProtectedStringType> passwordValueProperty = null;
		PrismObject<F> user;
		if (ChangeType.ADD == userDelta.getChangeType()) {
			user = focusContext.getDelta().getObjectToAdd();
			if (user != null) {
				passwordValueProperty = user.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
			}
			if (passwordValueProperty == null){
				if (wasExecuted(userDelta, focusContext)){
					LOGGER.trace("Skipping processing password policies. User addition was already executed.");
					return;
				}
			}
		} else if (ChangeType.MODIFY == userDelta.getChangeType()) {
			PropertyDelta<ProtectedStringType> passwordValueDelta;
			passwordValueDelta = userDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
			if (passwordValueDelta == null) {
				LOGGER.trace("Skipping processing password policies. User delta does not contain password change.");
				return;
			}
			if (userDelta.getChangeType() == ChangeType.MODIFY) {
				if (passwordValueDelta.isAdd()) {
					passwordValueProperty = (PrismProperty<ProtectedStringType>) passwordValueDelta.getItemNewMatchingPath(null);
				} else if (passwordValueDelta.isDelete()) {
					passwordValueProperty = null;
				} else {
					passwordValueProperty = (PrismProperty<ProtectedStringType>) passwordValueDelta.getItemNewMatchingPath(null);
				}
			} else {
				passwordValueProperty = (PrismProperty<ProtectedStringType>) passwordValueDelta.getItemNewMatchingPath(null);
			}
		}
		
		ValuePolicyType passwordPolicy;
		if (focusContext.getOrgPasswordPolicy() == null) {
			passwordPolicy = determineValuePolicy(userDelta, focusContext.getObjectAny(), context, task, result);
			focusContext.setOrgPasswordPolicy(passwordPolicy);
		} else {
			passwordPolicy = focusContext.getOrgPasswordPolicy();
		}
		
		processPasswordPolicy(passwordPolicy, focusContext.getObjectOld(), passwordValueProperty, result);

	}
	
	private <F extends FocusType> void processPasswordPolicy(ValuePolicyType passwordPolicy, PrismObject<F> focus, PrismProperty<ProtectedStringType> passwordProperty, OperationResult result)
			throws PolicyViolationException, SchemaException {

		if (passwordPolicy == null) {
			LOGGER.trace("Skipping processing password policies. Password policy not specified.");
			return;
		}

        String passwordValue = determinePasswordValue(passwordProperty);
        List<String> oldPasswords = determineOldPasswordValues(focus);
       
        boolean isValid = PasswordPolicyUtils.validatePassword(passwordValue, oldPasswords, passwordPolicy, result);

		if (!isValid) {
			result.computeStatus();
			throw new PolicyViolationException("Provided password does not satisfy password policies. " + result.getMessage());
		}
	}

	private <F extends FocusType> List<String> determineOldPasswordValues(PrismObject<F> focus) {
		if (focus == null) {
			return null;
		}
		List<String> oldPasswords = null;
		if (focus.getCompileTimeClass().equals(UserType.class)) {
			
        	PrismContainer<PasswordHistoryEntryType> historyEntries = focus.findContainer(new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_HISTORY_ENTRY));
        	if (historyEntries == null || historyEntries.isEmpty()) {
        		return null;
        	}
        	
        	Collection<PasswordHistoryEntryType> historyEntryValues = historyEntries.getRealValues();
        	oldPasswords = new ArrayList<>(historyEntryValues.size());
        	for (PasswordHistoryEntryType historyEntryValue : historyEntryValues) {
        		try {
					oldPasswords.add(protector.decryptString(historyEntryValue.getValue()));
				} catch (EncryptionException e) { //TODO: do we want to fail when we can't decrypt old values?
					throw new SystemException("Failed to process password for user: " , e);
				}
        	}
        	
        }
		return oldPasswords;
	}

	private <F extends FocusType> boolean wasExecuted(ObjectDelta<UserType> userDelta, LensFocusContext<F> focusContext){
		
		for (LensObjectDeltaOperation<F> executedDeltaOperation : focusContext.getExecutedDeltas()){
			ObjectDelta<F> executedDelta = executedDeltaOperation.getObjectDelta();
			if (!executedDelta.isAdd()){
				continue;
			} else if (executedDelta.getObjectToAdd() != null && executedDelta.getObjectTypeClass().equals(UserType.class)){
				return true;
			}
		}
		
		return false;
	}
	
	//TODO: maybe some caching of orgs?????
	protected <T extends ObjectType, F extends FocusType> ValuePolicyType determineValuePolicy(ObjectDelta<F> userDelta, PrismObject<T> object, LensContext<F> context, Task task, OperationResult result) throws SchemaException{
		//check the modification of organization first
		ValuePolicyType valuePolicy = determineValuePolicy(userDelta, task, result);
		
		//if null, check the existing organization
		if (valuePolicy == null){
			valuePolicy = determineValuePolicy(object, task, result);
		}
		
		//if still null, just use global policy
		if (valuePolicy == null){
			valuePolicy = context.getEffectivePasswordPolicy();
		}
		
		if (valuePolicy != null){
			LOGGER.trace("Value policy {} will be user to check password.", valuePolicy.getName().getOrig());
		}
		
		return valuePolicy;
	}
	
	protected <F extends FocusType> ValuePolicyType determineValuePolicy(ObjectDelta<F> userDelta, Task task, OperationResult result)
			throws SchemaException {
		if (userDelta == null) {
			return null;
		}
		ReferenceDelta orgDelta = userDelta.findReferenceModification(UserType.F_PARENT_ORG_REF);

		LOGGER.trace("Determining password policy from org delta.");
		if (orgDelta == null) {
			return null;
		}

		PrismReferenceValue orgRefValue = orgDelta.getAnyValue();
		if (orgRefValue == null) {		// delta may be of type "replace to null"
			return null;
		}

		ValuePolicyType passwordPolicy = null;
		try {
			PrismObject<OrgType> org = resolver.resolve(orgRefValue,
					"resolving parent org ref", null, null, result);
			OrgType orgType = org.asObjectable();
			ObjectReferenceType ref = orgType.getPasswordPolicyRef();
			if (ref != null) {
				LOGGER.trace("Org {} has specified password policy.", orgType);
				passwordPolicy = resolver.resolve(ref, ValuePolicyType.class, null,
						"resolving password policy for organization", task, result);
				LOGGER.trace("Resolved password policy {}", passwordPolicy);
			}

			if (passwordPolicy == null) {
				passwordPolicy = determineValuePolicy(org, task, result);
			}

		} catch (ObjectNotFoundException e) {
			throw new IllegalStateException(e);
		}

		return passwordPolicy;
	}
	
	private ValuePolicyType determineValuePolicy(PrismObject object, Task task, OperationResult result)
			throws SchemaException {
		LOGGER.trace("Determining password policies from object: {}", ObjectTypeUtil.toShortString(object));
		PrismReference orgRef = object.findReference(ObjectType.F_PARENT_ORG_REF);
		if (orgRef == null) {
			return null;
		}
		List<PrismReferenceValue> orgRefValues = orgRef.getValues();
		ValuePolicyType resultingValuePolicy = null;
		List<PrismObject<OrgType>> orgs = new ArrayList<PrismObject<OrgType>>();
		try {
			for (PrismReferenceValue orgRefValue : orgRefValues) {
				if (orgRefValue != null) {

					PrismObject<OrgType> org = resolver.resolve(orgRefValue, "resolving parent org ref", null, null, result);
					orgs.add(org);
					ValuePolicyType valuePolicy = resolvePolicy(org, task, result);

					if (valuePolicy != null) {
						if (resultingValuePolicy == null) {
							resultingValuePolicy = valuePolicy;
						} else if (!StringUtils.equals(valuePolicy.getOid(), resultingValuePolicy.getOid())) {
							throw new IllegalStateException(
									"Found more than one policy while trying to validate user's password. Please check your configuration");
						}
					}
				}
			}
		} catch (ObjectNotFoundException ex) {
			throw new IllegalStateException(ex);
		}
		// go deeper
		if (resultingValuePolicy == null) {
			for (PrismObject<OrgType> orgType : orgs) {
				resultingValuePolicy = determineValuePolicy(orgType, task, result);
				if (resultingValuePolicy != null) {
					return resultingValuePolicy;
				}
			}
		}
		return resultingValuePolicy;
	}
	
	private ValuePolicyType resolvePolicy(PrismObject<OrgType> org, Task task, OperationResult result)
			throws SchemaException {
		try {
			OrgType orgType = org.asObjectable();
			ObjectReferenceType ref = orgType.getPasswordPolicyRef();
			if (ref == null) {
				return null;
			}

			return resolver.resolve(ref, ValuePolicyType.class, null,
					"resolving password policy for organization", task, result);

		} catch (ObjectNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalStateException(e);
		}

	}
	
	<F extends ObjectType> void processPasswordPolicy(LensProjectionContext projectionContext, 
			LensContext<F> context, Task task, OperationResult result) throws SchemaException, PolicyViolationException{
		
		ObjectDelta accountDelta = projectionContext.getDelta();
		
		if (accountDelta == null){
			LOGGER.trace("Skipping processing password policies. Shadow delta not specified.");
			return;
		}
		
		if (ChangeType.DELETE == accountDelta.getChangeType()){
			return;
		}
		
		PrismObject<ShadowType> accountShadow;
		PrismProperty<ProtectedStringType> password = null;
		if (ChangeType.ADD == accountDelta.getChangeType()){
			accountShadow = accountDelta.getObjectToAdd();
			if (accountShadow != null){
				password = accountShadow.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
			}
		}
		if (ChangeType.MODIFY == accountDelta.getChangeType() || password == null) {
			PropertyDelta<ProtectedStringType> passwordValueDelta =
					accountDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
			// Modification sanity check
			if (accountDelta.getChangeType() == ChangeType.MODIFY && passwordValueDelta != null
					&& (passwordValueDelta.isAdd() || passwordValueDelta.isDelete())) {
				throw new SchemaException("Shadow password value cannot be added or deleted, it can only be replaced");
			}
			if (passwordValueDelta == null) {
				LOGGER.trace("Skipping processing password policies. Shadow delta does not contain password change.");
				return;
			}
			password = (PrismProperty<ProtectedStringType>) passwordValueDelta.getItemNewMatchingPath(null);
		}

		ValuePolicyType passwordPolicy;
		if (isCheckOrgPolicy(context)){
			passwordPolicy = determineValuePolicy(context.getFocusContext().getObjectAny(), task, result);
			context.getFocusContext().setOrgPasswordPolicy(passwordPolicy);
		} else {
			passwordPolicy = projectionContext.getEffectivePasswordPolicy();
		}
		
		processPasswordPolicy(passwordPolicy, null, password, result);
	}
	
	private <F extends ObjectType> boolean isCheckOrgPolicy(LensContext<F> context) throws SchemaException{
		LensFocusContext focusCtx = context.getFocusContext();
		if (focusCtx == null) {
			return false;			// TODO - ok?
		}

		if (focusCtx.getDelta() != null){
			if (focusCtx.getDelta().isAdd()){
				return false;
			}
			
			if (focusCtx.getDelta().isModify() && focusCtx.getDelta().hasItemDelta(SchemaConstants.PATH_PASSWORD_VALUE)){
				return false;
			}
		}
		
		if (focusCtx.getOrgPasswordPolicy() != null){
			return false;
		}
		
		return true;
	}


    // On missing password this returns empty string (""). It is then up to password policy whether it allows empty passwords or not.
	private String determinePasswordValue(PrismProperty<ProtectedStringType> password) {
		if (password == null || password.getValue(ProtectedStringType.class) == null) {
			return null;
		}

		ProtectedStringType passValue = password.getRealValue();

		if (passValue == null) {
			return null;
		}

		String passwordStr = passValue.getClearValue();

		if (passwordStr == null && passValue.getEncryptedDataType () != null) {
			// TODO: is this appropriate handling???
			try {
				passwordStr = protector.decryptString(passValue);
			} catch (EncryptionException ex) {
				throw new SystemException("Failed to process password for user: " , ex);
			}
		}

		return passwordStr;
	}


}
