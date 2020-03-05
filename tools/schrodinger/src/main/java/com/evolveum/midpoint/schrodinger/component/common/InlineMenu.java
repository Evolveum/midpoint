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

package com.evolveum.midpoint.schrodinger.component.common;

import java.util.ArrayList;
import java.util.List;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.Component;

/**
 * Created by Viliam Repan (lazyman).
 */
public class InlineMenu<T> extends Component<T> {

    public InlineMenu(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public InlineMenu<T> caret() {
        SelenideElement caret = getParentElement().find("div.btn-group span[data-s-id=caret]");
        if (caret != null) {
            caret.click();
        }

        return this;
    }

    public List<String> getItems() {
        List<String> items = new ArrayList<>();

        ElementsCollection lis = getParentElement().findAll("div.btn-group ul.dropdown-menu li");
        for (SelenideElement li : lis) {
//            li.find()
        }
        // todo implement

        return items;
    }

    public List<String> getItemKeys() {
        // todo implement
        return null;
    }

    public InlineMenu<T> clickItem(String itemName) {
        // todo implement
        SelenideElement dropdown = getParentElement().find("div.btn-group ul.dropdown-menu li");

        return this;
    }
}
