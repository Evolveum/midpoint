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
import com.evolveum.midpoint.schrodinger.component.FocusTableWithChoosableElements;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/2/2018.
 */
public class FocusSetProjectionModal<T> extends ModalBox<T> {
    public FocusSetProjectionModal(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public FocusTableWithChoosableElements<FocusSetProjectionModal<T>> table() {
        SelenideElement resourcesBox = $(By.cssSelector("box boxed-table"));

        return new FocusTableWithChoosableElements<>(this, resourcesBox);
    }

    public T clickAdd() {

        $(Schrodinger.byDataResourceKey("userBrowserDialog.button.addButton"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this.getParent();
    }
}
