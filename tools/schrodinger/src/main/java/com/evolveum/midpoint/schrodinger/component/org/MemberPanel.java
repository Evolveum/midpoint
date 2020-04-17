/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.org;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * @author skublik
 */

public class MemberPanel<T> extends Component<T> {

    public MemberPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public UserPage newMember() {
        SelenideElement mainButton = $(By.xpath("//button[@type='button'][@title='Create  member ']"));
        String expanded = mainButton.getAttribute("aria-haspopup");
        if (Boolean.getBoolean(expanded)) {
            newMember("Create  member ");
        } else {
            mainButton.click();
        }
        return null; //TODO implement return popup
    }

    public AssignmentHolderDetailsPage newMember(String title) {
        SelenideElement mainButton = $(By.xpath("//button[@type='button'][@title='Create  member ']"));
        if (!Boolean.getBoolean(mainButton.getAttribute("aria-expanded"))) {
            mainButton.click();
            mainButton.waitWhile(Condition.attribute("aria-expanded", "false"), MidPoint.TIMEOUT_MEDIUM_6_S);
        }
        $(Schrodinger.byElementAttributeValue("div", "title", title))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return new AssignmentHolderDetailsPage(){};
    }

    public MemberPanel<T> selectType(String type) {
        getParentElement().$x(".//select[@name='type:propertyLabel:row:selectWrapper:select']")
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S).selectOption(type);
        return this;
    }

    public AssignmentHolderObjectListTable<MemberPanel<T>, AssignmentHolderDetailsPage> table() {
        SelenideElement table = getParentElement().$x(".//div[@" + Schrodinger.DATA_S_ID + "='table']");
        return new AssignmentHolderObjectListTable<MemberPanel<T>, AssignmentHolderDetailsPage>(this, table) {
            @Override
            public AssignmentHolderDetailsPage getObjectDetailsPage() {
                return new AssignmentHolderDetailsPage() {};
            }

            @Override
            public <P extends TableWithPageRedirect<MemberPanel<T>>> TableHeaderDropDownMenu<P> clickHeaderActionDropDown() {
                return null;
            }
        };
    }
}
