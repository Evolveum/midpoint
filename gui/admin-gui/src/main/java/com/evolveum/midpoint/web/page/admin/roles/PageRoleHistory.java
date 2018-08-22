/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.users.PageUserHistory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar
 */
@PageDescriptor(url = "/admin/roleHistory", action = {
        @AuthorizationAction(actionUri = PageAdminRoles.AUTH_ROLE_ALL,
                label = PageAdminRoles.AUTH_ROLE_ALL_LABEL,
                description = PageAdminRoles.AUTH_ROLE_ALL_DESCRIPTION),
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
    protected ObjectWrapper<RoleType> loadObjectWrapper(PrismObject<RoleType> role, boolean isReadonly) {
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

