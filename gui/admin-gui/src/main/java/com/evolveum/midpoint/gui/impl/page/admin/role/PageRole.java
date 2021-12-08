/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.focus.PageFocusDetails;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.authentication.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.PageDescriptor;
import com.evolveum.midpoint.authentication.api.Url;
import com.evolveum.midpoint.web.page.admin.roles.component.RoleSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleNew")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL, label = "PageAdminRoles.auth.roleAll.label", description = "PageAdminRoles.auth.roleAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_URL, label = "PageRole.auth.role.label", description = "PageRole.auth.role.description") })
public class PageRole extends PageFocusDetails<RoleType, FocusDetailsModels<RoleType>> {

    public PageRole() {
        super();
    }

    public PageRole(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageRole(PrismObject<RoleType> role) {
        super(role);
    }

    @Override
    public Class<RoleType> getType() {
        return RoleType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, LoadableModel<RoleType> summaryModel) {
        return new RoleSummaryPanel(id, summaryModel, getSummaryPanelSpecification());
    }
}
