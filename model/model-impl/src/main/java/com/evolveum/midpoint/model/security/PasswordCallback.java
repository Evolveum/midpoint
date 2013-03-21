/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 Igor Farinic
 */
package com.evolveum.midpoint.model.security;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.model.security.api.UserDetailsService;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import org.apache.ws.security.WSPasswordCallback;

/**
 * @author Igor Farinic
 */
public class PasswordCallback implements CallbackHandler {

    private UserDetailsService userDetailsService;
    private Protector protector;

    public PasswordCallback(UserDetailsService userDetailsService, Protector protector) {
        this.userDetailsService = userDetailsService;
        this.protector = protector;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

        MidPointPrincipal user = userDetailsService.getUser(pc.getIdentifier());
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