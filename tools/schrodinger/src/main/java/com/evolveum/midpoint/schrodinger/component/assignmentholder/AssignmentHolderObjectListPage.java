/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.assignmentholder;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar
 */
public abstract class AssignmentHolderObjectListPage<T extends AssignmentHolderObjectListTable> extends BasicPage {

    public abstract T table();

    protected SelenideElement getTableBoxElement(){
        StringBuilder tableStyle = new StringBuilder(".box.boxed-table");
        String additionalTableClass = getTableAdditionalClass();
        if (StringUtils.isNotEmpty(additionalTableClass)){
            tableStyle.append(".");
            tableStyle.append(additionalTableClass);
        }
        SelenideElement box = $(By.cssSelector(".box.boxed-table"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
        return box;
    }

    protected String getTableAdditionalClass(){
        return null;
    }

}
