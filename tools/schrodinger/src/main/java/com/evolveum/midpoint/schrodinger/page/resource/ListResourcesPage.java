package com.evolveum.midpoint.schrodinger.page.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.resource.ResourcesPageTable;
import com.evolveum.midpoint.schrodinger.component.resource.TestConnectionModal;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListResourcesPage extends BasicPage {

    public ResourcesPageTable<ListResourcesPage> table() {
        SelenideElement table = $(By.cssSelector(".box.boxed-table.object-resource-box")).waitUntil(Condition.exist, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ResourcesPageTable<>(this, table);
    }

    public TestConnectionModal<ListResourcesPage> testConnectionClick(String resourceName){
        table()
                .search()
                .byName()
                .inputValue(resourceName)
                .updateSearch();

        SelenideElement testConnectionIcon = $(By.cssSelector("fa fa-question fa-fw")).waitUntil(Condition.exist, MidPoint.TIMEOUT_DEFAULT_2_S);
        testConnectionIcon.click();
        SelenideElement testModalBox = $(Schrodinger
                .byElementAttributeValue("div", "aria-labelledby", "Test connection result(s)"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new TestConnectionModal<>(this, testModalBox);

    }
}
