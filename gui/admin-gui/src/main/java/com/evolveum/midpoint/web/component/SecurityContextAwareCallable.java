/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.ThreadContext;
import org.springframework.security.core.Authentication;

import java.util.concurrent.Callable;

/**
 * @author lazyman
 */
public abstract class SecurityContextAwareCallable<V> implements Callable<V> {

    private SecurityContextManager securityContextManager;
    private Authentication authentication;
    private HttpConnectionInformation connInfo;

    protected SecurityContextAwareCallable(SecurityContextManager manager, Authentication auth) {
        this(manager, auth, null);
    }

    protected SecurityContextAwareCallable(SecurityContextManager manager, Authentication auth, HttpConnectionInformation connInfo) {
        Validate.notNull(manager, "Security enforcer must not be null.");

        this.securityContextManager = manager;
        this.authentication = auth;
        this.connInfo = connInfo;
    }

    @Override
    public final V call() throws Exception {
        if (connInfo != null) {
            securityContextManager.storeConnectionInformation(connInfo);
        }

        securityContextManager.setupPreAuthenticatedSecurityContext(authentication);

        try {
            return callWithContextPrepared();
        } finally {
            securityContextManager.setupPreAuthenticatedSecurityContext((Authentication) null);
            securityContextManager.storeConnectionInformation(null);
        }
    }

    protected void setupContext(Application application, Session session) {
        if (!Application.exists() && application != null) {
            ThreadContext.setApplication(application);
        }
        if (!Session.exists() && session != null) {
            ThreadContext.setSession(session);
        }
    }

    public abstract V callWithContextPrepared() throws Exception;
}
