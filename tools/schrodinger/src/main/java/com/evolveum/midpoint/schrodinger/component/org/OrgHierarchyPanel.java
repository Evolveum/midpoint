/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.org;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.org.OrgPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author skublik
 */

public class OrgHierarchyPanel<T> extends Component<T> {
    public OrgHierarchyPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public OrgHierarchyPanel<T> selectOrgInTree(String orgName) {
        boolean exist = getParentElement().$(Schrodinger.byElementValue("span", "class", "tree-label", orgName)).exists();
        if (!exist) {
            expandAllIfNeeded();
        }
        getParentElement().$(Schrodinger.byElementValue("span", "class", "tree-label", orgName))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return this;
    }

    private void expandAllIfNeeded() {
        boolean existExpandButton = getParentElement().$(By.cssSelector(".tree-junction-collapsed")).exists();
        if (existExpandButton) {
            expandAllOrgs();
        }
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
    }

    public OrgHierarchyPanel<T> expandAllOrgs() {
        clickOnTreeMenu();
        getParentElement().$(Schrodinger.byDataResourceKey("schrodinger", "TreeTablePanel.expandAll")).parent()
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }

    private void clickOnTreeMenu() {
        getParentElement().$(Schrodinger.byDataId("div", "treeMenu")).click();
    }

    public boolean containsChildOrg(String parentOrg, String... expectedChild){
        expandAllIfNeeded();
        SelenideElement parentNode = getParentOrgNode(parentOrg);
        SelenideElement subtree = parentNode.$x(".//div[@"+Schrodinger.DATA_S_ID+"='subtree']");
        ElementsCollection childsLabels = subtree.$$x(".//span[@"+Schrodinger.DATA_S_ID+"='label']");
        List<String> childs = new ArrayList<String>();
        for (SelenideElement childLabel : childsLabels) {
            childs.add(childLabel.getText());
        }
        return childs.containsAll(Arrays.asList(expectedChild));
    }

    private SelenideElement getParentOrgNode (String parentOrg) {
        selectOrgInTree(parentOrg);
        return getParentElement().$(By.cssSelector(".tree-node.success")).parent();
    }

    public OrgHierarchyPanel<T> expandOrg(String orgName) {
        SelenideElement parentNode = getParentOrgNode(orgName);
        SelenideElement node = parentNode.$x(".//div[@"+Schrodinger.DATA_S_ID+"='node']");
        SelenideElement expandButton = node.$x(".//a[@" + Schrodinger.DATA_S_ID + "='junction']");
        if (expandButton.has(Condition.cssClass("tree-junction-collapsed"))) {
            expandButton.waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S).click();
            expandButton.waitWhile(Condition.cssClass("tree-junction-collapsed"), MidPoint.TIMEOUT_DEFAULT_2_S);
        }
        return this;
    }

    public OrgTreeNodeDropDown showTreeNodeDropDownMenu(String orgName) {
        SelenideElement parentNode = getParentOrgNode(orgName);
        SelenideElement node = parentNode.$x(".//div[@"+Schrodinger.DATA_S_ID+"='node']");
        SelenideElement menuButton = node.$x(".//span[@" + Schrodinger.DATA_S_ID + "='menu']");
        menuButton.waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        SelenideElement menu = menuButton.$x(".//ul[@" + Schrodinger.DATA_S_ID + "='dropDownMenu']").waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new OrgTreeNodeDropDown(this, menu);
    }
}
