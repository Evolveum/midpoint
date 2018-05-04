package com.evolveum.midpoint.schrodinger.component.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.table.Table;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithClickableElements;
import com.evolveum.midpoint.schrodinger.page.resource.ViewResourcePage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by matus on 4/25/2018.
 */
public class ResourcesTable<T> extends Table<T> implements TableWithClickableElements {
    public ResourcesTable(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }


    @Override
    public ViewResourcePage clickByName(String name) {
        getParentElement().$(Schrodinger.byElementEnclosedTextValue("span", "data-s-id", "label", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return new ViewResourcePage();
    }
}
