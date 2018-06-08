package com.evolveum.midpoint.schrodinger.component.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.DropDown;
import com.evolveum.midpoint.schrodinger.page.task.NewTaskPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

/**
 * Created by matus on 5/22/2018.
 */
public class ResourceTaskQuickAccessDropDown<T> extends DropDown<T> {
    public ResourceTaskQuickAccessDropDown(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public T clickShowExisting() {
        $(Schrodinger.byDataResourceKey("schrodinger", "ResourceContentResourcePanel.showExisting")).parent()
                .click();

        return this.getParent();
    }

    public NewTaskPage clickCreateNew() {

        ElementsCollection elements = $$(Schrodinger.byElementValue("a", "data-s-id", "menuItemLink", "Create new"));
        for (SelenideElement element : elements) {

            if (element.isDisplayed()) {
                element.click();
                break;
            }
        }

        return new NewTaskPage();
    }

}
