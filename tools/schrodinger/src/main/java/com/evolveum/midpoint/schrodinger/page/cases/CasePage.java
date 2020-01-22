/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.cases;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;

/**
 * Created by Kate Honchar.
 */
public class CasePage extends AssignmentHolderDetailsPage {

    @Override
    public AssignmentHolderBasicTab<CasePage> selectTabBasic(){
        SelenideElement element = findTabPanel().clickTab("pageAdminFocus.basic")
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new AssignmentHolderBasicTab<CasePage>(this, element);
    }

    @Override
    public AssignmentsTab<CasePage> selectTabAssignments(){
        SelenideElement element = findTabPanel().clickTab("pageAdminFocus.assignments");

        return new AssignmentsTab<CasePage>(this, element);
    }

    public ChildrenCasesTab selectTabChildren(){
        SelenideElement element = findTabPanel().clickTab("PageCase.childCasesTab");

        return new ChildrenCasesTab(this, element);
    }

    public OperationRequestTab selectTabOperationRequest(){
        SelenideElement element = findTabPanel().clickTab("PageCase.operationRequestTab");

        return new OperationRequestTab(this, element);
    }

    public WorkitemsTab selectTabWorkitems(){
        SelenideElement element = findTabPanel().clickTab("PageCase.workitemsTab");

        return new WorkitemsTab(this, element);
    }
}
