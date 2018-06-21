package com.evolveum.midpoint.schrodinger.page.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.resource.*;
import com.evolveum.midpoint.schrodinger.component.common.TabPanel;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;


public class EditResourceConfigurationPage extends BasicPage {

    private static final String CONFIGURATION_TAB_NAME = "Configuration";
    private static final String CONNECTORPOOL_TAB_NAME = "Connector pool";
    private static final String TIMEOUTS_TAB_NAME = "Timeouts";
    private static final String RESULTHANDLERS_TAB_NAME = "Results handlers";

    private TabPanel findTabPanel() {
        SelenideElement tabPanelElement = $(Schrodinger.byDataId("div", "tabs-container"));
        return new TabPanel<>(this, tabPanelElement);
    }

    public ResourceConfigurationTab selectTabconfiguration() {
        SelenideElement element = findTabPanel().clickTabWithName(CONFIGURATION_TAB_NAME);

        return new ResourceConfigurationTab(this, element);
    }

    public ResourceConnectorPoolTab selectTabConnectorPool() {
        SelenideElement element = findTabPanel().clickTabWithName(CONNECTORPOOL_TAB_NAME);

        return new ResourceConnectorPoolTab(this, element);
    }

    public ResourceTimeoutsTab selectTabTimeouts() {
        SelenideElement element = findTabPanel().clickTabWithName(TIMEOUTS_TAB_NAME);

        return new ResourceTimeoutsTab(this, element);
    }

    public ResourceResultsHandlersTab selectTabResultHandlers() {
        SelenideElement element = findTabPanel().clickTabWithName(RESULTHANDLERS_TAB_NAME);

        return new ResourceResultsHandlersTab(this, element);
    }

    public TestConnectionModal<EditResourceConfigurationPage> clickSaveAndTestConnection() {
        $(Schrodinger.byDataId("testConnection")).click();
        SelenideElement testModalBox = $(Schrodinger
                .byElementAttributeValue("div", "aria-labelledby", "Test connection result(s)"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

        return new TestConnectionModal<>(this, testModalBox);
    }


}
