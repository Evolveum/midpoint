/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;

/**
 * @author Viliam Repan (lazyman)
 * @author Radovan Semancik
 */
@PageDescriptor(url = {"/self/profile/org/unit"}, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_PROFILE_URL,
                label = "PageSelfProfile.auth.profile.label",
                description = "PageSelfProfile.auth.profile.description")})
public class PageOrgSelfProfile extends PageOrgUnit {

    public PageOrgSelfProfile() {
        super();
    }

    public PageOrgSelfProfile(PageParameters parameters) {
        super(parameters);
    }

    public PageOrgSelfProfile(PrismObject<OrgType> org) {
        super(org);
    }

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(PageOrgSelfProfile.class);

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
        bc.setIcon(new Model(GuiStyleConstants.CLASS_OBJECT_ORG_ICON));
    }

}
