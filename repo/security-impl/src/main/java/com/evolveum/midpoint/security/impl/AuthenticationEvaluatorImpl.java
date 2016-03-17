/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.security.impl;

import java.util.Collection;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCredentialPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCredentialType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoginEventType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 *
 */
public class AuthenticationEvaluatorImpl {
	
	private static final Trace LOGGER = TraceManager.getTrace(AuthenticationEvaluatorImpl.class);
	
	@Autowired(required = true)
	private Protector protector;
	
	@Autowired(required = true)
	private Clock clock;
	
	public Authentication authenticateUserPassword(MidPointPrincipal principal, ConnectionEnvironment connEnv, String enteredPassword) throws BadCredentialsException {		
		if (StringUtils.isBlank(enteredPassword)) {
			throw new BadCredentialsException("web.security.provider.access.denied");
		}
		
		if (principal == null || principal.getUser() == null || principal.getUser().getCredentials() == null) {
			throw new BadCredentialsException("web.security.provider.invalid");
		}

		if (!principal.isEnabled()) {
			throw new BadCredentialsException("web.security.provider.disabled");
		}
		
		UserType userType = principal.getUser();
		CredentialsType credentials = userType.getCredentials();
		PasswordType passwordType = credentials.getPassword();
		SecurityPolicyType securityPolicy = principal.getApplicableSecurityPolicy();
		PasswordCredentialsPolicyType passwordCredentialsPolicy = null;
		if (securityPolicy != null) {
			CredentialsPolicyType credentialsPolicyType = securityPolicy.getCredentials();
			if (credentialsPolicyType != null) {
				passwordCredentialsPolicy = credentialsPolicyType.getPassword();
			}
		}

		// Lockout
		if (isLockedOut(passwordType, passwordCredentialsPolicy)) {
			logFailure(principal, "password locked-out");
			throw new BadCredentialsException("web.security.provider.locked");
		}

		if (StringUtils.isEmpty(enteredPassword)) {
			logFailure(principal, "entered empty password");
			throw new BadCredentialsException("web.security.provider.password.encoding");
		}
		
		Collection<Authorization> authorizations = principal.getAuthorities();
		if (authorizations == null || authorizations.isEmpty()){
			logFailure(principal, "no authorizations");
			throw new BadCredentialsException("web.security.provider.access.denied");
		}
		
		for (Authorization auth : authorizations){
			if (auth.getAction() == null || auth.getAction().isEmpty()){
				logFailure(principal, "no authorization actions");
				throw new BadCredentialsException("web.security.provider.access.denied");
			}
		}

		checkPasswordValidityAndAge(principal, passwordType, passwordCredentialsPolicy);
		
		if (passwordMatches(principal, passwordType, passwordCredentialsPolicy, enteredPassword)) {
			
			recordAuthenticationSuccess(principal, connEnv, passwordType, passwordCredentialsPolicy);
			UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(principal, 
					enteredPassword, principal.getAuthorities());
			return token;
			
		} else {
			
			recordAuthenticationFailure(principal, connEnv, passwordType, passwordCredentialsPolicy);
			
			throw new BadCredentialsException("web.security.provider.invalid");
		}
	}
	
	private void checkPasswordValidityAndAge(MidPointPrincipal principal, PasswordType passwordType,
			PasswordCredentialsPolicyType passwordCredentialsPolicy) {
		ProtectedStringType protectedString = passwordType.getValue();
		if (protectedString == null) {
			logFailure(principal, "no stored password value");
			throw new BadCredentialsException("web.security.provider.password.bad");
		}
		if (passwordCredentialsPolicy == null) {
			return;
		}
		Duration maxAge = passwordCredentialsPolicy.getMaxAge();
		if (maxAge != null) {
			XMLGregorianCalendar changeTimestamp = MiscSchemaUtil.getChangeTimestamp(passwordType.getMetadata());
			if (changeTimestamp != null) {
				XMLGregorianCalendar passwordValidUntil = XmlTypeConverter.addDuration(changeTimestamp, maxAge);
				if (clock.isPast(passwordValidUntil)) {
					logFailure(principal, "password expired");
					throw new BadCredentialsException("web.security.provider.password.bad");
				}
			}
		}
	}

