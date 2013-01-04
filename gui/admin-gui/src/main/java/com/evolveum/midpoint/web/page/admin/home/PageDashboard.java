/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.home;

import com.evolveum.midpoint.web.component.dashboard.DashboardPanel;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author lazyman
 */
public class PageDashboard extends PageAdminHome {

    private static final String ID_PERSONAL_INFO = "personalInfo";
    private static final String ID_WORK_ITEMS = "workItems";
    private static final String ID_ACCOUNTS = "accounts";
    private static final String ID_ROLES = "assignedRoles";
    private static final String ID_RESOURCES = "assignedResources";

    public PageDashboard() {
        initLayout();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.renderCSSReference(new PackageResourceReference(PageDashboard.class, "PageDashboard.css"));
    }

    private void initLayout() {
        DashboardPanel personalInfo = new DashboardPanel(ID_PERSONAL_INFO);
        add(personalInfo);

        DashboardPanel workItems = new DashboardPanel(ID_WORK_ITEMS);
        add(workItems);

        DashboardPanel accounts = new DashboardPanel(ID_ACCOUNTS);
        add(accounts);

        DashboardPanel assignedRoles = new DashboardPanel(ID_ROLES);
        add(assignedRoles);

        DashboardPanel assignedResources = new DashboardPanel(ID_RESOURCES);
        add(assignedResources);
    }
}
