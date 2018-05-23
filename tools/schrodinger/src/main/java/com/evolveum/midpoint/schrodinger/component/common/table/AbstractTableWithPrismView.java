package com.evolveum.midpoint.schrodinger.component.common.table;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.common.PrismFormWithActionButtons;

/**
 * Created by matus on 5/17/2018.
 */
public abstract class AbstractTableWithPrismView<T> extends Table<T> {
    public AbstractTableWithPrismView(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public abstract PrismFormWithActionButtons<AbstractTableWithPrismView<T>> clickByName(String name);

    public abstract AbstractTableWithPrismView<T> selectCheckboxByName(String name);

    public abstract AbstractTableWithPrismView<T> unassignByName(String name);
}
