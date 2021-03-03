/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.common.table;

import com.codeborne.selenide.SelenideElement;
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

    public abstract AbstractTableWithPrismView<T> selectHeaderCheckbox();

    public abstract AbstractTableWithPrismView<T> removeByName(String name);

    public abstract AbstractTableWithPrismView<T> clickHeaderActionButton(String actionButtonStyle);
}
