/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.modal;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
/**
 * Created by honchar
 */
public class ReportConfigurationModal<T> extends ModalBox<T>{

    public ReportConfigurationModal(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public T runReport() {
        getParentElement().$x("//a[@data-s-id='runReport']").waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return getParent();
    }
}
