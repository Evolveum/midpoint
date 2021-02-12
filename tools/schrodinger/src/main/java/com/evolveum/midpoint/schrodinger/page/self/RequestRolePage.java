/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.self;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.TabPanel;
import com.evolveum.midpoint.schrodinger.component.self.RequestRoleTab;
import com.evolveum.midpoint.schrodinger.component.self.RoleCatalogViewTab;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RequestRolePage extends BasicPage {

    public RoleCatalogViewTab selectRoleCatalogViewTab(){
        SelenideElement tabElement = getTabPanel().clickTab("AssignmentViewType.ROLE_CATALOG_VIEW");
        return new RoleCatalogViewTab(this, tabElement);
    }

    public RequestRoleTab selectAllRolesViewTab(){
        SelenideElement tabElement = getTabPanel().clickTab("AssignmentViewType.ROLE_TYPE");
        return new RequestRoleTab(this, tabElement);
    }

    public RequestRoleTab selectAllOrganizationsViewTab(){
        SelenideElement tabElement = getTabPanel().clickTab("AssignmentViewType.ORG_TYPE");
        return new RequestRoleTab(this, tabElement);
    }

    public RequestRoleTab selectAllServicesViewTab(){
        SelenideElement tabElement = getTabPanel().clickTab("AssignmentViewType.SERVICE_TYPE");
        return new RequestRoleTab(this, tabElement);
    }

    public RequestRoleTab selectUserAssignmentsTab(){
        SelenideElement tabElement = getTabPanel().clickTab("AssignmentViewType.USER_TYPE");
        return new RequestRoleTab(this, tabElement);
    }

    private TabPanel<RequestRolePage> getTabPanel() {
        SelenideElement tabPanelElement = $(Schrodinger.byDataId("div", "viewsTabPanel"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new TabPanel<>(this, tabPanelElement);
    }

}
