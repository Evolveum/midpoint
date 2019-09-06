/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
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
