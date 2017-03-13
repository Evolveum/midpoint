/**
 * Copyright (c) 2016-2017 Evolveum
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
package com.evolveum.midpoint.model.impl.security;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collection;

/**
 * @author semancik
 *
 */
@Component("authenticationEvaluator")
public class AuthenticationEvaluatorImpl implements AuthenticationEvaluator {
	
	private static final Trace LOGGER = TraceManager.getTrace(AuthenticationEvaluatorImpl.class);
	
	@Autowired
	private Protector protector;
	
	@Autowired
	private Clock clock;
	
	// Has to be package-private so the tests can manipulate it
	@Autowired
	UserProfileService userProfileService;
	
	@Autowired
	private SecurityHelper securityHelper;
	
	@Override
	public UsernamePasswordAuthenticationToken authenticateUserPassword(ConnectionEnvironment connEnv, String enteredUsername, String enteredPassword) 
			throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException, 
			CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException {		
		if (StringUtils.isBlank(enteredPassword)) {
			recordAuthenticationFailure(enteredUsername, connEnv, "empty password provided");
			throw new BadCredentialsException("web.security.provider.password.encoding");
		}
		
		MidPointPrincipal principal = getAndCheckPrincipal(connEnv, enteredUsername, true);
		
		UserType userType = principal.getUser();
		CredentialsType credentials = userType.getCredentials();
		if (credentials == null) {
			recordAuthenticationFailure(principal, connEnv, "no credentials in user");
			throw new AuthenticationCredentialsNotFoundException("web.security.provider.invalid");
		}
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
			recordAuthenticationFailure(principal, connEnv, "password locked-out");
			throw new LockedException("web.security.provider.locked");
		}

		// Authorizations
		if (!hasAnyAuthorization(principal)) {
			recordAuthenticationFailure(principal, connEnv, "no authorizations");
			throw new DisabledException("web.security.provider.access.denied");
		}
		
		// Password age
		checkPasswordValidityAndAge(connEnv, principal, passwordType.getValue(), passwordType.getMetadata(), passwordCredentialsPolicy);
		
