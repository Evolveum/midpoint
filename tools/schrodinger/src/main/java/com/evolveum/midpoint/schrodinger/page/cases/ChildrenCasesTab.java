/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.cases;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.cases.ChildrenCaseTable;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Kate Honchar
 */
public class ChildrenCasesTab extends Component<CasePage> {

    public ChildrenCasesTab(CasePage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public ChildrenCaseTable table(){
        SelenideElement tableBox = $(By.cssSelector(".box.boxed-table")).waitUntil(Condition.exist, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new ChildrenCaseTable(getParent(), tableBox);
    }
}
