/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.common.AvailableLocale;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
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
        if (locale == null || !AvailableLocale.containsLocale(locale)) {
            //default locale for web application
            setLocale(AvailableLocale.getDefaultLocale());
        }
        LOGGER.debug("Using {} as locale", getLocale());
    }

    public static MidPointAuthWebSession get() {
        return (MidPointAuthWebSession) Session.get();
    }

    @Override
    public Roles getRoles() {
        Roles roles = new Roles();
        //todo - used for wicket auth roles...
        MidPointPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal == null) {
            return roles;
        }
        for (Authorization authz : principal.getAuthorities()) {
            roles.addAll(authz.getAction());
        }

        return roles;
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
        MidPointPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal == null) {
            return;
        }

        if (principal instanceof GuiProfiledPrincipal guiProfiledPrincipal) {
            CompiledGuiProfile profile = guiProfiledPrincipal.getCompiledGuiProfile();
            if (profile.isInvalid()) {
                sessionStorage.clearPageStorage();
            }
        }

        //setting locale
        setLocale(WebComponentUtil.getLocale());
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
        DebugUtil.debugDumpWithLabel(sb, "sessionStorage", sessionStorage, indent + 1);
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
