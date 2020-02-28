/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.component.common.table;

import static com.codeborne.selenide.Selectors.byPartialLinkText;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

import java.util.Objects;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.Paging;
import com.evolveum.midpoint.schrodinger.component.common.Search;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Table<T> extends Component<T> {

    public Table(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public TableRow rowByColumnLabel(String label, String rowValue) {
        ElementsCollection headers = getParentElement().findAll("thead th div span[data-s-id=label]");
        int index = 1;
        for (SelenideElement header : headers) {
            String value = header.text();
            if (value == null) {
                index++;
                continue;
            }

            if (Objects.equals(label, value)) {
                break;
            }
            index++;
        }

        ElementsCollection rows = getParentElement().findAll("tbody tr");
        for (SelenideElement row : rows) {
            String value = row.find("td:nth-child(" + index + ")").text();
            if (value == null) {
                continue;
            }
            value = value.trim();

            if (Objects.equals(rowValue, value)) {
                return new TableRow(this, row);
            }
        }

        return null;
    }

    public TableRow rowByColumnResourceKey(String key, String rowValue) {
        // todo implement
        return null;
    }

    public Search<? extends Table> search() {
        SelenideElement searchElement = getParentElement().$(By.cssSelector(".form-inline.pull-right.search-form"));

        return new Search<>(this, searchElement);
    }

    public Paging<T> paging() {
        SelenideElement pagingElement = getParentElement().$(By.className("boxed-table-footer-paging"));

        return new Paging(this, pagingElement);
    }

    public Table<T> selectAll() {

        $(Schrodinger.bySelfOrAncestorElementAttributeValue("input", "type", "checkbox", "data-s-id", "topToolbars"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this;
    }

    public boolean currentTableContains(String elementValue) {
        return currentTableContains("Span", elementValue);
    }

    public boolean currentTableContains(String elementName, String elementValue) {

        // TODO replate catch Throwable with some less generic error
        try {
            return $(Schrodinger.byElementValue(elementName, elementValue)).waitUntil(Condition.visible, MidPoint.TIMEOUT_MEDIUM_6_S).is(Condition.visible);
        } catch (Throwable t) {
            return false;
        }

    }

    public boolean containsText(String value) {
        return $(byText(value)).is(Condition.visible);
    }

    public boolean containsLinkTextPartially(String value) {
        return $(byPartialLinkText(value)).is(Condition.visible);
    }

    public boolean buttonToolBarExists() {
        return $(Schrodinger.byDataId("buttonToolbar")).exists();
    }

    public SelenideElement getButtonToolbar() {
        return $(Schrodinger.byDataId("buttonToolbar"));
    }

}
