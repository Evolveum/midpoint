package com.evolveum.midpoint.schrodinger.page.resource;

import com.codeborne.selenide.Condition;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar.
 */
public class ResourceWizardPage extends BasicPage {

    public void clickOnWizardTab(String tabName){
        $(By.linkText(tabName))
                .shouldBe(Condition.visible)
                .click();
    }
}
