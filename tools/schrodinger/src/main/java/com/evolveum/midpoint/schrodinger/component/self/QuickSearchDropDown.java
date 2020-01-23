/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.self;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.common.DropDown;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/10/2018.
 */
public class QuickSearchDropDown<T> extends DropDown<T> {
    public QuickSearchDropDown(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public T clickUsers(){
        $(Schrodinger.byElementValue("a","Users"))
                .click();

        return this.getParent();
    }

    public T clickResources(){
        $(Schrodinger.byElementValue("a" ,"Resources"))
                .click();

        return this.getParent();
    }

    public T clickTasks(){
        $(Schrodinger.byElementValue("a" ,"Tasks")).
                click();

        return this.getParent();
    }
}
