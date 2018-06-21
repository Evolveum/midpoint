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

package com.evolveum.midpoint.schrodinger.component.user;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.FocusSetAssignmentsModal;
import com.evolveum.midpoint.schrodinger.component.common.PrismFormWithActionButtons;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTableWithPrismView;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UserAssignmentsTab extends Component<UserPage> {

    public UserAssignmentsTab(UserPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public AbstractTableWithPrismView<UserAssignmentsTab> table() {

        SelenideElement tableBox = $(Schrodinger.byDataId("div", "assignmentsTable"));

        return new AbstractTableWithPrismView<UserAssignmentsTab>(this, tableBox) {
            @Override
            public PrismFormWithActionButtons<AbstractTableWithPrismView<UserAssignmentsTab>> clickByName(String name) {

                $(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

                SelenideElement prismElement = $(Schrodinger.byDataId("div", "assignmentsContainer"))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

                return new PrismFormWithActionButtons<>(this, prismElement);
            }

            @Override
            public AbstractTableWithPrismView<UserAssignmentsTab> selectCheckboxByName(String name) {

                $(Schrodinger.byFollowingSiblingEnclosedValue("td", "class", "check", "data-s-id", "3", name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

                return this;
            }

            @Override
            public AbstractTableWithPrismView<UserAssignmentsTab> unassignByName(String name) {

                $(Schrodinger.byAncestorPrecedingSiblingElementValue("button", "title", "Unassign", null, null, name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

                return this;
            }
        };
    }

    public FocusSetAssignmentsModal<UserAssignmentsTab> clickAddAssignemnt() {
        $(Schrodinger.byDataId("div", "newAssignmentButton"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        SelenideElement modalElement = $(Schrodinger.byElementAttributeValue("div", "aria-labelledby", "Select object(s)"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

        return new FocusSetAssignmentsModal<>(this, modalElement);
    }
}
