/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.org;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.org.OrgTreePage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

/**
 * @author skublik
 */

public class OrgRootTab extends Component<OrgTreePage> {

    public OrgRootTab(OrgTreePage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public OrgHierarchyPanel<OrgRootTab> getOrgHierarchyPanel() {
        SelenideElement treePanel = getParentElement().$(Schrodinger.byDataId("div", "treePanel"));
        return new OrgHierarchyPanel<>(this, treePanel);
    }

    public MemberPanel<OrgRootTab> getMemberPanel() {
        SelenideElement memberPanel = getParentElement().$(Schrodinger.byDataId("div", "memberPanel"));
        return new MemberPanel<>(this, memberPanel);
    }
}
