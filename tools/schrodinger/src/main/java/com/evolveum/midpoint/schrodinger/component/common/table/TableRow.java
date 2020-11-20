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

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;

import com.evolveum.midpoint.schrodinger.component.common.InputBox;

import org.openqa.selenium.By;

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
        getParentElement().find("input[type=checkbox]")
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }

    public TableRow clickCheckBoxByColumnName(String columnName) {
        int index = getParent().findColumnByLabel(columnName);
        if (index < 0) {
            return null;
        }
        getParentElement().$(By.cssSelector("td:nth-child(" + index + ") checkbox"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }

    public TableRow clickColumnByName(String name) {
        int index = getParent().findColumnByLabel(name);
        if (index < 0) {
            return this;
        }
        SelenideElement a = getParentElement().$(By.cssSelector("td:nth-child(" + index + ") a"));
        a.click();
        // todo implement
        return this;
    }

    public TableRow setTextToInputFieldByColumnName(String columnName, String textValue) {
        int index = getParent().findColumnByLabel(columnName);
        if (index < 0) {
            return this;
        }
        SelenideElement input = getParentElement().$(By.cssSelector("td:nth-child(" + index + ") input"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        InputBox inputBox = new InputBox(this, input);
        inputBox.inputValue(textValue);
        return this;
    }

    public TableRow setValueToDropdownFieldByColumnName(String columnName, String textValue) {
        int index = getParent().findColumnByLabel(columnName);
        if (index < 0) {
            return this;
        }
        SelenideElement select = getParentElement().$(By.cssSelector("td:nth-child(" + index + ") select"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        select.selectOption(textValue);
        return this;
    }

    public TableRow setValueToDropDownByColumnName(String columnName, String selectValue) {
        int index = getParent().findColumnByLabel(columnName);
        if (index < 0) {
            return this;
        }
        getParentElement().$(By.cssSelector("td:nth-child(" + index + ") select"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).selectOptionContainingText(selectValue);
        return this;
    }

    public SelenideElement getColumnCellElementByColumnName(String columnName) {
        int index = getParent().findColumnByLabel(columnName);
        if (index < 0) {
            return null;
        }
        SelenideElement cell = getParentElement().$(By.cssSelector("td:nth-child(" + index + ") div"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return cell;
    }

    public TableRow clickColumnByKey(String key) {
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
