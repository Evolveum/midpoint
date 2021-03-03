/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.util;

import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.component.common.CheckFormGroupPanel;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTableWithPrismView;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Utils {

    public static <T> T createInstance(Class<T> type) {
        try {
            return type.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void setOptionCheckedByName(String optionName, boolean checked) {
        SelenideElement checkBox = $(By.name(optionName)).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        checkBox.setSelected(checked).waitUntil(Condition.checked, MidPoint.TIMEOUT_DEFAULT_2_S);
    }

    public static void setOptionCheckedById(String id, boolean checked) {
        SelenideElement checkBox = $(Schrodinger.byDataId(id)).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        checkBox.setSelected(checked);
    }

    public static void setCheckFormGroupOptionCheckedById(String id, boolean checked) {
        CheckFormGroupPanel checkBoxGroup = new CheckFormGroupPanel(null,
                $(Schrodinger.byDataId(id)).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
        checkBoxGroup.setOptionCheckedById(checked);
    }

    public static void setCheckFormGroupOptionCheckedByTitleResourceKey(String titleResourceKey, boolean checked) {
        CheckFormGroupPanel checkBoxGroup = new CheckFormGroupPanel(null,
                $(Schrodinger.byDataResourceKey(titleResourceKey)).parent().parent().waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
        checkBoxGroup.setOptionCheckedById(checked);
    }

    public static <P extends AssignmentHolderDetailsPage> void removeAssignments(AssignmentsTab<P> tab, String... assignments) {
        AbstractTableWithPrismView<AssignmentsTab<P>> table = tab.table();
        for (String assignment : assignments) {
            table.removeByName(assignment);
        }
        tab
        .and()
        .clickSave()
            .feedback()
                .isSuccess();

    }

    public static <P extends AssignmentHolderDetailsPage> void removeAllAssignments(AssignmentsTab<P> tab) {
        tab
                .table()
                    .selectHeaderCheckbox()
                    .clickHeaderActionButton("fa fa-minus fa-fw");
    }

    public static <P extends AssignmentHolderDetailsPage> void addAsignments(AssignmentsTab<P> tab, String... assignments){
        for (String assignment : assignments) {
            tab.clickAddAssignemnt()
                .table()
                    .search()
                        .byName()
                            .inputValue(assignment)
                            .updateSearch()
                        .and()
                    .selectCheckboxByName(assignment)
                    .and()
                .clickAdd();
        }

        tab.and()
            .clickSave()
                .feedback()
                    .isSuccess();
    }

    public static <P extends AssignmentHolderDetailsPage> void setStatusForAssignment(AssignmentsTab<P> tab, String assignment, String status) {
        tab.table()
                    .clickByName(assignment)
                        .showEmptyAttributes("Activation")
                        .setDropDownAttributeValue(ActivationType.F_ADMINISTRATIVE_STATUS , status)
                        .and()
                    .and()
                .and()
                .clickSave()
                    .feedback()
                        .isSuccess();
    }

    public static SelenideElement getModalWindowSelenideElement() {
        return $(By.className("wicket-modal"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
    }
}
