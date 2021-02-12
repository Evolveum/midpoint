/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.self;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.org.OrgHierarchyPanel;
import com.evolveum.midpoint.schrodinger.page.self.RequestRolePage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by honchar
 */
public class RoleCatalogViewTab extends RequestRoleTab {

    public RoleCatalogViewTab(RequestRolePage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public OrgHierarchyPanel<RoleCatalogViewTab> getRoleCatalogHierarchyPanel() {
        SelenideElement treePanel = getParentElement().$(Schrodinger.byDataId("div", "treePanel"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new OrgHierarchyPanel<>(this, treePanel);
    }


}