	private boolean passwordMatches(MidPointPrincipal principal, PasswordType passwordType,
			PasswordCredentialsPolicyType passwordCredentialsPolicy, String enteredPassword) {
		ProtectedStringType protectedString = passwordType.getValue();
		String decryptedPassword;
		if (protectedString.getEncryptedDataType() != null) {
			try {
				decryptedPassword = protector.decryptString(protectedString);
			} catch (EncryptionException e) {
				logFailure(principal, "error decrypting password: "+e.getMessage());
				throw new AuthenticationServiceException("web.security.provider.unavailable", e);
			}
		} else {
			LOGGER.warn("Authenticating user based on clear value. Please check objects, "
					+ "this should not happen. Protected string should be encrypted.");
			decryptedPassword = protectedString.getClearValue();
		}
		return (enteredPassword.equals(decryptedPassword));
	}

	private boolean isLockedOut(AbstractCredentialType credentialsType, AbstractCredentialPolicyType credentialsPolicy) {
		return isOverFailedLockoutAttempts(credentialsType, credentialsPolicy) && !isLockoutExpired(credentialsType, credentialsPolicy);
	}
	
	private boolean isOverFailedLockoutAttempts(AbstractCredentialType credentialsType, AbstractCredentialPolicyType credentialsPolicy) {
		int failedLogins = credentialsType.getFailedLogins() != null ? credentialsType.getFailedLogins() : 0;
		return credentialsPolicy != null && credentialsPolicy.getLockoutMaxFailedAttempts() != null &&
				credentialsPolicy.getLockoutMaxFailedAttempts() > 0 && failedLogins >= credentialsPolicy.getLockoutMaxFailedAttempts();
	}
	
	private boolean isLockoutExpired(AbstractCredentialType credentialsType, AbstractCredentialPolicyType credentialsPolicy) {
		Duration lockoutDuration = credentialsPolicy.getLockoutDuration();
		if (lockoutDuration == null) {
			return false;
		}
		LoginEventType lastFailedLogin = credentialsType.getLastFailedLogin();
		if (lastFailedLogin == null) {
			return true;
		}
		XMLGregorianCalendar lastFailedLoginTimestamp = lastFailedLogin.getTimestamp();
		if (lastFailedLoginTimestamp == null) {
			return true;
		}
		XMLGregorianCalendar lockedUntilTimestamp = XmlTypeConverter.addDuration(lastFailedLoginTimestamp, lockoutDuration);
		return clock.isPast(lockedUntilTimestamp);
	}

	private void recordAuthenticationSuccess(MidPointPrincipal principal, ConnectionEnvironment connEnv, 
			PasswordType passwordType, PasswordCredentialsPolicyType passwordCredentialsPolicy) {
		Integer failedLogins = passwordType.getFailedLogins();
		if (failedLogins != null && failedLogins > 0) {
			passwordType.setFailedLogins(0);
		}
		LoginEventType event = new LoginEventType();
		event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
		event.setFrom(connEnv.getRemoteHost());

		passwordType.setPreviousSuccessfulLogin(passwordType.getLastSuccessfulLogin());
		passwordType.setLastSuccessfulLogin(event);

		userProfileService.updateUser(principal);
		

	}
	
	private void recordAuthenticationFailure(MidPointPrincipal principal, ConnectionEnvironment connEnv,
			PasswordType passwordType, PasswordCredentialsPolicyType passwordCredentialsPolicy) {
		Integer failedLogins = passwordType.getFailedLogins();
		if (failedLogins == null) {
			passwordType.setFailedLogins(1);
		} else {
			passwordType.setFailedLogins(failedLogins + 1);
		}
		
		LoginEventType event = new LoginEventType();
		event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
		event.setFrom(connEnv.getRemoteHost());
		
		passwordType.setLastFailedLogin(event);
		userProfileService.updateUser(principal);

	}
	
	private void logFailure(MidPointPrincipal principal, String reason) {
		if (!LOGGER.isDebugEnabled()) {
			return;
		}
		LOGGER.debug("Password authentication of {} failed: {}", principal.getUser(), reason);
	}
	
}
