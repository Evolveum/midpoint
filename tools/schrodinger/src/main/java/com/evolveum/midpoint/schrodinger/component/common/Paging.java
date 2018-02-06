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

package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.SchrodingerException;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.jsoup.helper.Validate;
import org.openqa.selenium.By;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Paging<T> extends Component<T> {

    public Paging(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public Paging<T> first() {
        getParentElement().$(Schrodinger.byElementValue("a", "<<")).click();
        return this;
    }

    public Paging<T> previous() {
        getParentElement().$(Schrodinger.byElementValue("a", "<")).click();
        return this;
    }

    public Paging<T> next() {
        getParentElement().$(Schrodinger.byElementValue("a", ">")).click();
        return this;
    }

    public Paging<T> last() {
        getParentElement().$(Schrodinger.byElementValue("a", ">>")).click();
        return this;
    }

    private void moveThroughPages(int offsetFromActual) {
        SelenideElement ul = getParentElement().$(By.cssSelector(".pagination.pagination-sm.no-margin.pull-right"));

        ElementsCollection col = ul.$$x(".//li");
        SelenideElement active = col.find(Condition.cssClass("active"));
        int index = col.indexOf(active);

        index = index + offsetFromActual;
        if (index < 2 || index > col.size() - 2) {
            // it's <<, <, >, >>
            throw new SchrodingerException("Can't move through paging, page doesn't exist");
        }

        col.get(index).$x(".//a").click();
    }

    public Paging<T> actualPageMinusOne() {
        moveThroughPages(-1);
        return this;
    }

    public Paging<T> actualPageMinusTwo() {
        moveThroughPages(-2);
        return this;
    }

    public Paging<T> actualPagePlusOne() {
        moveThroughPages(1);
        return this;
    }

    public Paging<T> actualPagePlusTwo() {
        moveThroughPages(2);
        return this;
    }

    public Paging<T> pageSize(int size) {
        Validate.isTrue(size > 0, "Size must be larger than zero.");

        SelenideElement parent = getParentElement();

        SelenideElement button = parent.$(By.cssSelector(".btn.btn-default.dropdown-toggle"));
        button.click();

        button.parent().$(By.className("dropdown-menu")).$$x(".//a").first().click();

        SelenideElement popover = parent.$$(By.className("popover-title"))
                .findBy(Condition.text("Page size")).parent(); //todo fix localization

        popover.$(By.tagName("input")).setValue(Integer.toString(size));
        popover.$(By.tagName("button")).click();

        return this;
    }

    public int currentPageNumber() {
        // todo implement
        return -1;
    }

    public int currentMaxPages() {
        // todo implement
        return -1;
    }
}
