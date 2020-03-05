/*
 * Copyright (c) 2010-2020 Evolveum
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

package com.evolveum.midpoint.schrodinger.component.common.table;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.InlineMenu;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TableRow<X, T extends Table<X>> extends Component<T> {

    public TableRow(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public TableRow clickCheckBox() {
        getParentElement().find("input[type=checkbox]").click();
        // todo implement
        return this;
    }

    public InlineMenu<TableRow> getInlineMenu() {
        SelenideElement element = getParentElement().find("td:last-child div.btn-group");
        if (element == null) {
            return null;
        }

        return new InlineMenu<>(this, element);
    }
}
