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
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.ChooseFocusTypeAndRelationModal;
import com.evolveum.midpoint.schrodinger.component.modal.FocusSetAssignmentsModal;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.page.FocusPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import com.evolveum.midpoint.schrodinger.util.Utils;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * @author skublik
 */

public class MemberPanel<T> extends Component<T> {

    public MemberPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public ChooseFocusTypeAndRelationModal<MemberPanel<T>> newMember() {
        SelenideElement mainButton = $(By.xpath("//button[@type='button'][@title='Create  member ']"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).parent();
        mainButton.click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        if (mainButton.$x(".//div[@data-s-id='additionalButton']").exists()) {
            return newMember("Create  member ");
        }
        return new ChooseFocusTypeAndRelationModal<>(this, Utils.getModalWindowSelenideElement());
    }

    public ChooseFocusTypeAndRelationModal newMember(String title) {
        SelenideElement mainButton = $(By.xpath("//button[@type='button'][@title='Create  member ']"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).parent();
        if (!mainButton.$x(".//div[@data-s-id='additionalButton']").exists()) {
            mainButton.click();
            Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        }
        mainButton.$x(".//div[@title=" + title + "]")
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return new ChooseFocusTypeAndRelationModal<>(this, Utils.getModalWindowSelenideElement());
    }

    public FocusSetAssignmentsModal<T> assignMember() {
        $(By.xpath("//button[@type='button'][@title='Assign  member ']")).waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return new FocusSetAssignmentsModal<T>((T) this.getParent(),  Utils.getModalWindowSelenideElement());
    }

    public MemberPanel<T> selectType(String type) {
        getParentElement().$x(".//select[@name='type:propertyLabel:row:selectWrapper:select']")
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S).selectOption(type);
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return this;
    }

    public MemberPanel<T> selectRelation(String relation) {
        getParentElement().$x(".//select[@name='searchByRelation:propertyLabel:row:selectWrapper:select']")
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S).selectOption(relation);
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return this;
    }

    public MemberTable<MemberPanel<T>> table() {
        SelenideElement table = getParentElement().$x(".//div[@" + Schrodinger.DATA_S_ID + "='memberTable']");
        return new MemberTable<>(this, table);
    }
}
