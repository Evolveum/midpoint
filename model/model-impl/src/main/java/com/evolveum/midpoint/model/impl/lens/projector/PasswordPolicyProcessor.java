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

package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.policy.PasswordPolicyUtils;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
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
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;


@Component
public class PasswordPolicyProcessor {
	
	private static final Trace LOGGER = TraceManager.getTrace(PasswordPolicyProcessor.class);
	
	@Autowired(required = true)
	Protector protector;
	
	@Autowired(required = true)
	ModelObjectResolver resolver;

	void processPasswordPolicy(ValuePolicyType passwordPolicy, PrismProperty password, OperationResult result)
			throws PolicyViolationException, SchemaException {

		if (passwordPolicy == null) {
			LOGGER.trace("Skipping processing password policies. Password policy not specified.");
			return;
		}

        String passwordValue = determinePasswordValue(password);

        boolean isValid = PasswordPolicyUtils.validatePassword(passwordValue, passwordPolicy, result);

		if (!isValid) {
			result.computeStatus();
			throw new PolicyViolationException("Provided password does not satisfy password policies. " + result.getMessage());
		}
	}
	
	<F extends FocusType> void processPasswordPolicy(LensFocusContext<F> focusContext, 
			LensContext<F> context, OperationResult result)
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

		PrismProperty<PasswordType> password = null;
		PrismObject<F> user = null;
		if (ChangeType.ADD == userDelta.getChangeType()) {
			user = focusContext.getDelta().getObjectToAdd();
			if (user != null) {
				password = user.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
			}
		} else if (ChangeType.MODIFY == userDelta.getChangeType()) {
			PropertyDelta<PasswordType> passwordValueDelta = null;
			if (userDelta != null) {
				passwordValueDelta = userDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
				if (passwordValueDelta == null) {
					LOGGER.trace("Skipping processing password policies. User delta does not contain password change.");
					return;
				}
				if (userDelta.getChangeType() == ChangeType.MODIFY && passwordValueDelta != null) {
					if (passwordValueDelta.isAdd()) {
						password = (PrismProperty<PasswordType>) passwordValueDelta.getItemNewMatchingPath(null);
					} else if (passwordValueDelta.isDelete()) {
						password = null;
					} else {
						password = (PrismProperty<PasswordType>) passwordValueDelta.getItemNewMatchingPath(null);
					}
				} else {
					password = (PrismProperty<PasswordType>) passwordValueDelta.getItemNewMatchingPath(null);
				}
			}
		}
		ValuePolicyType passwordPolicy = null;
		if (focusContext.getOrgPasswordPolicy() == null){
			passwordPolicy = determineValuePolicy(userDelta, focusContext.getObjectAny(), context, result);
			focusContext.setOrgPasswordPolicy(passwordPolicy);
		} else {
			passwordPolicy = focusContext.getOrgPasswordPolicy();
		}
		
