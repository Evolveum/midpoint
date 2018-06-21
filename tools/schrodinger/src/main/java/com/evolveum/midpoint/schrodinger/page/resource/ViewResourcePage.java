package com.evolveum.midpoint.schrodinger.page.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.FeedbackBox;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceAccountsTab;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceConfigurationTab;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.page.configuration.AboutPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;


public class ViewResourcePage extends BasicPage {

    public ResourceConfigurationTab clickEditResourceConfiguration() {

        $(Schrodinger.byDataResourceKey("a", "pageResource.button.configurationEdit")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return new ResourceConfigurationTab(new EditResourceConfigurationPage(), null);
    }

    public ResourceAccountsTab<ViewResourcePage> clicAccountsTab() {

        $(Schrodinger.byDataResourceKey("schrodinger", "PageResource.tab.content.account")).parent()
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        SelenideElement tabContent = $(By.cssSelector(".tab-pane.active"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

        return new ResourceAccountsTab<>(this, tabContent);
    }

    public FeedbackBox<ViewResourcePage> feedback() {
        SelenideElement feedback = $(By.cssSelector("div.feedbackContainer"));

        return new FeedbackBox<>(this, feedback);
    }
}
