/*
 * Copyright (c) 2010-2013 Evolveum
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

import org.apache.wss4j.common.ext.WSPasswordCallback;

import java.io.IOException;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
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
 */
public class PasswordCallback implements CallbackHandler {
	
	private static final Trace LOGGER = TraceManager.getTrace(PasswordCallback.class);

    private UserProfileService userDetailsService;
    private Protector protector;

    public PasswordCallback(UserProfileService userDetailsService, Protector protector) {
        this.userDetailsService = userDetailsService;
        this.protector = protector;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
    	LOGGER.trace("Invoked PasswordCallback with {} callbacks: {}", callbacks.length, callbacks);
        WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

        String wssPasswordType = pc.getType();
        LOGGER.trace("Password type: {}", wssPasswordType);
        
        MidPointPrincipal user;
		try {
			user = userDetailsService.getPrincipal(pc.getIdentifier());
		} catch (ObjectNotFoundException e) {
			throw new SecurityException("unknown user", e);
		}
        UserType userType = user.getUser();
        CredentialsType credentials = userType.getCredentials();
        if (user != null && credentials != null && credentials.getPassword() != null 
        		&& credentials.getPassword().getValue() != null) {
            try {
            	PasswordType password = credentials.getPassword();
                pc.setPassword(protector.decryptString(password.getValue()));
            } catch (EncryptionException e) {
                throw new IOException(e);
            }
        } else {
            throw new SecurityException("unknown user");
        }
    }
}