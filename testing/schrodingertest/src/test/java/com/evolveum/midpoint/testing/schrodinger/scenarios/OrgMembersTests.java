package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.codeborne.selenide.Condition;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.page.org.NewOrgPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.testing.schrodinger.TestBase;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar.
 */
public class OrgMembersTests extends TestBase {

    private static final String ORG_NAME = "";

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
}
