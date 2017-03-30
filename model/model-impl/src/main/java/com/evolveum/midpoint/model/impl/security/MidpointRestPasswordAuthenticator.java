package com.evolveum.midpoint.model.impl.security;

import java.io.IOException;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;

@Component
public class MidpointRestPasswordAuthenticator extends MidpointRestAuthenticator<PasswordAuthenticationContext>{

	
	@Autowired(required = true)
	private AuthenticationEvaluator<PasswordAuthenticationContext> passwordAuthenticationEvaluator;
	
	@Override
	protected AuthenticationEvaluator<PasswordAuthenticationContext> getAuthenticationEvaluator() {
		return passwordAuthenticationEvaluator;
	}

	@Override
	protected PasswordAuthenticationContext createAuthenticationContext(AuthorizationPolicy policy) throws IOException{
		return new PasswordAuthenticationContext(policy.getUserName(), policy.getPassword());
	}

}
