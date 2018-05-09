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
import com.evolveum.midpoint.schrodinger.component.common.Search;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithRedirectElements;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UsersTable<T> extends TableWithRedirectElements<T> {

    public UsersTable(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public TableWithRedirectElements<T> selectCheckboxByName(String name) {
        return null;
    }

    @Override
    public UserPage clickByName(String name) {

        getParentElement().$(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return new UserPage();
    }

    @Override
    public Search<UsersTable<T>> search() {
        SelenideElement searchElement = getParentElement().$(By.cssSelector(".form-inline.pull-right.search-form"));

        return new Search<>(this, searchElement);
    }
}
