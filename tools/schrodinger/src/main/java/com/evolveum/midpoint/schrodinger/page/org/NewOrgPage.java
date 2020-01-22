/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.org;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class NewOrgPage extends AssignmentHolderDetailsPage {

    @Override
    public AssignmentHolderBasicTab<NewOrgPage> selectTabBasic(){
        SelenideElement element = findTabPanel().clickTab("pageAdminFocus.basic")
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new AssignmentHolderBasicTab<NewOrgPage>(this, element);
    }

    public AssignmentsTab<NewOrgPage> selectTabAssignments(){
        SelenideElement element = findTabPanel().clickTab("pageAdminFocus.assignments");

        return new AssignmentsTab<NewOrgPage>(this, element);
    }

}
