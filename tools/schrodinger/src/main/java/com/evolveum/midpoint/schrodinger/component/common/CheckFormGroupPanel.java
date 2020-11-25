/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;

/**
 * Created by honchar
 */
public class CheckFormGroupPanel<T> extends Component<T> {

    public CheckFormGroupPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public void setOptionCheckedById(boolean checked) {
        getParentElement().$x("//input[@data-s-id='check']")
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setSelected(checked);
    }

}
