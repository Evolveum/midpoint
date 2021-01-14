/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.modal.FocusSetAssignmentsModal;
import com.evolveum.midpoint.schrodinger.component.common.PrismFormWithActionButtons;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTableWithPrismView;
import com.evolveum.midpoint.schrodinger.component.table.DirectIndirectAssignmentTable;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.schrodinger.util.Utils;

import org.testng.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AssignmentsTab<P extends AssignmentHolderDetailsPage> extends TabWithTableAndPrismView<P> {

    public AssignmentsTab(P parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }


    public <A extends AssignmentsTab<P>> FocusSetAssignmentsModal<A> clickAddAssignemnt() {
        $(Schrodinger.byElementAttributeValue("i", "class", "fe fe-assignment "))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        SelenideElement modalElement = getNewAssignmentModal();

        return new FocusSetAssignmentsModal<A>((A) this, modalElement);
    }

    public <A extends AssignmentsTab<P>> FocusSetAssignmentsModal<A> clickAddAssignemnt(String title) {
        $(Schrodinger.byElementAttributeValue("div", "title", title))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        SelenideElement modalElement = getNewAssignmentModal();

        return new FocusSetAssignmentsModal<A>((A) this, modalElement);
    }

    private SelenideElement getNewAssignmentModal() {
        return Utils.getModalWindowSelenideElement();
    }


    public boolean assignmentExists(String assignmentName){
        SelenideElement assignmentSummaryDisplayName = table()
                .clickByName(assignmentName)
                    .getParentElement()
                        .$(Schrodinger.byDataId("displayName"));
        return assignmentName.equals(assignmentSummaryDisplayName.getText());
    }

    public <A extends AssignmentsTab<P>> A selectTypeAll() {
        selectType("allAssignments");
        return (A) this;
    }

    public <A extends AssignmentsTab<P>> A selectTypeRole() {
        selectType("roleTypeAssignments");
        return (A) this;
    }

    public <A extends AssignmentsTab<P>> A selectTypeOrg() {
        selectType("orgTypeAssignments");
        return (A) this;
    }

    public <A extends AssignmentsTab<P>> A selectTypeService() {
        selectType("serviceAssignments");
        return (A) this;
    }

    public <A extends AssignmentsTab<P>> A selectTypeResource() {
        selectType("resourceAssignments");
        return (A) this;
    }

    public <A extends AssignmentsTab<P>> DirectIndirectAssignmentTable<A> selectTypeAllDirectIndirect() {
        selectType("showIndirectAssignmentsButton");
        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);
        SelenideElement table = $(Schrodinger.byDataId("table", "table"));

        return new DirectIndirectAssignmentTable<A>((A) this, table);
    }

    protected void selectType(String resourceKey) {
        $(Schrodinger.byDataId("div", resourceKey)).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
    }

    public boolean containsAssignmentsWithRelation(String relation, String... expectedAssignments) {
        String relationString = relation.equals("Default") ? "" : ("Relation: " + relation);
        ElementsCollection labels = getParentElement()
                .$$(Schrodinger.byAncestorFollowingSiblingDescendantOrSelfElementEnclosedValue("span", "data-s-id", "label",
                        "data-s-id", "5", relationString));
        List<String> indirectAssignments = new ArrayList<String>();
        for (SelenideElement label : labels) {
            if (!label.getText().isEmpty()) {
                indirectAssignments.add(label.getText());
            }
        }
        return indirectAssignments.containsAll(Arrays.asList(expectedAssignments));
    }

    @Override
    protected String getPrismViewPanelId() {
        return "assignmentsContainer";
    }

    public AssignmentsTab<P> assertAssignmentsWithRelationExist(String relation, String... expectedAssignments) {
        Assert.assertEquals(containsAssignmentsWithRelation(relation, expectedAssignments), "Assignments doesn't exist.");
        return this;
    }
}
