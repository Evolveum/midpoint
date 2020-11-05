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
public class ForgetPasswordSecurityQuestionsPage {


    public MyPasswordQuestionsPanel<ForgetPasswordSecurityQuestionsPage> getPasswordQuestionsPanel(){
        SelenideElement element = $(Schrodinger.byDataId("pwdQuestionsPanel"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new MyPasswordQuestionsPanel<ForgetPasswordSecurityQuestionsPage>(this, element);
    }

    public void clickSendButton() {
        $(Schrodinger.byDataId("send")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
    }
}