		processPasswordPolicy(passwordPolicy, password, result);

	}
	
	//TODO: maybe some caching of orgs?????
	private <T extends ObjectType, F extends ObjectType> ValuePolicyType determineValuePolicy(ObjectDelta<UserType> userDelta, PrismObject<T> object, LensContext<F> context, OperationResult result) throws SchemaException{
		//check the modification of organization first
		ValuePolicyType valuePolicy = determineValuePolicy(userDelta, result);
		
		//if null, check the existing organization
		if (valuePolicy == null){
			valuePolicy = determineValuePolicy(object, result);
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
	
	private ValuePolicyType determineValuePolicy(ObjectDelta<UserType> userDelta, OperationResult result)
			throws SchemaException {
		ReferenceDelta orgDelta = userDelta.findReferenceModification(UserType.F_PARENT_ORG_REF);
		ValuePolicyType passwordPolicy = null;
		LOGGER.trace("Determining password policy from org delta.");
		if (orgDelta != null) {
			PrismReferenceValue orgRefValue = orgDelta.getAnyValue();

			try {
				PrismObject<OrgType> org = (PrismObject<OrgType>) resolver.resolve(orgRefValue,
						"resolving parent org ref", null, null, result);
				OrgType orgType = org.asObjectable();
				ObjectReferenceType ref = orgType.getPasswordPolicyRef();
				if (ref != null) {
					LOGGER.trace("Org {} has specified password policy.", orgType);
					passwordPolicy = resolver.resolve(ref, ValuePolicyType.class, null,
							"resolving password policy for organization", result);
					LOGGER.trace("Resolved password policy {}", passwordPolicy);
				}

				if (passwordPolicy == null) {
					passwordPolicy = determineValuePolicy(org, result);
				}

			} catch (ObjectNotFoundException e) {
				throw new IllegalStateException(e);
			}

		}
		
		return passwordPolicy;
	}
	
	private ValuePolicyType determineValuePolicy(PrismObject object, OperationResult result)
			throws SchemaException {
		LOGGER.trace("Determining password policies from object", object);
		PrismReference orgRef = object.findReference(ObjectType.F_PARENT_ORG_REF);
		if (orgRef == null) {
			return null;
		}
		List<PrismReferenceValue> values = orgRef.getValues();
		ValuePolicyType valuePolicy = null;
		List<PrismObject<OrgType>> orgs = new ArrayList<PrismObject<OrgType>>();
		try {
			for (PrismReferenceValue orgRefValue : values) {
				if (orgRefValue != null) {

					if (valuePolicy != null) {
						throw new IllegalStateException(
								"Found more than one policy while trying to validate user's password. Please check your configuration");
					}

					PrismObject<OrgType> org = (PrismObject<OrgType>) resolver.resolve(orgRefValue,
							"resolving parent org ref", null, null, result);
					orgs.add(org);
					valuePolicy = resolvePolicy(org, result);

				}
			}
		} catch (ObjectNotFoundException ex) {
			throw new IllegalStateException(ex);
		}
		// go deeper
		if (valuePolicy == null) {
			for (PrismObject<OrgType> orgType : orgs) {
				valuePolicy = determineValuePolicy(orgType, result);
				if (valuePolicy != null){
					return valuePolicy;
				}
			}
		}
		return valuePolicy;
	}
	
	private ValuePolicyType resolvePolicy(PrismObject<OrgType> org, OperationResult result)
			throws SchemaException {
		try {
			OrgType orgType = org.asObjectable();
			ObjectReferenceType ref = orgType.getPasswordPolicyRef();
			if (ref == null) {
				return null;
			}

			return resolver.resolve(ref, ValuePolicyType.class, null,
					"resolving password policy for organization", result);

		} catch (ObjectNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalStateException(e);
		}

	}
	
	<F extends ObjectType> void processPasswordPolicy(LensProjectionContext projectionContext, 
			LensContext<F> context, OperationResult result) throws SchemaException, PolicyViolationException{
		
		ObjectDelta accountDelta = projectionContext.getDelta();
		
		if (accountDelta == null){
			LOGGER.trace("Skipping processing password policies. Shadow delta not specified.");
			return;
		}
		
		if (ChangeType.DELETE == accountDelta.getChangeType()){
			return;
		}
		
		PrismObject<ShadowType> accountShadow = null;
		PrismProperty<PasswordType> password = null;
		if (ChangeType.ADD == accountDelta.getChangeType()){
			accountShadow = accountDelta.getObjectToAdd();
			if (accountShadow != null){
				password = accountShadow.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
				
			}
		}
		if (ChangeType.MODIFY == accountDelta.getChangeType() || password == null) {
			PropertyDelta<PasswordType> passwordValueDelta = null;
			if (accountDelta != null) {
				passwordValueDelta = accountDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
				// Modification sanity check
				if (accountDelta.getChangeType() == ChangeType.MODIFY && passwordValueDelta != null
						&& (passwordValueDelta.isAdd() || passwordValueDelta.isDelete())) {
					throw new SchemaException("Shadow password value cannot be added or deleted, it can only be replaced");
				}
				if (passwordValueDelta == null) {
					LOGGER.trace("Skipping processing password policies. Shadow delta does not contain password change.");
					return;
				}
				password = (PrismProperty<PasswordType>) passwordValueDelta.getItemNewMatchingPath(null);
			}
		}

//		PrismProperty<PasswordType> password = getPassword(projectionContext);
		ValuePolicyType passwordPolicy = null;
		if (isCheckOrgPolicy(context)){
			passwordPolicy = determineValuePolicy(context.getFocusContext().getObjectAny(), result);
			context.getFocusContext().setOrgPasswordPolicy(passwordPolicy);
		} else {
			passwordPolicy = projectionContext.getEffectivePasswordPolicy();
		}
		
		processPasswordPolicy(passwordPolicy, password, result);
	}
	
	private <F extends ObjectType> boolean isCheckOrgPolicy(LensContext<F> context) throws SchemaException{
		LensFocusContext focusCtx = context.getFocusContext();
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
	private String determinePasswordValue(PrismProperty<PasswordType> password) {
		if (password == null || password.getValue(ProtectedStringType.class) == null) {
			return "";
		}

		ProtectedStringType passValue = password.getValue(ProtectedStringType.class).getValue();

		if (passValue == null) {
			return "";
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

		return passwordStr != null ? passwordStr : "";
	}


}
