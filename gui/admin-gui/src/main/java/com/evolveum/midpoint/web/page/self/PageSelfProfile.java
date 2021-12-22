/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.gui.api.page.PageBase;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.authentication.api.authorization.Url;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/self/profile")
        },
        action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_PROFILE_URL,
                label = "PageRoleSelfProfile.auth.profile.label",
                description = "PageRoleSelfProfile.auth.profile.description")})
public class PageSelfProfile extends PageBase {

    public PageSelfProfile() {
        getRequestCycle().setResponsePage(WebComponentUtil.resolveSelfPage());
    }

    public PageSelfProfile(PageParameters parameters) {
        super(parameters);
        getRequestCycle().setResponsePage(WebComponentUtil.resolveSelfPage());
    }

}
