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
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

/**
 * Created by Kate Honchar
 */
public class ChildrenCaseTable extends TableWithPageRedirect<CasePage> {
    public ChildrenCaseTable(CasePage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public CasePage clickByName(String name) {
        getParentElement().$(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return new CasePage();
    }

    @Override
    public ChildrenCaseTable selectCheckboxByName(String name) {
        //TODO implement

        return this;
    }

    public CasePage clickByPartialName(String name) {
        getParentElement()
                .$(Schrodinger.byDataId("tableContainer"))
                .$(By.partialLinkText(name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return new CasePage();
    }

}
