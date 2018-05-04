package com.evolveum.midpoint.schrodinger.component.common.table;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;

/**
 * Created by matus on 5/4/2018.
 */
public abstract class TableWithPrismContainers<T> extends Table<T> {

    public TableWithPrismContainers(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public abstract PrismForm<TableWithPrismContainers<T>> clickByName(String name);

}
