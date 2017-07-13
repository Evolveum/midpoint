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

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.session.SessionStorage;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;

import java.util.Locale;

/**
 * @author lazyman
 */
public class MidPointAuthWebSession extends AuthenticatedWebSession implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointAuthWebSession.class);

    private SessionStorage sessionStorage;

    public MidPointAuthWebSession(Request request) {
        super(request);
        Injector.get().inject(this);

        Locale locale = getLocale();
        LOGGER.debug("Found locale {}", locale);
        if (locale == null || !MidPointApplication.containsLocale(locale)) {
            //default locale for web application
            setLocale(MidPointApplication.getDefaultLocale());
        }
        LOGGER.debug("Using {} as locale", getLocale());
    }

    @Override
    public Roles getRoles() {
        Roles roles = new Roles();
        //todo - used for wicket auth roles...
        MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
        if (principal == null) {
            return roles;
        }
        for (Authorization authz : principal.getAuthorities()) {
            roles.addAll(authz.getAction());
        }

        return roles;
    }

    public static MidPointAuthWebSession getSession() {
        return (MidPointAuthWebSession) Session.get();
    }

    @Override
    public boolean authenticate(String username, String password) {
        return false;
    }

    public SessionStorage getSessionStorage() {
        if (sessionStorage == null) {
            sessionStorage = new SessionStorage();
        }

        return sessionStorage;
    }

    public void setClientCustomization() {
        MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
        if (principal == null) {
            return;
        }

        //setting locale
        setLocale(WebModelServiceUtils.getLocale());
        LOGGER.debug("Using {} as locale", getLocale());

        //set time zone
        ClientProperties props = WebSession.get().getClientInfo().getProperties();
        props.setTimeZone(WebModelServiceUtils.getTimezone());

        LOGGER.debug("Using {} as time zone", props.getTimeZone());
    }

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("MidPointAuthWebSession\n");
		DebugUtil.debugDumpWithLabel(sb, "sessionStorage", sessionStorage, indent+1);
		return sb.toString();
	}
	
	public String dumpSizeEstimates(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.dumpObjectSizeEstimate(sb, "MidPointAuthWebSession", this, indent);
		if (sessionStorage != null) {
			sb.append("\n");
			sessionStorage.dumpSizeEstimates(sb, indent + 1);
		}
		return sb.toString();
	}
}
