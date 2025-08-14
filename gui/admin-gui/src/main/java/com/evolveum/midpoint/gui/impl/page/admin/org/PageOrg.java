/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.org;

import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.AbstractRoleDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.PageAbstractRole;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.impl.page.admin.org.component.OrgSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/org")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_ALL_URL,
                label = "PageAdminUsers.auth.orgAll.label",
                description = "PageAdminUsers.auth.orgAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_UNIT_URL,
                label = "PageOrgUnit.auth.orgUnit.label",
                description = "PageOrgUnit.auth.orgUnit.description") })
public class PageOrg extends PageAbstractRole<OrgType, AbstractRoleDetailsModel<OrgType>> {

    public PageOrg() {
        super();
    }

    public PageOrg(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageOrg(PrismObject<OrgType> org) {
        super(org);
    }

    @Override
    public Class<OrgType> getType() {
        return OrgType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<OrgType> summaryModel) {
        return new OrgSummaryPanel(id, summaryModel, getSummaryPanelSpecification());
    }
}
