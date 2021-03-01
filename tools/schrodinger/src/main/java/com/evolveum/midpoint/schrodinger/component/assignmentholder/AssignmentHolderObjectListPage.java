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
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.page.BasicPage;

import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.testng.Assert;

import static com.codeborne.selenide.Selenide.$;

/**
 * @author skublik
 */

public abstract class AssignmentHolderObjectListPage<T extends AssignmentHolderObjectListTable, D extends AssignmentHolderDetailsPage> extends BasicPage {

    public abstract T table();

    public abstract D getObjectDetailsPage();

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

    public int getCountOfObjects() {
        String countString = $(Schrodinger.byDataId("div", "count")).getText();
        return Integer.valueOf(countString.substring(countString.lastIndexOf(" ")+1));
    }

    public D newObjectCollection(String title) {
        SelenideElement mainButton = $(By.xpath("//button[@type='button'][@" + Schrodinger.DATA_S_ID + "='mainButton']"));
        if (!Boolean.getBoolean(mainButton.getAttribute("aria-expanded"))) {
            mainButton.click();
        }
        $(Schrodinger.byElementAttributeValue("div", "title", title))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return getObjectDetailsPage();
    }

    public AssignmentHolderObjectListPage<T, D> assertObjectsCountEquals(int expectedCount) {
        assertion.assertEquals(getCountOfObjects(), expectedCount, "Objects count doesn't equal to " + expectedCount);
        return this;
    }

}
