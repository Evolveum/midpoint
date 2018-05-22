package com.evolveum.midpoint.schrodinger.component.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.page.resource.ViewResourcePage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by matus on 4/25/2018.
 */
public class ResourcesTablePage<T> extends TableWithPageRedirect<T> {
    public ResourcesTablePage(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public TableWithPageRedirect<T> selectCheckboxByName(String name) {
        return null;
    }


    @Override
    public ViewResourcePage clickByName(String name) {
        getParentElement().$(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return new ViewResourcePage();
    }
}
