package com.evolveum.midpoint.schrodinger.page.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Resource.ResourceConfigurationTab;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.page.task.EditTaskPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;


public class ViewResourcePage extends BasicPage {

    public ResourceConfigurationTab clickEditResourceConfiguration(){

     $(Schrodinger.byDataResourceKey("a","pageResource.button.configurationEdit")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

    return new ResourceConfigurationTab(new EditResourceConfigurationPage(),null);
    }

}
