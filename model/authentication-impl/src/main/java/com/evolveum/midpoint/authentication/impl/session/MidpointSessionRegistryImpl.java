/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.session;

import com.evolveum.midpoint.authentication.api.MidpointSessionRegistry;
import com.evolveum.midpoint.authentication.api.RemoveUnusedSecurityFilterPublisher;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.AbstractSessionEvent;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * @author skublik
 */

public class MidpointSessionRegistryImpl extends SessionRegistryImpl implements MidpointSessionRegistry {

    private final RemoveUnusedSecurityFilterPublisher removeUnusedSecurityFilterPublisher;
    private boolean getLoggedInUsersProcessing = false;

    public MidpointSessionRegistryImpl(RemoveUnusedSecurityFilterPublisher removeUnusedSecurityFilterPublisher){
        this.removeUnusedSecurityFilterPublisher = removeUnusedSecurityFilterPublisher;
    }

    @Override
    public void onApplicationEvent(AbstractSessionEvent event) {
        super.onApplicationEvent(event);
        if (event instanceof SessionDestroyedEvent) {
            SessionDestroyedEvent sessionEvent = (SessionDestroyedEvent) event;
            for (SecurityContext context : sessionEvent.getSecurityContexts()) {
                if (context != null && context.getAuthentication() != null
                        && context.getAuthentication() instanceof MidpointAuthentication) {
                    removeUnusedSecurityFilterPublisher.publishCustomEvent(
                            ((MidpointAuthentication)context.getAuthentication()).getAuthModules());
                }
            }
        }
    }

    public SessionInformation getSessionInformation(String sessionId) {
        if (getLoggedInUsersProcessing) {
            return super.getSessionInformation(sessionId);
        }

        HttpServletRequest request = getRequest();
        if (AuthSequenceUtil.isRecordSessionLessAccessChannel(request)) {
            return null;
        }
        return super.getSessionInformation(sessionId);
    }

    private HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes)requestAttributes).getRequest();
        }
        return null;
    }

    @Override
    public List<SessionInformation> getLoggedInUsersSession(Object principal) {
        synchronized (this) {
            getLoggedInUsersProcessing = true;
            try {
                return getAllSessions(principal, false);
            } finally {
                getLoggedInUsersProcessing = false;
            }
        }
    }
}
