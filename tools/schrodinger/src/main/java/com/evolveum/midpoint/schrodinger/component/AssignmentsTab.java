/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

            @Override
            public AbstractTableWithPrismView<AssignmentsTab<P>> unassignByName(String name) {

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

        return new FocusSetAssignmentsModal<AssignmentsTab<P>>(this, modalElement);
    }

    public boolean assignmentExists(String assignmentName){
        SelenideElement assignmentSummaryDisplayName = table()
                .clickByName(assignmentName)
                    .getParentElement()
                        .$(Schrodinger.byDataId("displayName"));
        return assignmentName.equals(assignmentSummaryDisplayName.getText());
    }
}
