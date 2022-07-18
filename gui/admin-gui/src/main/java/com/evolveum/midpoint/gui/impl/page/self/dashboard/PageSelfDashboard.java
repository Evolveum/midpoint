/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self.dashboard;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.self.PageSelf;
import com.evolveum.midpoint.web.page.self.component.LinksPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RichHyperlinkType;

import org.apache.wicket.model.Model;

import java.util.Arrays;
import java.util.List;


@PageDescriptor(
        urls = {
//                @Url(mountUrl = "/self", matchUrlForSecurity = "/self"),
                @Url(mountUrl = "/self/dashboardNew")
        },
        action = {
                @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                        label = PageSelf.AUTH_SELF_ALL_LABEL,
                        description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_DASHBOARD_URL,
                        label = "PageSelfDashboard.auth.dashboard.label",
                        description = "PageSelfDashboard.auth.dashboard.description")
        })
public class PageSelfDashboard extends PageSelf {

    private static final Trace LOGGER = TraceManager.getTrace(PageSelfDashboard.class);
    private static final String ID_SEARCH_PANEL = "searchPanel";
    private static final String ID_LINKS_PANEL = "linksPanel";
    private static final String ID_WIDGETS_PANEL = "widgetsPanel";

    public PageSelfDashboard() {
        initLayout();
    }

    private void initLayout() {
        DashboardSearchPanel dashboardSearchPanel = new DashboardSearchPanel(ID_SEARCH_PANEL);
        List<String> searchPanelActions = Arrays.asList(AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_USERS_URL, AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCES_URL, AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_TASKS_URL);
        dashboardSearchPanel.add(new VisibleBehaviour(() -> {
//            UserInterfaceElementVisibilityType visibility = getComponentVisibility(PredefinedDashboardWidgetId.SEARCH);
//            return WebComponentUtil.getElementVisibility(visibility, searchPanelActions);
            return true;
        }));
        add(dashboardSearchPanel);

        LinksPanel linksPanel = new LinksPanel(ID_LINKS_PANEL, Model.ofList(loadLinksList()));
        linksPanel.add(new VisibleBehaviour(() -> {
//            UserInterfaceElementVisibilityType visibility = getComponentVisibility(PredefinedDashboardWidgetId.SHORTCUTS);
//            return WebComponentUtil.getElementVisibility(visibility);
            return true;
        }));
        add(linksPanel);

     }

    private List<RichHyperlinkType> loadLinksList() {
        return ((PageBase) getPage()).getCompiledGuiProfile().getUserDashboardLink();
    }
}
