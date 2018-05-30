package com.evolveum.midpoint.schrodinger.component.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.DropDown;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/25/2018.
 */
public class ResourceShadowTable<T> extends TableWithPageRedirect<T> {
    public ResourceShadowTable(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public <E extends BasicPage> E clickByName(String name) {
        return null;
    }

    @Override
    public ResourceShadowTable<T> selectCheckboxByName(String name) {

        $(Schrodinger.byAncestorFollowingSiblingElementValue("input", "type", "checkbox", "data-s-id", "1", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();
        return this;
    }

    public ResourceShadowTableCog<ResourceShadowTable<T>> clickCog() {

        $(Schrodinger.byElementAttributeValue("a", "about", "dropdownMenu"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        SelenideElement cog = $(By.cssSelector(".dropdown-menu.pull-right"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

        return new ResourceShadowTableCog<>(this, cog);
    }

}
