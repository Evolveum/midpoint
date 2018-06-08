package com.evolveum.midpoint.schrodinger.component.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

/**
 * Created by matus on 5/22/2018.
 */
public class ResourceAccountsTab<T> extends Component<T> {
    public ResourceAccountsTab(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public ResourceTaskQuickAccessDropDown<ResourceAccountsTab<T>> importTask() {
        $(Schrodinger.byElementAttributeValue("label", "data-s-id", "label", "Import"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        SelenideElement dropDownElement = $(Schrodinger.byElementAttributeValue("ul", "role", "menu"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

        return new ResourceTaskQuickAccessDropDown<>(this, dropDownElement);
    }

    public ResourceTaskQuickAccessDropDown<ResourceAccountsTab<T>> reconciliationTask() {
        $(Schrodinger.byElementAttributeValue("label", "data-s-id", "label", "Reconciliation"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        SelenideElement dropDownElement = $(Schrodinger.byElementAttributeValue("ul", "role", "menu"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

        return new ResourceTaskQuickAccessDropDown<>(this, dropDownElement);
    }

    public ResourceTaskQuickAccessDropDown<ResourceAccountsTab<T>> liveSyncTask() {
        $(Schrodinger.byElementValue("label", "data-s-id", "label", "Live Sync"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        ElementsCollection dropDownElement = $$(By.cssSelector(".dropdown-menu.pull-right"));

        SelenideElement concretElement = null;

        for (SelenideElement element : dropDownElement) {
            if (element.isDisplayed()) {
                concretElement = element;
                break;
            }
        }
        return new ResourceTaskQuickAccessDropDown<>(this, concretElement);
    }

    public ResourceAccountsTab<T> clickSearchInRepository() {

        $(Schrodinger.byDataId("a", "repositorySearch"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        $(Schrodinger.byDataId("a", "repositorySearch"))
                .waitUntil(Condition.enabled, MidPoint.TIMEOUT_DEFAULT);

        return this;
    }

    public ResourceAccountsTab<T> clickSearchInResource() {
        $(Schrodinger.byDataId("a", "resourceSearch"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();
        $(Schrodinger.byDataId("a", "resourceSearch"))
                .waitUntil(Condition.enabled, MidPoint.TIMEOUT_DEFAULT);
        return this;
    }

    public ResourceShadowTable<ResourceAccountsTab<T>> table() {

        SelenideElement element = $(By.cssSelector(".box.boxed-table.object-shadow-box"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

        return new ResourceShadowTable<>(this, element);
    }

}
