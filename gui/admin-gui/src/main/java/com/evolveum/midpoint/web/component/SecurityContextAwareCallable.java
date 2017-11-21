/*
 * Copyright (c) 2010-2017 Evolveum
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
