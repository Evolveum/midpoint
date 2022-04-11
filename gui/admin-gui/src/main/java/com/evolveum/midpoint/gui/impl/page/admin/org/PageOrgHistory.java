/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.org;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * Created by honchar.
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/orgHistoryNew", matchUrlForSecurity = "/admin/orgUnitHistory")
        },
        action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_ALL_URL,
                label = "PageAdminUsers.auth.orgAll.label",
                description = "PageAdminUsers.auth.orgAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_UNIT_HISTORY_URL,
                label = "PageOrgUnitHistory.auth.orgUnitHistory.label",
                description = "PageOrgUnitHistory.auth.orgUnitHistory.description")})
public class PageOrgHistory extends PageOrg {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageOrgHistory.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(PageOrgHistory.class);
    private String date = "";

    public PageOrgHistory(final PrismObject<OrgType> org, String date) {
        super(org);
        this.date = date;
    }

    @Override
    protected FocusDetailsModels<OrgType> createObjectDetailsModels(PrismObject<OrgType> object) {
        return new FocusDetailsModels<OrgType>(createPrismObjectModel(object), this) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean isReadonly() {
                return true;
            }
        };
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                String name = null;
                if (getModelObjectType() != null) {
                    name = WebComponentUtil.getName(getModelObjectType());
                }
                return createStringResource("PageUserHistory.title", name, date).getObject();
            }
        };
    }

}
