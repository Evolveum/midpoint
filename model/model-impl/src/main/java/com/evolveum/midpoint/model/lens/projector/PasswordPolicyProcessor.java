package com.evolveum.midpoint.model.lens.projector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.password.PasswordPolicyUtils;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.PasswordPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;


@Component
public class PasswordPolicyProcessor {
	
	private static final Trace LOGGER = TraceManager.getTrace(UserPolicyProcessor.class);
	
	@Autowired(required = true)
	Protector protector;

	
	void processPasswordPolicy(PasswordPolicyType passwordPolicy, PrismProperty password, OperationResult result)
			throws PolicyViolationException, SchemaException {

		String passwordValue = determinePasswordValue(password);
		if (passwordValue == null || passwordPolicy == null) {
			LOGGER.trace("Skipping processing password policies. Password value or password policies not specified.");
			return;
		}

		boolean isValid = PasswordPolicyUtils.validatePassword(passwordValue, passwordPolicy, result);

		if (!isValid) {
			throw new PolicyViolationException("Provided password does not satisfy password policies.");

		}

	}
	
	<F extends ObjectType, P extends ObjectType> void processPasswordPolicy(LensFocusContext<UserType> focusContext, LensContext<F,P> context, OperationResult result)
			throws PolicyViolationException, SchemaException {
		
		PrismProperty<PasswordType> password = getPassword(focusContext);
		
		PasswordPolicyType passwordPolicy = context.getGlobalPasswordPolicy();
		
		processPasswordPolicy(passwordPolicy, password, result);

//		String passwordValue = determinePasswordValue(password);
//		
//		
//		
//		if (passwordValue == null || passwordPolicy == null) {
//			LOGGER.trace("Skipping processing password policies. Password value or password policies not specified.");
//			return;
//		}
//
//		boolean isValid = PasswordPolicyUtils.validatePassword(passwordValue, passwordPolicy, result);
//
//		if (!isValid) {
//			throw new PolicyViolationException("Provided password does not satisfy password policies.");
//
//		}

	}
	
	<F extends ObjectType, P extends ObjectType> void processPasswordPolicy(LensProjectionContext<AccountShadowType> projectionContext, LensContext<F,P> context, OperationResult result) throws SchemaException, PolicyViolationException{
		PrismProperty<PasswordType> password = getPassword(projectionContext);
		
		PasswordPolicyType passwordPolicy = projectionContext.getAccountPasswordPolicy();
		
		if (passwordPolicy == null){
			passwordPolicy = context.getGlobalPasswordPolicy();
		}
		
		processPasswordPolicy(passwordPolicy, password, result);
	}

	private PrismProperty<PasswordType> getPassword(LensProjectionContext<AccountShadowType> projectionContext) throws SchemaException{
		ObjectDelta accountDelta = projectionContext.getDelta();
		
		if (accountDelta == null){
			LOGGER.trace("Skipping processing password policies. User delta not specified.");
			return null;
		}
		
		if (ChangeType.DELETE == accountDelta.getChangeType()){
			return null;
		}
		
		PrismObject<AccountShadowType> accountShadow = null;
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
					return null;
				}
				password = passwordValueDelta.getPropertyNew();
			}
		}

		return password;
	}
	
	private PrismProperty<PasswordType> getPassword(LensFocusContext<UserType> focusContext)
			throws SchemaException {
		

		ObjectDelta userDelta = focusContext.getDelta();

		if (userDelta == null) {
			LOGGER.trace("Skipping processing password policies. User delta not specified.");
			return null;
		}

		PrismProperty<PasswordType> password = null;
		PrismObject<UserType> user = null;
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
					return null;
				}
				password = passwordValueDelta.getPropertyNew();
			}
		}
		return password;
	}

	
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
