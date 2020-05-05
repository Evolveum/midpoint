/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/17/2018.
 */
public class PrismFormWithActionButtons<T> extends PrismForm<T> {
    public PrismFormWithActionButtons(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public T clickDone() {

        $(Schrodinger.byDataResourceKey("div", "MultivalueContainerListPanel.doneButton"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this.getParent();
    }

    public T clickCancel() {

        $(Schrodinger.byDataResourceKey("div", "MultivalueContainerListPanel.cancelButton"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this.getParent();
    }
}
