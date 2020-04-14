/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.cases;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;

/**
 * Created by Kate Honchar.
 */
public class CasePage extends AssignmentHolderDetailsPage {

    public ChildrenCasesTab selectTabChildren(){
        SelenideElement element = getTabPanel().clickTab("PageCase.childCasesTab");

        return new ChildrenCasesTab(this, element);
    }

    public OperationRequestTab selectTabOperationRequest(){
        SelenideElement element = getTabPanel().clickTab("PageCase.operationRequestTab");

        return new OperationRequestTab(this, element);
    }

    public WorkitemsTab selectTabWorkitems(){
        SelenideElement element = getTabPanel().clickTab("PageCase.workitemsTab");

        return new WorkitemsTab(this, element);
    }
}
