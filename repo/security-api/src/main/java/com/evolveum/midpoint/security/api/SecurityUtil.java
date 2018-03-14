/**
 * Copyright (c) 2014-2017 Evolveum
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
package com.evolveum.midpoint.security.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author Radovan Semancik
 */
public class SecurityUtil {

	private static final Trace LOGGER = TraceManager.getTrace(SecurityUtil.class);

	@NotNull private static List<String> remoteHostAddressHeaders = Collections.emptyList();

	public static Collection<String> getActions(Collection<ConfigAttribute> configAttributes) {
		Collection<String> actions = new ArrayList<>(configAttributes.size());
		for (ConfigAttribute attr: configAttributes) {
			actions.add(attr.getAttribute());
		}
		return actions;
	}

	public static void logSecurityDeny(Object object, String message) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Denied access to {} by {} {}", object, getSubjectDescription(), message);
		}
	}
	
	public static void logSecurityDeny(MidPointPrincipal midPointPrincipal, Object object, String message) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Denied access to {} by {} {}", object, midPointPrincipal, message);
		}
	}

	public static void logSecurityDeny(Object object, String message, Throwable cause, Collection<String> requiredAuthorizations) {
		if (LOGGER.isDebugEnabled()) {
			String subjectDesc = getSubjectDescription();
			LOGGER.debug("Denied access to {} by {} {}", object, subjectDesc, message);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Denied access to {} by {} {}; one of the following authorization actions is required: "+requiredAuthorizations,
						object, subjectDesc, message, cause);
			}
		}
	}

	/**
	 * Returns short description of the subject suitable for log
	 * and error messages.
	 * Does not throw errors. Safe to toString-like methods.
	 * May return null (means anonymous or unknown)
	 */
	public static String getSubjectDescription() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return null;
		}
		Object principalObject = authentication.getPrincipal();
		if (principalObject == null) {
			return null;
		}
		if (!(principalObject instanceof MidPointPrincipal)) {
			return principalObject.toString();
		}
		return ((MidPointPrincipal)principalObject).getUsername();
	}

	public static <T> T getCredPolicyItem(CredentialPolicyType defaltCredPolicyType, CredentialPolicyType credPolicyType, Function<CredentialPolicyType, T> getter) {
		if (credPolicyType != null) {
			T val = getter.apply(credPolicyType);
			if (val != null) {
				return val;
			}
		}
		if (defaltCredPolicyType != null) {
			T val = getter.apply(defaltCredPolicyType);
			if (val != null) {
				return val;
			}
		}
		return null;
	}

	public static PasswordCredentialsPolicyType getEffectivePasswordCredentialsPolicy(SecurityPolicyType securityPolicy) {
		if (securityPolicy == null) {
			return null;
		}
		CredentialsPolicyType creds = securityPolicy.getCredentials();
		if (creds == null) {
			return null;
		}
		if (creds.getDefault() == null) {
			return creds.getPassword();
		}
		PasswordCredentialsPolicyType passPolicy = creds.getPassword();
		if (passPolicy == null) {
			passPolicy = new PasswordCredentialsPolicyType();
		} else {
			passPolicy = passPolicy.clone();
		}
		copyDefaults(creds.getDefault(), passPolicy);
		return passPolicy;
	}

	public static SecurityQuestionsCredentialsPolicyType getEffectiveSecurityQuestionsCredentialsPolicy(SecurityPolicyType securityPolicy) {
		if (securityPolicy == null) {
			return null;
		}
		CredentialsPolicyType creds = securityPolicy.getCredentials();
		if (creds == null) {
			return null;
		}
		if (creds.getDefault() == null) {
			return creds.getSecurityQuestions();
		}
		SecurityQuestionsCredentialsPolicyType securityQuestionsPolicy = creds.getSecurityQuestions();
		if (securityQuestionsPolicy == null) {
			securityQuestionsPolicy = new SecurityQuestionsCredentialsPolicyType();
		} else {
			securityQuestionsPolicy = securityQuestionsPolicy.clone();
		}
		copyDefaults(creds.getDefault(), securityQuestionsPolicy);
		return securityQuestionsPolicy;
	}


	public static List<NonceCredentialsPolicyType> getEffectiveNonceCredentialsPolicies(SecurityPolicyType securityPolicy) {
		if (securityPolicy == null) {
			return null;
		}
		CredentialsPolicyType creds = securityPolicy.getCredentials();
		if (creds == null) {
			return null;
		}
		if (creds.getDefault() == null) {
			return creds.getNonce();
		}
		List<NonceCredentialsPolicyType> existingNoncePolicies = creds.getNonce();
		List<NonceCredentialsPolicyType> newNoncePolicies = new ArrayList<>(existingNoncePolicies.size());
		for(NonceCredentialsPolicyType noncePolicy: existingNoncePolicies) {
			NonceCredentialsPolicyType newNoncePolicy = noncePolicy.clone();
			copyDefaults(creds.getDefault(), newNoncePolicy);
			newNoncePolicies.add(newNoncePolicy);
		}
		return newNoncePolicies;
	}

	public static NonceCredentialsPolicyType getEffectiveNonceCredentialsPolicy(SecurityPolicyType securityPolicy) throws SchemaException {
		List<NonceCredentialsPolicyType> noncePolicies = getEffectiveNonceCredentialsPolicies(securityPolicy);
		if (CollectionUtils.isEmpty(noncePolicies)) {
			return null;
		}
		if (noncePolicies.size() > 1) {
			throw new SchemaException("More than one nonce policy");
		}
		return noncePolicies.get(0);
	}

	private static void copyDefaults(CredentialPolicyType defaults,
			CredentialPolicyType target) {
		// Primitive, but efficient
		if (target.getHistoryLength() == null && defaults.getHistoryLength() != null) {
			target.setHistoryLength(defaults.getHistoryLength());
		}
		if (target.getHistoryStorageMethod() == null && defaults.getHistoryStorageMethod() != null) {
			target.setHistoryStorageMethod(defaults.getHistoryStorageMethod());
		}
		if (target.getLockoutDuration() == null && defaults.getLockoutDuration() != null) {
			target.setLockoutDuration(defaults.getLockoutDuration());
		}
		if (target.getLockoutFailedAttemptsDuration() == null && defaults.getLockoutFailedAttemptsDuration() != null) {
			target.setLockoutFailedAttemptsDuration(defaults.getLockoutFailedAttemptsDuration());
		}
		if (target.getLockoutMaxFailedAttempts() == null && defaults.getLockoutMaxFailedAttempts() != null) {
			target.setLockoutMaxFailedAttempts(defaults.getLockoutMaxFailedAttempts());
		}
		if (target.getMaxAge() == null && defaults.getMaxAge() != null) {
			target.setMaxAge(defaults.getMaxAge());
		}
		if (target.getMinAge() == null && defaults.getMinAge() != null) {
			target.setMinAge(defaults.getMinAge());
		}
		if (target.getPropagationUserControl() == null && defaults.getPropagationUserControl() != null) {
			target.setPropagationUserControl(defaults.getPropagationUserControl());
		}
		if (target.getResetMethod() == null && defaults.getResetMethod() != null) {
			target.setResetMethod(defaults.getResetMethod());
		}
		if (target.getStorageMethod() == null && defaults.getStorageMethod() != null) {
			target.setStorageMethod(defaults.getStorageMethod());
		}
		if (target.getWarningBeforeExpirationDuration() == null && defaults.getWarningBeforeExpirationDuration() != null) {
			target.setWarningBeforeExpirationDuration(defaults.getWarningBeforeExpirationDuration());
		}
	}

	public static int getCredentialHistoryLength(CredentialPolicyType credentialPolicy) {
		if (credentialPolicy == null) {
			return 0;
		}
		Integer historyLength = credentialPolicy.getHistoryLength();
		if (historyLength == null) {
			return 0;
		}
		return historyLength;
	}

	public static CredentialsStorageTypeType getCredentialStorageTypeType(CredentialsStorageMethodType storageMethod) {
		if (storageMethod == null) {
			return null;
		}
		return storageMethod.getStorageType();
	}

	/**
	 * Not very systematic. Used mostly in hacks.
	 */
	public static ValuePolicyType getPasswordPolicy(SecurityPolicyType securityPolicy) {
		if (securityPolicy == null) {
			return null;
		}
		CredentialsPolicyType creds = securityPolicy.getCredentials();
		if (creds == null) {
			return null;
		}
		PasswordCredentialsPolicyType passd = creds.getPassword();
		if (passd == null) {
			return null;
		}
		ObjectReferenceType valuePolicyRef = passd.getValuePolicyRef();
		if (valuePolicyRef == null) {
			return null;
		}
		PrismObject<ValuePolicyType> policyObj = valuePolicyRef.asReferenceValue().getObject();
		if (policyObj == null) {
			return null;
		}
		return policyObj.asObjectable();
	}

	// This is a bit of hack. We don't have an access to system configuration object from the static context,
	// so we rely on the upper layers to provide 'client address' header list here when the configuration object changes.
	// A more serious solution would be moving getCurrentConnectionInformation out of static context (perhaps into
	// securityEnforcer). But this would mean the easiness of its use would be gone...
	@SuppressWarnings("NullableProblems")
	public static void setRemoteHostAddressHeaders(SystemConfigurationType config) {
		List<String> newValue = config != null && config.getInfrastructure() != null ?
				new ArrayList<>(config.getInfrastructure().getRemoteHostAddressHeader()) :
				Collections.emptyList();
		if (!MiscUtil.unorderedCollectionEquals(remoteHostAddressHeaders, newValue)) {
			LOGGER.debug("Setting new value for 'remoteHostAddressHeaders': {}", newValue);
		}
		remoteHostAddressHeaders = newValue;
	}

	/**
	 * Returns current connection information, as derived from HTTP request stored in current thread.
	 * May be null if the thread is not associated with any HTTP request (e.g. task threads, operations invoked from GUI but executing in background).
	 */
	public static HttpConnectionInformation getCurrentConnectionInformation() {
		RequestAttributes attr = RequestContextHolder.getRequestAttributes();
		if (!(attr instanceof ServletRequestAttributes)) {
			return null;
		}
		ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) attr;
		HttpServletRequest request = servletRequestAttributes.getRequest();
		if (request == null) {
			return null;
		}
		HttpConnectionInformation rv = new HttpConnectionInformation();
		HttpSession session = request.getSession(false);
		if (session != null) {
			rv.setSessionId(session.getId());
		}
		rv.setLocalHostName(request.getLocalName());
		rv.setRemoteHostAddress(getRemoteHostAddress(request));
		return rv;
	}

	private static String getRemoteHostAddress(HttpServletRequest request) {
		for (String headerName : remoteHostAddressHeaders) {
			String header = request.getHeader(headerName);
			if (header != null) {
				return getAddressFromHeader(headerName, header);
			}
		}
		return request.getRemoteAddr();
	}

	// TODO implement various methods if necessary
	private static String getAddressFromHeader(String name, String value) {
		String[] split = StringUtils.split(value, ",");
		return StringUtils.trim(split[0]);
	}

	/**
	 * Returns principal representing currently logged-in user. Returns null if the user is anonymous.
	 */
	public static MidPointPrincipal getPrincipal() throws SecurityViolationException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			SecurityViolationException ex = new SecurityViolationException("No authentication");
			LOGGER.error("No authentication", ex);
			throw ex;
		}
		Object principalObject = authentication.getPrincipal();
		if (!(principalObject instanceof MidPointPrincipal)) {
			if (authentication.getPrincipal() instanceof String && "anonymousUser".equals(principalObject)) {
				return null;
			} else {
				throw new IllegalArgumentException("Expected that spring security principal will be of type "+
					MidPointPrincipal.class.getName()+" but it was "+ MiscUtil.getObjectName(principalObject));
			}
		}
		return (MidPointPrincipal) principalObject;
	}

	public static boolean isAuthenticated() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return (authentication != null);
	}
}
