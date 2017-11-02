package com.evolveum.midpoint.model.impl.security;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@Component("passwordAuthenticationEvaluator")
public class PasswordAuthenticationEvaluatorImpl extends AuthenticationEvaluatorImpl<PasswordType, PasswordAuthenticationContext>{

	@Override
	protected void checkEnteredCredentials(ConnectionEnvironment connEnv, PasswordAuthenticationContext authCtx) {
		if (StringUtils.isBlank(authCtx.getPassword())) {
			recordAuthenticationFailure(authCtx.getUsername(), connEnv, "empty password provided");
			throw new BadCredentialsException(messages.getMessage("web.security.provider.password.encoding"));
		}
	}

	@Override
	protected boolean suportsAuthzCheck() {
		return true;
	}

	@Override
	protected PasswordType getCredential(CredentialsType credentials) {
		return credentials.getPassword();
	}

	@Override
	protected void validateCredentialNotNull(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, PasswordType credential) {

		ProtectedStringType protectedString = credential.getValue();

		if (protectedString == null) {
			recordAuthenticationFailure(principal, connEnv, "no stored password value");
			throw new AuthenticationCredentialsNotFoundException(messages.getMessage("web.security.provider.password.bad"));
		}

	}

	@Override
	protected boolean passwordMatches(ConnectionEnvironment connEnv, MidPointPrincipal principal,
			PasswordType passwordType, PasswordAuthenticationContext authCtx) {
		return decryptAndMatch(connEnv, principal, passwordType.getValue(), authCtx.getPassword());
	}


	@Override
	protected CredentialPolicyType getEffectiveCredentialPolicy(SecurityPolicyType securityPolicy,
			PasswordAuthenticationContext authnCtx) throws SchemaException {
		return SecurityUtil.getEffectivePasswordCredentialsPolicy(securityPolicy);
	}

	@Override
	protected boolean supportsActivation() {
		return true;
	}


}
