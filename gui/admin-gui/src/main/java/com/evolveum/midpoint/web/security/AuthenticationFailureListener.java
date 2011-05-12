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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.util.FacesUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 *
 * @author Vilo Repan
 */
@Component
public class AuthenticationFailureListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {

    private static final Trace TRACE = TraceManager.getTrace(AuthenticationFailureListener.class);
    @Autowired
    private UserDetailsService userService;

    @Override
    public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent evt) {
        AuthenticationException ex = evt.getException();
        FacesUtils.addErrorMessage(ex.getMessage());
        
        String username = evt.getAuthentication().getName();
        if (userService == null) {
            TRACE.warn("Person repository is not available. Authentication failure for '" +
                    username + "' couldn't be logged.");
            return;
        }

        PrincipalUser user = userService.getUser(username);
        if (user == null) {
            return;
        }

        Credentials credentials = user.getCredentials();
        credentials.addFailedLogin();
        credentials.setLastFailedLoginAttempt(System.currentTimeMillis());

        userService.updateUser(user);
    }
}
