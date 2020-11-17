/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.user;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.ProjectionsTab;
import com.evolveum.midpoint.schrodinger.component.user.*;
import com.evolveum.midpoint.schrodinger.page.FocusPage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UserPage extends FocusPage {

    public UserPersonasTab selectTabPersonas() {
        SelenideElement element = getTabPanel().clickTab("pageAdminFocus.personas");

        return new UserPersonasTab(this, element);
    }

    public UserTasksTab selectTabTasks() {
        SelenideElement element = getTabPanel().clickTab("pageAdminFocus.tasks");

        return new UserTasksTab(this, element);
    }

    public UserHistoryTab selectTabHistory() {
        SelenideElement element = getTabPanel().clickTab("pageAdminFocus.objectHistory");

        return new UserHistoryTab(this, element);
    }

    public UserDelegationsTab selectTabDelegations() {
        SelenideElement element = getTabPanel().clickTab("FocusType.delegations");

        return new UserDelegationsTab(this, element);
    }

    public UserDelegatedToMeTab selectTabDelegatedToMe() {
        SelenideElement element = getTabPanel().clickTab("FocusType.delegatedToMe");

        return new UserDelegatedToMeTab(this, element);
    }

    @Override
    public ProjectionsTab<UserPage> selectTabProjections() {
        return super.selectTabProjections();
    }
}
