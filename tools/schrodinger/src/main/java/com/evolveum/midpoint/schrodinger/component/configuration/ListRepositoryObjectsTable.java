package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.Condition;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.page.configuration.ListRepositoryObjectsPage;
import com.evolveum.midpoint.schrodinger.page.configuration.RepositoryObjectPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

public class ListRepositoryObjectsTable extends TableWithPageRedirect<ListRepositoryObjectsPage> {

    public ListRepositoryObjectsTable(ListRepositoryObjectsPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public RepositoryObjectPage clickByName(String name) {
        getParentElement().$(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return new RepositoryObjectPage();
    }

    @Override
    public ListRepositoryObjectsTable selectCheckboxByName(String name) {
        return null;
    }

    @Override
    protected TableHeaderDropDownMenu<ListRepositoryObjectsTable> clickHeaderActionDropDown() {
        return null;
    }

}
