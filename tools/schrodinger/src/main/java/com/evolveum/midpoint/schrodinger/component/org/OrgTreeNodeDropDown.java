/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.org;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.DropDown;
import com.evolveum.midpoint.schrodinger.page.org.OrgPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * @author skublik
 */

public class OrgTreeNodeDropDown<T> extends DropDown<T> {

    public OrgTreeNodeDropDown(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public OrgPage edit(){
        getParentElement().$x(".//schrodinger[@"+ Schrodinger.DATA_S_RESOURCE_KEY +"='TreeTablePanel.edit']").parent()
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return new OrgPage();
    }

    public T expandAll(){
        getParentElement().$x(".//schrodinger[@"+ Schrodinger.DATA_S_RESOURCE_KEY +"='TreeTablePanel.expandAll']").parent()
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        return getParent();
    }

    public T collapseAll(){
        getParentElement().$x(".//schrodinger[@"+ Schrodinger.DATA_S_RESOURCE_KEY +"='TreeTablePanel.collapseAll']").parent()
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        return getParent();
    }
}
