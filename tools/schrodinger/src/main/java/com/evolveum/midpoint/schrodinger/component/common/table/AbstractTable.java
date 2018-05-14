package com.evolveum.midpoint.schrodinger.component.common.table;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;

/**
 * Created by matus on 5/9/2018.
 */
public abstract class AbstractTable<T> extends Table<T> {
    public AbstractTable(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public abstract PrismForm<AbstractTable<T>> clickByName(String name);

    public abstract AbstractTable<T> selectCheckboxByName(String name);
}
