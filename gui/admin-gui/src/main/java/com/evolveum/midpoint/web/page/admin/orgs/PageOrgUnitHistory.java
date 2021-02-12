/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.orgs;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * Created by honchar.
 */
@PageDescriptor(url = "/admin/orgUnitHistory", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_ALL_URL,
                label = "PageAdminUsers.auth.orgAll.label",
                description = "PageAdminUsers.auth.orgAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_UNIT_HISTORY_URL,
                label = "PageOrgUnitHistory.auth.orgUnitHistory.label",
                description = "PageOrgUnitHistory.auth.orgUnitHistory.description")})
public class PageOrgUnitHistory extends PageOrgUnit {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageOrgUnitHistory.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(PageOrgUnitHistory.class);
    private String date = "";

    public PageOrgUnitHistory(final PrismObject<OrgType> org, String date) {
        super(org);
        this.date = date;
    }

    @Override
    protected PrismObjectWrapper<OrgType> loadObjectWrapper(PrismObject<OrgType> org, boolean isReadonly) {
        return super.loadObjectWrapper(org, true);
    }

    @Override
    protected void setSummaryPanelVisibility(ObjectSummaryPanel summaryPanel) {
        summaryPanel.setVisible(true);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<String>() {
            @Override
            protected String load() {
                String name = null;
                if (getObjectWrapper() != null && getObjectWrapper().getObject() != null) {
                    name = WebComponentUtil.getName(getObjectWrapper().getObject());
                }
                return createStringResource("PageUserHistory.title", name, date).getObject();
            }
        };
    }

    @Override
    protected boolean isFocusHistoryPage(){
        return true;
    }
}