		if (passwordMatches(connEnv, principal, passwordType, enteredPassword)) {
			
			recordPasswordAuthenticationSuccess(principal, connEnv, passwordType, passwordCredentialsPolicy);
			UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(principal, 
					enteredPassword, principal.getAuthorities());
			return token;
			
		} else {
			recordPasswordAuthenticationFailure(principal, connEnv, passwordType, passwordCredentialsPolicy, "password mismatch");
			
			throw new BadCredentialsException("web.security.provider.invalid");
		}
	}
	
	@Override
	public UsernamePasswordAuthenticationToken authenticateUserNonce(ConnectionEnvironment connEnv, String enteredUsername, String enteredNonce, NonceCredentialsPolicyType noncePolicy) 
			throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException, 
			CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException {		
		if (StringUtils.isBlank(enteredNonce)) {
			recordAuthenticationFailure(enteredUsername, connEnv, "empty password provided");
			throw new BadCredentialsException("web.security.provider.password.encoding");
		}
		
		MidPointPrincipal principal = getAndCheckPrincipal(connEnv, enteredUsername, false);
		
		UserType userType = principal.getUser();
		CredentialsType credentials = userType.getCredentials();
		if (credentials == null) {
			recordAuthenticationFailure(principal, connEnv, "no credentials in user");
			throw new AuthenticationCredentialsNotFoundException("web.security.provider.invalid");
		}
		NonceType nonceType = credentials.getNonce();
		
		// Password age
		
		if (nonceType == null) {
			throw new BadCredentialsException("web.security.provider.invalid.link");
		}
		
		checkPasswordValidityAndAge(connEnv, principal, nonceType.getValue(), nonceType.getMetadata(), noncePolicy);
		
		if (nonceMatches(connEnv, principal, nonceType, enteredNonce)) {
			
			recordNonceAuthenticationSuccess(principal, connEnv, nonceType, noncePolicy);
			UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(principal, 
					credentials.getPassword() != null ? getPassword(connEnv, principal, credentials.getPassword().getValue()) : enteredNonce, principal.getAuthorities());
			return token;
			
		} else {
//			recordPasswordAuthenticationFailure(principal, connEnv, nonceType, noncePolicy, "password mismatch");
			//TODO what to do after expiration?
			throw new BadCredentialsException("web.security.provider.invalid");
		}
	}
	
	/**
	 * Special-purpose method used for Web Service authentication based on javax.security callbacks.
	 * 
	 * In that case there is no reasonable way how to reuse existing methods. Therefore this method is NOT part of the
	 * AuthenticationEvaluator interface. It is mostly a glue to make the old Java security code work.
	 */
	public String getAndCheckUserPassword(ConnectionEnvironment connEnv, String enteredUsername) 
			throws AuthenticationCredentialsNotFoundException, DisabledException, LockedException, 
			CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException {		

		MidPointPrincipal principal = getAndCheckPrincipal(connEnv, enteredUsername, true);
		
		UserType userType = principal.getUser();
		CredentialsType credentials = userType.getCredentials();
		if (credentials == null) {
			recordAuthenticationFailure(principal, connEnv, "no credentials in user");
			throw new AuthenticationCredentialsNotFoundException("web.security.provider.invalid");
		}
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
			recordAuthenticationFailure(principal, connEnv, "password locked-out");
			throw new LockedException("web.security.provider.locked");
		}

		// Authorizations
		if (!hasAnyAuthorization(principal)) {
			recordAuthenticationFailure(principal, connEnv, "no authorizations");
			throw new AccessDeniedException("web.security.provider.access.denied");
		}
		
		// Password age
		checkPasswordValidityAndAge(connEnv, principal, passwordType.getValue(), passwordType.getMetadata(), passwordCredentialsPolicy);
		
		return getPassword(connEnv, principal, passwordType.getValue());
	}
	
	@Override
	public PreAuthenticatedAuthenticationToken authenticateUserPreAuthenticated(ConnectionEnvironment connEnv,
			String enteredUsername) {
		
		MidPointPrincipal principal = getAndCheckPrincipal(connEnv, enteredUsername, true);
		
		// Authorizations
		if (!hasAnyAuthorization(principal)) {
			recordAuthenticationFailure(principal, connEnv, "no authorizations");
			throw new AccessDeniedException("web.security.provider.access.denied");
		}
		
		PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal, null, principal.getAuthorities());
		
		recordAuthenticationSuccess(principal, connEnv);
		
		return token;
	}

	@NotNull
	private MidPointPrincipal getAndCheckPrincipal(ConnectionEnvironment connEnv, String enteredUsername, boolean checkActivation) {
		
		if (StringUtils.isBlank(enteredUsername)) {
			recordAuthenticationFailure(enteredUsername, connEnv, "no username");
			throw new UsernameNotFoundException("web.security.provider.invalid");
		}
		
		MidPointPrincipal principal;
		try {
			principal = userProfileService.getPrincipal(enteredUsername);
		} catch (ObjectNotFoundException e) {
			recordAuthenticationFailure(enteredUsername, connEnv, "no user");
			throw new UsernameNotFoundException("web.security.provider.invalid");
		} catch (SchemaException e) {
			recordAuthenticationFailure(enteredUsername, connEnv, "schema error");
			throw new AccessDeniedException("web.security.provider.invalid");
		}
		
		
		if (principal == null) {
			recordAuthenticationFailure(enteredUsername, connEnv, "no user");
			throw new UsernameNotFoundException("web.security.provider.invalid");
		}

		if (checkActivation && !principal.isEnabled()) {
			recordAuthenticationFailure(principal, connEnv, "user disabled");
			throw new DisabledException("web.security.provider.disabled");
		}
		return principal;
	}
	
	private boolean hasAnyAuthorization(MidPointPrincipal principal) {
		Collection<Authorization> authorizations = principal.getAuthorities();
		if (authorizations == null || authorizations.isEmpty()){
			return false;
		}
		for (Authorization auth : authorizations){
			if (auth.getAction() != null && !auth.getAction().isEmpty()){
				return true;
			}
		}
		return false;
	}

	private <T extends CredentialPolicyType> void checkPasswordValidityAndAge(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, ProtectedStringType protectedString, MetadataType passwordMetadata, 
			T passwordCredentialsPolicy) {
		if (protectedString == null) {
			recordAuthenticationFailure(principal, connEnv, "no stored password value");
			throw new AuthenticationCredentialsNotFoundException("web.security.provider.password.bad");
		}
		if (passwordCredentialsPolicy == null) {
			return;
		}
		Duration maxAge = passwordCredentialsPolicy.getMaxAge();
		if (maxAge != null) {
			XMLGregorianCalendar changeTimestamp = MiscSchemaUtil.getChangeTimestamp(passwordMetadata);
			if (changeTimestamp != null) {
				XMLGregorianCalendar passwordValidUntil = XmlTypeConverter.addDuration(changeTimestamp, maxAge);
				if (clock.isPast(passwordValidUntil)) {
					recordAuthenticationFailure(principal, connEnv, "password expired");
					throw new CredentialsExpiredException("web.security.provider.password.bad");
				}
			}
		}
	}
	
	private boolean nonceMatches(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, NonceType nonceType,
			String enteredNonce) {
		String decryptedPassword = getPassword(connEnv, principal, nonceType.getValue());
		return enteredNonce.equals(decryptedPassword);
	}

	private boolean passwordMatches(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, PasswordType passwordType,
			String enteredPassword) {
		String decryptedPassword = getPassword(connEnv, principal, passwordType.getValue());
		return enteredPassword.equals(decryptedPassword);
	}
	
	private String getPassword(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, ProtectedStringType protectedString) {
		String decryptedPassword;
		if (protectedString.getEncryptedDataType() != null) {
			try {
				decryptedPassword = protector.decryptString(protectedString);
			} catch (EncryptionException e) {
				recordAuthenticationFailure(principal, connEnv, "error decrypting password: "+e.getMessage());
				throw new AuthenticationServiceException("web.security.provider.unavailable", e);
			}
		} else {
			LOGGER.warn("Authenticating user based on clear value. Please check objects, "
					+ "this should not happen. Protected string should be encrypted.");
			decryptedPassword = protectedString.getClearValue();
		}
		return decryptedPassword;
	}

	private boolean isLockedOut(AbstractCredentialType credentialsType, CredentialPolicyType credentialsPolicy) {
		return isOverFailedLockoutAttempts(credentialsType, credentialsPolicy) && !isLockoutExpired(credentialsType, credentialsPolicy);
	}
	
	private boolean isOverFailedLockoutAttempts(AbstractCredentialType credentialsType, CredentialPolicyType credentialsPolicy) {
		int failedLogins = credentialsType.getFailedLogins() != null ? credentialsType.getFailedLogins() : 0;
		return isOverFailedLockoutAttempts(failedLogins, credentialsPolicy);
	}
	
	private boolean isOverFailedLockoutAttempts(int failedLogins, CredentialPolicyType credentialsPolicy) {
		return credentialsPolicy != null && credentialsPolicy.getLockoutMaxFailedAttempts() != null &&
				credentialsPolicy.getLockoutMaxFailedAttempts() > 0 && failedLogins >= credentialsPolicy.getLockoutMaxFailedAttempts();
	}
		
	private boolean isLockoutExpired(AbstractCredentialType credentialsType, CredentialPolicyType credentialsPolicy) {
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

	private void recordPasswordAuthenticationSuccess(MidPointPrincipal principal, ConnectionEnvironment connEnv,
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
		
		ActivationType activation = principal.getUser().getActivation();
		if (activation != null) {
			activation.setLockoutStatus(LockoutStatusType.NORMAL);
			activation.setLockoutExpirationTimestamp(null);
		}

		userProfileService.updateUser(principal);
		
		recordAuthenticationSuccess(principal, connEnv);
	}
	
	private void recordNonceAuthenticationSuccess(MidPointPrincipal principal, ConnectionEnvironment connEnv,
			NonceType nonceType, NonceCredentialsPolicyType passwordCredentialsPolicy) {
		
		LoginEventType event = new LoginEventType();
		event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
		event.setFrom(connEnv.getRemoteHost());

		ActivationType activation = principal.getUser().getActivation();
		if (activation != null) {
			activation.setLockoutStatus(LockoutStatusType.NORMAL);
			activation.setLockoutExpirationTimestamp(null);
		}

		userProfileService.updateUser(principal);
		
		recordAuthenticationSuccess(principal, connEnv);
	}
	
	private void recordAuthenticationSuccess(@NotNull MidPointPrincipal principal, @NotNull ConnectionEnvironment connEnv) {
		securityHelper.auditLoginSuccess(principal.getUser(), connEnv);
	}
	
	private void recordPasswordAuthenticationFailure(@NotNull MidPointPrincipal principal, @NotNull ConnectionEnvironment connEnv,
			@NotNull PasswordType passwordType, PasswordCredentialsPolicyType passwordCredentialsPolicy, String reason) {
		Integer failedLogins = passwordType.getFailedLogins();
		LoginEventType lastFailedLogin = passwordType.getLastFailedLogin();
		XMLGregorianCalendar lastFailedLoginTs = null;
		if (lastFailedLogin != null) {
			lastFailedLoginTs = lastFailedLogin.getTimestamp();
		}
		
		if (passwordCredentialsPolicy != null) {
			Duration lockoutFailedAttemptsDuration = passwordCredentialsPolicy.getLockoutFailedAttemptsDuration();
			if (lockoutFailedAttemptsDuration != null) {
				if (lastFailedLoginTs != null) {
					XMLGregorianCalendar failedLoginsExpirationTs = XmlTypeConverter.addDuration(lastFailedLoginTs, lockoutFailedAttemptsDuration);
					if (clock.isPast(failedLoginsExpirationTs)) {
						failedLogins = 0;
					}
				}
			}
		}
		if (failedLogins == null) {
			failedLogins = 1;
		} else {
			failedLogins++;
		}
		
		passwordType.setFailedLogins(failedLogins);
		
		LoginEventType event = new LoginEventType();
		event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
		event.setFrom(connEnv.getRemoteHost());
		
		passwordType.setLastFailedLogin(event);
		
		ActivationType activationType = principal.getUser().getActivation();
		
		if (failedLogins != null && isOverFailedLockoutAttempts(failedLogins, passwordCredentialsPolicy)) {
			if (activationType == null) {
				activationType = new ActivationType();
				principal.getUser().setActivation(activationType);
			}
			activationType.setLockoutStatus(LockoutStatusType.LOCKED);
			XMLGregorianCalendar lockoutExpirationTs = null;
			if (passwordCredentialsPolicy != null) {
				Duration lockoutDuration = passwordCredentialsPolicy.getLockoutDuration();
				if (lockoutDuration != null) {
					lockoutExpirationTs = XmlTypeConverter.addDuration(event.getTimestamp(), lockoutDuration);
				}
			}
			activationType.setLockoutExpirationTimestamp(lockoutExpirationTs);
		}
		
		userProfileService.updateUser(principal);
		
		recordAuthenticationFailure(principal, connEnv, reason);
	}
	
	private void recordAuthenticationFailure(@NotNull MidPointPrincipal principal, ConnectionEnvironment connEnv, String reason) {
		securityHelper.auditLoginFailure(principal.getUsername(), principal.getUser(), connEnv, reason);
	}
	
	private void recordAuthenticationFailure(String username, ConnectionEnvironment connEnv, String reason) {
		securityHelper.auditLoginFailure(username, null, connEnv, reason);
	}
		
}
