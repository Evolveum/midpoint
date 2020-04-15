/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.component.user;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UserHistoryTab extends Component<UserPage> {

    public UserHistoryTab(UserPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
