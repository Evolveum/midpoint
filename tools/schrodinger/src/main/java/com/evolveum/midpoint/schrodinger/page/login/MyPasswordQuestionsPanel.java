/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.login;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar.
 */
public class MyPasswordQuestionsPanel<T> extends Component<T> {

    public MyPasswordQuestionsPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public MyPasswordQuestionsPanel<T> setMyPasswordTFValue(String value) {
        $(Schrodinger.byDataId("answerTF")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(value);
        return this;
    }
}
