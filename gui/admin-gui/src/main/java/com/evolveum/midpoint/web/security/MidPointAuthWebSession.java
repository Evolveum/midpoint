/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.request.Request;
import org.apache.wicket.resource.loader.ComponentStringResourceLoader;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Locale;

/**
 * @author lazyman
 */
public class MidPointAuthWebSession extends AuthenticatedWebSession {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointAuthWebSession.class);
    @SpringBean(name = "midPointAuthenticationProvider")
    private AuthenticationProvider authenticationProvider;

    public MidPointAuthWebSession(Request request) {
        super(request);
        Injector.get().inject(this);

        if (getLocale() == null) {
            //default locale for web application
            setLocale(new Locale("en", "US"));
        }
    }

    @Override
    public Roles getRoles() {
        Roles roles = new Roles();
        //todo - used for wicket auth roles...
        return roles;
    }

    public static MidPointAuthWebSession getSession() {
        return (MidPointAuthWebSession) Session.get();
    }

    @Override
    public boolean authenticate(String username, String password) {
        LOGGER.debug("Authenticating '{}' {} password in web session.",
                new Object[]{username, (StringUtils.isEmpty(password) ? "without" : "with")});

        boolean authenticated;
        try {
            Authentication authentication = authenticationProvider.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            authenticated = authentication.isAuthenticated();
        } catch (AuthenticationException ex) {
        	ComponentStringResourceLoader comp = new ComponentStringResourceLoader();
            error(comp.loadStringResource(MidPointApplication.class, ex.getMessage(), getLocale(), "", ""));

            LOGGER.debug("Couldn't authenticate user.", ex);
            authenticated = false;
        }

        return authenticated;
    }
}
