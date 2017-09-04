package com.evolveum.midpoint.model.impl.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class MidPointLdapAuthenticationProvider extends LdapAuthenticationProvider{

	private static final Trace LOGGER = TraceManager.getTrace(MidPointLdapAuthenticationProvider.class);

	@Autowired private SecurityHelper securityHelper;

	public MidPointLdapAuthenticationProvider(LdapAuthenticator authenticator) {
		super(authenticator);
	}


	@Override
	protected DirContextOperations doAuthentication(UsernamePasswordAuthenticationToken authentication) {

		try {
		return super.doAuthentication(authentication);
		} catch (RuntimeException e) {
			LOGGER.error("Failed to authenticate user {}. Error: {}", authentication.getName(), e.getMessage(), e);
			securityHelper.auditLoginFailure(authentication.getName(), null, ConnectionEnvironment.create(SchemaConstants.CHANNEL_GUI_USER_URI), "bad credentials");
			throw e;
		}
	}


	@Override
	protected Authentication createSuccessfulAuthentication(UsernamePasswordAuthenticationToken authentication,
			UserDetails user) {
		Authentication authNCtx = super.createSuccessfulAuthentication(authentication, user);

		Object principal = authNCtx.getPrincipal();
		if (!(principal instanceof MidPointPrincipal)) {
			throw new BadCredentialsException("LdapAuthentication.incorrect.value");
		}
		MidPointPrincipal midPointPrincipal = (MidPointPrincipal) principal;
		UserType userType = midPointPrincipal.getUser();

		if (userType == null) {
			throw new BadCredentialsException("LdapAuthentication.bad.user");
		}

		securityHelper.auditLoginSuccess(userType, ConnectionEnvironment.create(SchemaConstants.CHANNEL_GUI_USER_URI));
		return authNCtx;
	}



}
