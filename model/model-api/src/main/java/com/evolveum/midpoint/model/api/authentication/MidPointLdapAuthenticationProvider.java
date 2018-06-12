/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.api.authentication;

import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;

public class MidPointLdapAuthenticationProvider extends LdapAuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointLdapAuthenticationProvider.class);

    @Autowired
    private ModelAuditRecorder auditProvider;

    public MidPointLdapAuthenticationProvider(LdapAuthenticator authenticator) {
        super(authenticator);
    }

    @Override
    protected DirContextOperations doAuthentication(UsernamePasswordAuthenticationToken authentication) {

        try {
            return super.doAuthentication(authentication);
        } catch (InternalAuthenticationServiceException e) {
        	// This sometimes happens ... for unknown reasons the underlying libraries cannot
        	// figure out correct exception. Which results to wrong error message (MID-4518)
        	// So, be smart here and try to figure out correct error.
        	throw processInternalAuthenticationException(e, e);
        	
        } catch (RuntimeException e) {
            LOGGER.error("Failed to authenticate user {}. Error: {}", authentication.getName(), e.getMessage(), e);
            auditProvider.auditLoginFailure(authentication.getName(), null, ConnectionEnvironment.create(SchemaConstants.CHANNEL_GUI_USER_URI), "bad credentials");
            throw e;
        }
    }

    private RuntimeException processInternalAuthenticationException(InternalAuthenticationServiceException rootExeption, Throwable currentException) {
    	if (currentException instanceof javax.naming.AuthenticationException) {
    		String message = ((javax.naming.AuthenticationException)currentException).getMessage();
    		if (message.contains("error code 49")) {
    			// JNDI and Active Directory strike again
    			return new BadCredentialsException("Invalid username and/or password.", rootExeption);
    		}
    	}
    	Throwable cause = currentException.getCause();
    	if (cause == null) {
    		return rootExeption;
    	} else {
    		return processInternalAuthenticationException(rootExeption, cause);
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

        auditProvider.auditLoginSuccess(userType, ConnectionEnvironment.create(SchemaConstants.CHANNEL_GUI_USER_URI));
        return authNCtx;
    }
}
