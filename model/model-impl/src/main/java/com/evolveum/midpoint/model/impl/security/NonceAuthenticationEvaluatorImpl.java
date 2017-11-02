package com.evolveum.midpoint.model.impl.security;

import org.apache.commons.lang.StringUtils;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.NonceAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

@Component("nonceAuthenticationEvaluator")
public class NonceAuthenticationEvaluatorImpl extends AuthenticationEvaluatorImpl<NonceType, NonceAuthenticationContext>{


	@Override
	protected void checkEnteredCredentials(ConnectionEnvironment connEnv,
			NonceAuthenticationContext authCtx) {
		if (StringUtils.isBlank(authCtx.getNonce())) {
			recordAuthenticationFailure(authCtx.getUsername(), connEnv, "empty password provided");
			throw new BadCredentialsException(messages.getMessage("web.security.provider.password.encoding"));
		}
	}

	@Override
	protected boolean suportsAuthzCheck() {
		return false;
	}

	@Override
	protected NonceType getCredential(CredentialsType credentials) {
		return credentials.getNonce();
	}

	@Override
	protected void validateCredentialNotNull(ConnectionEnvironment connEnv, MidPointPrincipal principal,
			NonceType credential) {
		if (credential.getValue() == null) {
			recordAuthenticationFailure(principal, connEnv, "no stored password value");
			throw new AuthenticationCredentialsNotFoundException(messages.getMessage("web.security.provider.password.bad"));
		}

	}

	@Override
	protected boolean passwordMatches(ConnectionEnvironment connEnv, MidPointPrincipal principal,
			NonceType passwordType, NonceAuthenticationContext authCtx) {
		return decryptAndMatch(connEnv, principal, passwordType.getValue(), authCtx.getNonce());
	}

	@Override
	protected CredentialPolicyType getEffectiveCredentialPolicy(SecurityPolicyType securityPolicy,
			NonceAuthenticationContext authnCtx) throws SchemaException {
		NonceCredentialsPolicyType policy = authnCtx.getPolicy();
		if (policy == null) {
			policy = SecurityUtil.getEffectiveNonceCredentialsPolicy(securityPolicy);
		}
		return policy;
	}



	@Override
	protected boolean supportsActivation() {
		return false;
	}
}
