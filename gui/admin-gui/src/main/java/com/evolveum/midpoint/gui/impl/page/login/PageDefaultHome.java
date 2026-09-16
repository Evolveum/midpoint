/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.security.api.AuthorizationConstants;

import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serial;

/**
 * Stable URL entry point for resolving the current user's default home page.
 *
 * Authentication code works with URL paths, while the real home page is
 * computed by MidPointApplication#getHomePage() after authentication.
 */
@PageDescriptor(
        urls = @Url(mountUrl = "/home/default", matchUrlForSecurity = "/home/default"),
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_HOME_ALL_URL),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_DASHBOARD_URL),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_ALL_URL),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_DASHBOARD_URL)
        })
public class PageDefaultHome extends PageBase {

    @Serial private static final long serialVersionUID = 1L;

    @Override
    protected void onInitialize() {
        super.onInitialize();
        CharSequence url = RequestCycle.get().urlFor(getMidpointApplication().getHomePage(), new PageParameters());
        throw new RedirectToUrlException(url.toString());
    }
}
