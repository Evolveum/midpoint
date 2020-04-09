/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.modal.FocusSetAssignmentsModal;
import com.evolveum.midpoint.schrodinger.component.common.PrismFormWithActionButtons;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTableWithPrismView;
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

    public AbstractTableWithPrismView<AssignmentsTab<P>> table() {

        SelenideElement tableBox = $(Schrodinger.byDataId("div", "assignmentsTable"));

        return new AbstractTableWithPrismView<AssignmentsTab<P>>(this, tableBox) {
            @Override
            public PrismFormWithActionButtons<AbstractTableWithPrismView<AssignmentsTab<P>>> clickByName(String name) {

                $(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

                SelenideElement prismElement = $(Schrodinger.byDataId("div", "assignmentsContainer"))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

                return new PrismFormWithActionButtons<>(this, prismElement);
            }

            @Override
            public AbstractTableWithPrismView<AssignmentsTab<P>> selectCheckboxByName(String name) {

                $(Schrodinger.byFollowingSiblingEnclosedValue("td", "class", "check", "data-s-id", "3", name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

                return this;
            }

            public AbstractTableWithPrismView<AssignmentsTab<P>> removeByName(String name) {

                $(Schrodinger.byAncestorPrecedingSiblingDescendantOrSelfElementEnclosedValue("button", "title", "Unassign", null, null, name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

                return this;
            }
        };
    }

    public FocusSetAssignmentsModal<AssignmentsTab<P>> clickAddAssignemnt() {
        $(Schrodinger.byElementAttributeValue("i", "class", "fe fe-assignment "))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        SelenideElement modalElement = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Select object(s)"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new FocusSetAssignmentsModal<>(this, modalElement);
    }

    public boolean assignmentExists(String assignmentName){
        SelenideElement assignmentSummaryDisplayName = table()
                .clickByName(assignmentName)
                    .getParentElement()
                        .$(Schrodinger.byDataId("displayName"));
        return assignmentName.equals(assignmentSummaryDisplayName.getText());
    }
}
