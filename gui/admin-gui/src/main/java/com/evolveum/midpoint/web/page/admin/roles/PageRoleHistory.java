/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

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
import com.evolveum.midpoint.web.page.admin.users.PageUserHistory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.apache.wicket.model.IModel;

/**
 * Created by honchar
 */
@PageDescriptor(url = "/admin/roleHistory", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL,
                label = "PageAdminRoles.auth.roleAll.label",
                description = "PageAdminRoles.auth.roleAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_HISTORY_URL,
                label = "PageRoleHistory.auth.roleHistory.label",
                description = "PageRoleHistory.auth.roleHistory.description")})
public class PageRoleHistory extends PageRole {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageUserHistory.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(PageUserHistory.class);
    private String date = "";

    public PageRoleHistory(final PrismObject<RoleType> role, String date) {
        super(role);
        this.date = date;
    }

    @Override
    protected PrismObjectWrapper<RoleType> loadObjectWrapper(PrismObject<RoleType> role, boolean isReadonly) {
        return super.loadObjectWrapper(role, true);
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

