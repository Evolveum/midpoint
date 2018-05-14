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
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTable;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UserProjectionsTab extends Component<UserPage> {
    public UserProjectionsTab(UserPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public UserProjectionsCog<UserProjectionsTab> clickCog() {

        $(Schrodinger.byElementAttributeValue("a", "about", "dropdownMenu"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        SelenideElement dropDownMenu = $(Schrodinger.byElementAttributeValue("ul", "class", "dropdown-menu pull-right"));

        return new UserProjectionsCog<>(this, dropDownMenu);
    }

    public AbstractTable<UserProjectionsTab> table() {

        SelenideElement tableBox = $(By.cssSelector(".box.projection"));

        return new AbstractTable<UserProjectionsTab>(this, tableBox) {
            @Override
            public PrismForm<AbstractTable<UserProjectionsTab>> clickByName(String name) {

                $(Schrodinger.byElementValue("span", "data-s-id", "name", name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

                SelenideElement prismElement = $(By.cssSelector(".container-fluid.prism-object"))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT);

                return new PrismForm<>(this, prismElement);
            }

            @Override
            public AbstractTable<UserProjectionsTab> selectCheckboxByName(String name) {

                $(Schrodinger.byFollowingSiblingElementValue("input", "type", "checkbox", "class", "check-table-label", name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

                return this;
            }
        };
    }
}
