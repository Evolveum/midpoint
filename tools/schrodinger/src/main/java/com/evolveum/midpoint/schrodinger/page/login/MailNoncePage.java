/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.login;

import com.codeborne.selenide.Condition;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MailNoncePage extends LoginPage {

    public MailNoncePage setMail(String mail) {
        $(Schrodinger.byDataId("email")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(mail);
        $(Schrodinger.byDataId("submit")).click();
        return  this;
    }

    protected static String getBasePath() {
        return "/emailNonce";
    }
}
