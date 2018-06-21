package com.evolveum.midpoint.schrodinger.component.common.table;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;

/**
 * Created by matus on 5/17/2018.
 */
public abstract class TableWithComponentRedirect<T> extends Table<T> {
    public TableWithComponentRedirect(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public abstract <E extends Component<TableWithComponentRedirect<T>>> E clickByName(String name);

    public abstract AbstractTable<T> selectCheckboxByName(String name);

}
