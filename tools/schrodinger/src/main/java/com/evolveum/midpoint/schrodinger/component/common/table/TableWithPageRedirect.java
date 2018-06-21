package com.evolveum.midpoint.schrodinger.component.common.table;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.page.BasicPage;

/**
 * Created by matus on 5/2/2018.
 */
public abstract class TableWithPageRedirect<T> extends Table<T> {

    public TableWithPageRedirect(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public abstract <E extends BasicPage> E clickByName(String name);

    public abstract TableWithPageRedirect<T> selectCheckboxByName(String name);
}
