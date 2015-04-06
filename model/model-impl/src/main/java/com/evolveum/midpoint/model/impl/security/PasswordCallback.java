/*
 * Copyright (c) 2010-2015 Evolveum
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

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.commons.lang.StringUtils;
import org.apache.wss4j.common.ext.WSPasswordCallback;

import java.io.IOException;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Igor Farinic
 * @author Radovan Semancik
 */
public class PasswordCallback implements CallbackHandler {
	
	private static final Trace LOGGER = TraceManager.getTrace(PasswordCallback.class);

    private UserProfileService userDetailsService;
    private Protector protector;
    private SecurityHelper securityHelper;

    public PasswordCallback(UserProfileService userDetailsService, Protector protector, SecurityHelper securityHelper) {
        this.userDetailsService = userDetailsService;
        this.protector = protector;
        this.securityHelper = securityHelper;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
    	LOGGER.trace("Invoked PasswordCallback with {} callbacks: {}", callbacks.length, callbacks);
        WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

        String username = pc.getIdentifier();
        String wssPasswordType = pc.getType();
        LOGGER.trace("Username: '{}', Password type: {}", username, wssPasswordType);
        
        if (StringUtils.isBlank(username)) {
        	securityHelper.auditLoginFailure(username, "No username", SchemaConstants.CHANNEL_WEB_SERVICE_URI);
        	throw new PasswordCallbackException("Authentication failed");
        }
        
        MidPointPrincipal user;
		try {
			user = userDetailsService.getPrincipal(username);
		} catch (ObjectNotFoundException e) {
			LOGGER.trace("User not found: {}", e.getMessage(), e);
			securityHelper.auditLoginFailure(username, "No user", SchemaConstants.CHANNEL_WEB_SERVICE_URI);
			// Do NOT attach the exception. We do not want to leak any information.
			throw new PasswordCallbackException("Authentication failed");
		}
		if (user == null) {
			securityHelper.auditLoginFailure(username, "No user", SchemaConstants.CHANNEL_WEB_SERVICE_URI);
			throw new PasswordCallbackException("Authentication failed");
		}
        UserType userType = user.getUser();
        CredentialsType credentials = userType.getCredentials();
        if (credentials == null) {
        	securityHelper.auditLoginFailure(username, "No user credentials", SchemaConstants.CHANNEL_WEB_SERVICE_URI);
        	throw new PasswordCallbackException("Authentication failed");
        }
        if (credentials.getPassword() == null) {
        	securityHelper.auditLoginFailure(username, "No user credentials password", SchemaConstants.CHANNEL_WEB_SERVICE_URI);
        	throw new PasswordCallbackException("Authentication failed");
        }
        if (credentials.getPassword().getValue() == null) {
        	securityHelper.auditLoginFailure(username, "No user credentials password value", SchemaConstants.CHANNEL_WEB_SERVICE_URI);
        	throw new PasswordCallbackException("Authentication failed");
        }
        try {
        	PasswordType password = credentials.getPassword();
            pc.setPassword(protector.decryptString(password.getValue()));
        } catch (EncryptionException e) {
        	LOGGER.error("Password decryption error: {}", e.getMessage(), e);
			securityHelper.auditLoginFailure(username, "Password decryption error: "+e.getMessage(), SchemaConstants.CHANNEL_WEB_SERVICE_URI);
			// Do NOT attach the exception. We do not want to leak any information.
			throw new PasswordCallbackException("Authentication failed");
        }
    }
}