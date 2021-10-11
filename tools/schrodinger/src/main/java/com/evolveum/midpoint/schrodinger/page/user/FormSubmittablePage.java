/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.user;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.SchrodingerException;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

public class FormSubmittablePage {

    SelenideElement element;

    public FormSubmittablePage() {

        this.element = $(Schrodinger.byDataId("dynamicForm")).waitUntil(Condition.visible, MidPoint.TIMEOUT_MEDIUM_6_S);

    }

    public PrismForm<FormSubmittablePage> form() {

        if (!element.exists()) {
            throw new SchrodingerException("Dynamic form not present");
        }

        return new PrismForm<>(this, element);
    }

}
