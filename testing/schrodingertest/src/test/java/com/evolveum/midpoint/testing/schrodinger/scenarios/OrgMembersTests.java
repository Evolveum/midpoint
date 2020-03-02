/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.codeborne.selenide.Condition;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.page.org.NewOrgPage;
import com.evolveum.midpoint.schrodinger.page.org.OrgTreePage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.testing.schrodinger.TestBase;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar.
 */
public class OrgMembersTests extends TestBase {

    private static final String ORG_NAME = "TestOrgWithMembers";
    private static final String USER_NAME = "OrgMembersWithDefaultRelation";

    @Test
    public void createOrgWithinMenuItem(){
        NewOrgPage newOrgPage = basicPage.newOrgUnit();
        AssignmentHolderBasicTab<NewOrgPage> basicTab = newOrgPage
                .selectTabBasic()
                    .form()
                    .addAttributeValue("Name", ORG_NAME)
                    .and();


        basicTab
                .and()
                .clickSave();

        $(Schrodinger.byElementAttributeValue("a", "class", "tab-label")).find(By.linkText(ORG_NAME)).shouldBe(Condition.visible);
    }

    @Test
    public void assignDefaultRelationMember(){
        UserPage user = basicPage.newUser();

        Assert.assertTrue(user.selectTabBasic()
                .form()
                    .addAttributeValue("name", USER_NAME)
                    .and()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                .feedback()
                .isSuccess());

        OrgTreePage orgTreePage = basicPage.orgStructure();

    }
}
