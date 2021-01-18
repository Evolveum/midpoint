/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.login;

import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.testng.Assert;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar.
 */
public class RegistrationConfirmationPage extends BasicPage {

    public RegistrationConfirmationPage assertSuccessPanelExists() {
        Assert.assertTrue($(Schrodinger.byDataId("successPanel")).exists());
        return this;
    }

}
