/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.modal.FocusSetAssignmentsModal;
import com.evolveum.midpoint.schrodinger.component.common.PrismFormWithActionButtons;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTableWithPrismView;
import com.evolveum.midpoint.schrodinger.component.table.DirectIndirectAssignmentTable;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AssignmentsTab<P extends AssignmentHolderDetailsPage> extends Component<P> {

    public AssignmentsTab(P parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public <A extends AssignmentsTab<P>> AbstractTableWithPrismView<A> table() {

        SelenideElement tableBox = $(Schrodinger.byDataId("div", "assignmentsTable"));

        return new AbstractTableWithPrismView<A>((A) this, tableBox) {
            @Override
            public PrismFormWithActionButtons<AbstractTableWithPrismView<A>> clickByName(String name) {

                $(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

                SelenideElement prismElement = $(Schrodinger.byDataId("div", "assignmentsContainer"))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

                return new PrismFormWithActionButtons<>(this, prismElement);
            }

            @Override
            public AbstractTableWithPrismView<A> selectCheckboxByName(String name) {

                $(Schrodinger.byFollowingSiblingEnclosedValue("td", "class", "check", "data-s-id", "3", name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

                return this;
            }

            public AbstractTableWithPrismView<A> removeByName(String name) {

                $(Schrodinger.byAncestorPrecedingSiblingDescendantOrSelfElementEnclosedValue("button", "title", "Unassign", null, null, name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

                return this;
            }
        };
    }

    public <A extends AssignmentsTab<P>> FocusSetAssignmentsModal<A> clickAddAssignemnt() {
        $(Schrodinger.byElementAttributeValue("i", "class", "fe fe-assignment "))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        SelenideElement modalElement = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Select object(s)"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new FocusSetAssignmentsModal<A>((A) this, modalElement);
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
}
