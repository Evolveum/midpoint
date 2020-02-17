/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.gui.api.page.PageBase;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;

/**
 * @author Viliam Repan (lazyman)
 * @author Radovan Semancik
 */
@PageDescriptor(url = {"/self/profile"}, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_PROFILE_URL,
                label = "PageSelfProfile.auth.profile.label",
                description = "PageSelfProfile.auth.profile.description")})
public class PageSelfProfile extends PageBase {

    private static final long serialVersionUID = 1L;

    private PageAdminFocus page;

    public PageSelfProfile() {
        page = resolvePage();
        setResponsePage(page);
    }

    public PageSelfProfile(PageParameters parameters) {
        page = resolvePage(parameters);
        setResponsePage(page);
    }

    public PageSelfProfile(PrismObject<? extends FocusType> focus) {
        page = resolvePage(focus);
        setResponsePage(page);
    }

    private PageAdminFocus resolvePage() {
        FocusType focusType = WebModelServiceUtils.getLoggedInFocus();
        if (focusType instanceof UserType) {
            return new PageUserSelfProfile();
        }
        if (focusType instanceof OrgType) {
            return new PageOrgSelfProfile();
        }
        if (focusType instanceof RoleType) {
            return new PageRoleSelfProfile();
        }
        if (focusType instanceof ServiceType) {
            return new PageServiceSelfProfile();
        }
        return null;
    }

    private PageAdminFocus resolvePage(PageParameters parameters) {
        FocusType focusType = WebModelServiceUtils.getLoggedInFocus();
        if (focusType instanceof UserType) {
            return new PageUserSelfProfile(parameters);
        }
        if (focusType instanceof OrgType) {
            return new PageOrgSelfProfile(parameters);
        }
        if (focusType instanceof RoleType) {
            return new PageRoleSelfProfile(parameters);
        }
        if (focusType instanceof ServiceType) {
            return new PageServiceSelfProfile(parameters);
        }
        return null;
    }

    private PageAdminFocus resolvePage(PrismObject<? extends FocusType> focus) {
        FocusType focusType = WebModelServiceUtils.getLoggedInFocus();
        if (focusType instanceof UserType) {
            return new PageUserSelfProfile((PrismObject<UserType>) focus);
        }
        if (focusType instanceof OrgType) {
            return new PageOrgSelfProfile((PrismObject<OrgType>) focus);
        }
        if (focusType instanceof RoleType) {
            return new PageRoleSelfProfile((PrismObject<RoleType>) focus);
        }
        if (focusType instanceof ServiceType) {
            return new PageServiceSelfProfile((PrismObject<ServiceType>) focus);
        }
        return null;
    }

}
