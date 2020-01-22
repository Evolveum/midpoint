/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.modal;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/9/2018.
 */
public class ConfirmationModal<T> extends ModalBox<T> {
    public ConfirmationModal(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public T clickYes() {
        $(Schrodinger.byDataResourceKey("a", "confirmationDialog.yes"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        $(Schrodinger.byDataResourceKey("a", "confirmationDialog.yes"))
                .waitUntil(Condition.disappears, MidPoint.TIMEOUT_EXTRA_LONG_1_M);

        return this.getParent();
    }

    public T clickNo() {

        $(Schrodinger.byDataResourceKey("a", "confirmationDialog.no"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        $(Schrodinger.byDataResourceKey("a", "confirmationDialog.yes"))
                .waitUntil(Condition.disappears, MidPoint.TIMEOUT_LONG_1_M);

        return this.getParent();
    }

    public T close() {
        $(By.className("w_close"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        $(Schrodinger.byDataResourceKey("a", "confirmationDialog.yes"))
                .waitUntil(Condition.disappears, MidPoint.TIMEOUT_LONG_1_M);

        return this.getParent();
    }

}
