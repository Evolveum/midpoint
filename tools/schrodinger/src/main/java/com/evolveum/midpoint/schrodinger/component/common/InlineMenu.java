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
import java.util.Objects;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

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

        ElementsCollection lis = getParentElement().findAll("div.btn-group ul.dropdown-menu li a");
        for (SelenideElement a : lis) {
            if (!a.isDisplayed()) {
                continue;
            }

            String txt = a.getText();
            if (txt != null) {
                items.add(txt.trim());
            }
        }

        return items;
    }

    public List<String> getItemKeys() {
        List<String> items = new ArrayList<>();

        ElementsCollection lis = getParentElement().findAll("div.btn-group ul.dropdown-menu li a schrodinger[data-s-resource-key]");
        for (SelenideElement schrodinger : lis) {
            if (!schrodinger.parent().isDisplayed()) {
                continue;
            }

            String key = schrodinger.getAttribute("data-s-resource-key");
            if (key != null) {
                items.add(key.trim());
            }
        }

        return items;
    }

    public InlineMenu<T> clickItemByName(String itemName) {
        boolean found = false;
        ElementsCollection lis = getParentElement().findAll("div.btn-group ul.dropdown-menu li a");
        for (SelenideElement a : lis) {
            if (!a.isDisplayed()) {
                continue;
            }

            String txt = a.getText();
            if (Objects.equals(itemName, txt.trim())) {
                a.parent().click();
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalStateException("Couldn't find item by name " + itemName);
        }

        return this;
    }

    public InlineMenu<T> clickItemByKey(String itemKey) {
        SelenideElement element = getParentElement().find("div.btn-group ul.dropdown-menu li a schrodinger[data-s-resource-key=" + itemKey + "]");
        element.parent().click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return this;
    }

    public InlineMenu<T> clickInlineMenuButtonByTitle(String title) {
        getParentElement().$(Schrodinger.byElementAttributeValue("button", "title", title))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }

    public InlineMenu<T> clickInlineMenuButtonByIconClass(String iconClass) {
        getParentElement().$(By.cssSelector(iconClass))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }
}
