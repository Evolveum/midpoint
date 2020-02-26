/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;

import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Viliam Repan (lazyman)
 * @author Radovan Semancik
 */
@PageDescriptor(url = {"/self/profile/user"}, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_PROFILE_URL,
                label = "PageSelfProfile.auth.profile.label",
                description = "PageSelfProfile.auth.profile.description")})
public class PageUserSelfProfile extends PageUser {

    private PageAdminFocus page;

    public PageUserSelfProfile() {
        super();
        page = new PageUser();
        setResponsePage(page);
    }

    public PageUserSelfProfile(PageParameters parameters) {
        super(parameters);
        page = new PageUser(parameters);
        setResponsePage(page);
    }

    public PageUserSelfProfile(PrismObject<UserType> user) {
        super(user);
        page = new PageUser(user);
        setResponsePage(page);
    }

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(PageUserSelfProfile.class);

    @Override
    protected String getObjectOidParameter() {
        return WebModelServiceUtils.getLoggedInFocusOid();
    }


    @Override
    protected boolean isSelfProfile(){
        return true;
    }


    @Override
    protected void createBreadcrumb() {
        super.createBreadcrumb();

        Breadcrumb bc = getLastBreadcrumb();
        bc.setIcon(new Model(GuiStyleConstants.CLASS_OBJECT_USER_ICON));
    }
}
