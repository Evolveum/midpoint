/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.user;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.component.ProjectionsTab;
import com.evolveum.midpoint.schrodinger.component.common.SummaryPanel;
import com.evolveum.midpoint.schrodinger.component.user.*;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.page.FocusPage;
import com.evolveum.midpoint.schrodinger.page.PreviewPage;
import com.evolveum.midpoint.schrodinger.page.org.OrgPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;
import static com.evolveum.midpoint.schrodinger.util.Utils.setOptionChecked;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UserPage extends FocusPage {

    public UserPersonasTab selectTabPersonas() {
        SelenideElement element = findTabPanel().clickTab("pageAdminFocus.personas");

        return new UserPersonasTab(this, element);
    }

    public UserTasksTab selectTabTasks() {
        SelenideElement element = findTabPanel().clickTab("pageAdminFocus.tasks");

        return new UserTasksTab(this, element);
    }

    public UserHistoryTab selectTabHistory() {
        SelenideElement element = findTabPanel().clickTab("pageAdminFocus.objectHistory");

        return new UserHistoryTab(this, element);
    }

    public UserDelegationsTab selectTabDelegations() {
        SelenideElement element = findTabPanel().clickTab("FocusType.delegations");

        return new UserDelegationsTab(this, element);
    }

    public UserDelegatedToMeTab selectTabDelegatedToMe() {
        SelenideElement element = findTabPanel().clickTab("FocusType.delegatedToMe");

        return new UserDelegatedToMeTab(this, element);
    }

    @Override
    public ProjectionsTab<UserPage> selectTabProjections() {
        return super.selectTabProjections();
    }

    @Override
    public AssignmentHolderBasicTab<UserPage> selectTabBasic() {
        return super.selectTabBasic();
    }

    @Override
    public AssignmentsTab<UserPage> selectTabAssignments() {
        return super.selectTabAssignments();
    }
}
