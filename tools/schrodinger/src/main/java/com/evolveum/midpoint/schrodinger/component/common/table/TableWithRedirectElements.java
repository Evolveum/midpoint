package com.evolveum.midpoint.schrodinger.component.common.table;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.page.BasicPage;

/**
 * Created by matus on 5/2/2018.
 */
public abstract class TableWithRedirectElements<T> extends Table<T> {

    public TableWithRedirectElements(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public abstract <E extends BasicPage> E clickByName(String name);

    public abstract TableWithRedirectElements<T> selectCheckboxByName (String name);
}
