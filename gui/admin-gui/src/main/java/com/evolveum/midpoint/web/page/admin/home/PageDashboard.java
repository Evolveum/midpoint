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

import com.evolveum.midpoint.web.component.dashboard.Dashboard;
import com.evolveum.midpoint.web.component.dashboard.DashboardPanel;
import com.evolveum.midpoint.web.page.admin.home.component.MyAccountsPanel;
import com.evolveum.midpoint.web.page.admin.home.component.PersonalInfoPanel;
import com.evolveum.midpoint.web.page.admin.home.dto.MyAccountsDashboard;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.Model;
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
    private static final String ID_ORG_UNITS = "assignedOrgUnits";

    public PageDashboard() {
        initLayout();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(
                new PackageResourceReference(PageDashboard.class, "PageDashboard.css")));
    }

    private void initLayout() {
        initPersonalInfo();
        initMyWorkItems();
        initMyAccounts();

        DashboardPanel assignedRoles = new DashboardPanel(ID_ROLES, createStringResource("PageDashboard.assignedRoles"));
        add(assignedRoles);

        DashboardPanel assignedOrgUnits = new DashboardPanel(ID_ORG_UNITS,
                createStringResource("PageDashboard.assignedOrgUnits"),
                new Model<Dashboard>(new Dashboard(true) {

                    private int i = 0;

                    @Override
                    public boolean isLoaded() {
                        i++;
                        if (i < 8) {
                            return false;
                        }
                        return true;
                    }
                }));
        add(assignedOrgUnits);

        DashboardPanel assignedResources = new DashboardPanel(ID_RESOURCES,
                createStringResource("PageDashboard.assignedResources"), new Model<Dashboard>(new Dashboard(true)));
        add(assignedResources);
    }

    private void initPersonalInfo() {
        DashboardPanel personalInfo = new DashboardPanel(ID_PERSONAL_INFO,
                createStringResource("PageDashboard.personalInfo")) {

            @Override
            protected Component getLazyLoadComponent(String componentId) {
                return new PersonalInfoPanel(componentId);
            }
        };
        add(personalInfo);
    }

    private void initMyWorkItems() {
        Dashboard dashboard = new Dashboard();
        dashboard.setShowMinimize(true);
        DashboardPanel workItems = new DashboardPanel(ID_WORK_ITEMS, createStringResource("PageDashboard.workItems"),
                new Model<Dashboard>(dashboard));
        add(workItems);
    }

    private void initMyAccounts() {
        DashboardPanel accounts = new DashboardPanel(ID_ACCOUNTS,
                createStringResource("PageDashboard.accounts"),
                new Model(new MyAccountsDashboard())) {

            @Override
            protected Component getLazyLoadComponent(String componentId) {
                return new MyAccountsPanel(componentId);
            }
        };
        add(accounts);
    }
}
