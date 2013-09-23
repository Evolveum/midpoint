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

package com.evolveum.midpoint.model.lens.projector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.policy.PasswordPolicyUtils;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;


@Component
public class PasswordPolicyProcessor {
	
	private static final Trace LOGGER = TraceManager.getTrace(UserPolicyProcessor.class);
	
	@Autowired(required = true)
	Protector protector;

	
	void processPasswordPolicy(ValuePolicyType passwordPolicy, PrismProperty password, OperationResult result)
			throws PolicyViolationException, SchemaException {

		String passwordValue = determinePasswordValue(password);
		if (passwordPolicy == null) {
			LOGGER.trace("Skipping processing password policies. Password value or password policies not specified.");
			return;
		}
		
		if (password == null || password.isEmpty()){
//			throw new PolicyViolationException("Provided password is empty.");
            return;
		}

		boolean isValid = PasswordPolicyUtils.validatePassword(passwordValue, passwordPolicy, result);

		if (!isValid) {
			result.computeStatus();
			throw new PolicyViolationException("Provided password does not satisfy password policies. " + result.getMessage());

		}

	}
	
	<F extends FocusType> void processPasswordPolicy(LensFocusContext<F> focusContext, 
			LensContext<F> context, OperationResult result)
			throws PolicyViolationException, SchemaException {
		
//		PrismProperty<PasswordType> password = getPassword(focusContext);
		ObjectDelta userDelta = focusContext.getDelta();

		if (userDelta == null) {
			LOGGER.trace("Skipping processing password policies. User delta not specified.");
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
				// Modification sanity check
				if (userDelta.getChangeType() == ChangeType.MODIFY && passwordValueDelta != null
						&& (passwordValueDelta.isAdd() || passwordValueDelta.isDelete())) {
					throw new SchemaException("User password value cannot be added or deleted, it can only be replaced");
				}
				if (passwordValueDelta == null) {
					LOGGER.trace("Skipping processing password policies. User delta does not contain password change.");
					return ;
				}
				password = passwordValueDelta.getPropertyNew();
			}
		}
		
		ValuePolicyType passwordPolicy = context.getGlobalPasswordPolicy();
		
		processPasswordPolicy(passwordPolicy, password, result);

	}
	
	<F extends ObjectType> void processPasswordPolicy(LensProjectionContext projectionContext, 
			LensContext<F> context, OperationResult result) throws SchemaException, PolicyViolationException{
		
ObjectDelta accountDelta = projectionContext.getDelta();
		
		if (accountDelta == null){
			LOGGER.trace("Skipping processing password policies. User delta not specified.");
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
					throw new SchemaException("User password value cannot be added or deleted, it can only be replaced");
				}
				if (passwordValueDelta == null) {
					LOGGER.trace("Skipping processing password policies. User delta does not contain password change.");
					return;
				}
				password = passwordValueDelta.getPropertyNew();
			}
		}

//		PrismProperty<PasswordType> password = getPassword(projectionContext);
		
		ValuePolicyType passwordPolicy = projectionContext.getEffectivePasswordPolicy();
		
		processPasswordPolicy(passwordPolicy, password, result);
	}

//	private PrismProperty<PasswordType> getPassword(LensProjectionContext<AccountShadowType> projectionContext) throws SchemaException{
//		ObjectDelta accountDelta = projectionContext.getDelta();
//		
//		if (accountDelta == null){
//			LOGGER.trace("Skipping processing password policies. User delta not specified.");
//			return null;
//		}
//		
//		if (ChangeType.DELETE == accountDelta.getChangeType()){
//			return null;
//		}
//		
//		PrismObject<AccountShadowType> accountShadow = null;
//		PrismProperty<PasswordType> password = null;
//		if (ChangeType.ADD == accountDelta.getChangeType()){
//			accountShadow = accountDelta.getObjectToAdd();
//			if (accountShadow != null){
//				password = accountShadow.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
//				
//			}
//		}
//		if (ChangeType.MODIFY == accountDelta.getChangeType() || password == null) {
//			PropertyDelta<PasswordType> passwordValueDelta = null;
//			if (accountDelta != null) {
//				passwordValueDelta = accountDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
//				// Modification sanity check
//				if (accountDelta.getChangeType() == ChangeType.MODIFY && passwordValueDelta != null
//						&& (passwordValueDelta.isAdd() || passwordValueDelta.isDelete())) {
//					throw new SchemaException("User password value cannot be added or deleted, it can only be replaced");
//				}
//				if (passwordValueDelta == null) {
//					LOGGER.trace("Skipping processing password policies. User delta does not contain password change.");
//					return null;
//				}
//				password = passwordValueDelta.getPropertyNew();
//			}
//		}
//
//		return password;
//	}
	
//	private PrismProperty<PasswordType> getPassword(LensFocusContext<UserType> focusContext)
//			throws SchemaException {
//		
//
//		ObjectDelta userDelta = focusContext.getDelta();
//
//		if (userDelta == null) {
//			LOGGER.trace("Skipping processing password policies. User delta not specified.");
//			return null;
//		}
//
//		PrismProperty<PasswordType> password = null;
//		PrismObject<UserType> user = null;
//		if (ChangeType.ADD == userDelta.getChangeType()) {
//			user = focusContext.getDelta().getObjectToAdd();
//			if (user != null) {
//				password = user.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
//			}
//		} else if (ChangeType.MODIFY == userDelta.getChangeType()) {
//			PropertyDelta<PasswordType> passwordValueDelta = null;
//			if (userDelta != null) {
//				passwordValueDelta = userDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
//				// Modification sanity check
//				if (userDelta.getChangeType() == ChangeType.MODIFY && passwordValueDelta != null
//						&& (passwordValueDelta.isAdd() || passwordValueDelta.isDelete())) {
//					throw new SchemaException("User password value cannot be added or deleted, it can only be replaced");
//				}
//				if (passwordValueDelta == null) {
//					LOGGER.trace("Skipping processing password policies. User delta does not contain password change.");
//					return null;
//				}
//				password = passwordValueDelta.getPropertyNew();
//			}
//		}
//		return password;
//	}

	
	private String determinePasswordValue(PrismProperty<PasswordType> password) {
		// TODO: what to do if the provided password is null???
		if (password == null || password.getValue(ProtectedStringType.class) == null) {
			return null;
		}

		ProtectedStringType passValue = password.getValue(ProtectedStringType.class).getValue();

		if (passValue == null) {
			return null;
		}

		String passwordStr = passValue.getClearValue();

		if (passwordStr == null && passValue.getEncryptedData() != null) {
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
