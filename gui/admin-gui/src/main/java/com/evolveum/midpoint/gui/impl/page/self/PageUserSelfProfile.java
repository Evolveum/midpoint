/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self;

import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;

import com.evolveum.midpoint.gui.impl.page.admin.user.UserDetailsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.page.self.PageSelf;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/self/profile/user")
        },
        action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_PROFILE_URL,
                label = "PageUserSelfProfile.auth.profile.label",
                description = "PageUserSelfProfile.auth.profile.description")})
public class PageUserSelfProfile extends PageUser {

    private static final long serialVersionUID = 1L;

    public PageUserSelfProfile() {
        super();

    }

    public PageUserSelfProfile(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected String getObjectOidParameter() {
        return WebModelServiceUtils.getLoggedInFocusOid();
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("PageAdmin.menu.profile");
    }

    @Override
    protected UserDetailsModel createObjectDetailsModels(PrismObject<UserType> object) {
        UserDetailsModel userDetailsModel = super.createObjectDetailsModels(object);
        userDetailsModel.setSelfProfile(true);
        return userDetailsModel;
    }

    @Override
    protected void createBreadcrumb() {
        super.createBreadcrumb();

        Breadcrumb bc = getLastBreadcrumb();
        bc.setIcon(new Model(GuiStyleConstants.CLASS_OBJECT_USER_ICON));
    }
}
