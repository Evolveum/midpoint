/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.objectcollection;

import com.codeborne.selenide.Condition;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.common.search.SearchPropertiesConfigPanel;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.schrodinger.util.Utils;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Kate Honchar.
 */
public class ObjectCollectionPage extends AssignmentHolderDetailsPage {

    public SearchPropertiesConfigPanel<ObjectCollectionPage> configSearch() {
        selectTabBasic()
                .form()
                .findProperty("Filter")
                .$(Schrodinger.byDataId("configureButton"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
        return new SearchPropertiesConfigPanel<>(this, Utils.getModalWindowSelenideElement());
    }

    @Override
    public AssignmentHolderBasicTab<ObjectCollectionPage> selectTabBasic() {
        SelenideElement element = getTabPanel().clickTab("pageObjectCollection.basic.title")
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new AssignmentHolderBasicTab<>(this, element);
    }

}
