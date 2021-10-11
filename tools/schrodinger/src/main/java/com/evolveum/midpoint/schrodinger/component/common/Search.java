/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.*;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Search<T> extends Component<T> {

    public Search(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public Popover<Search<T>> byName() {

        choiceBasicSearch();

        getParentElement().$x(".//a[@"+Schrodinger.DATA_S_ID+"='mainButton']").waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return new Popover<>(this, getDisplayedPopover());
    }

    private void choiceBasicSearch() {
        SelenideElement linksContainer = getParentElement().$(Schrodinger.byDataId("div", "linksContainer")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);
        try {
            linksContainer.$(Schrodinger.byDataId("a", "basic")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        } catch (Throwable t) {
            // all is ok, basic search is already selected option, TODO: Schrodinger should provide easy method to check component existence
        }
    }

    public InputBox<Search<T>> byFullText() {

        SelenideElement linksContainer = getParentElement().$(Schrodinger.byDataId("div", "linksContainer")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);
        try {
            linksContainer.$(Schrodinger.byDataId("a", "fullText")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        } catch (Throwable t) {
            // all is ok, fullText search is already selected option, TODO: Schrodinger should provide easy method to check component existence
        }

        // we assume fulltext is enabled in systemconfig, else error is thrown here:
        SelenideElement fullTextField = getParentElement().$(Schrodinger.byDataId("input", "fullTextField")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new InputBox<> (this, fullTextField);
    }

    public Search<T> addSearchItem(String name) {
        choiceBasicSearch();
        getParentElement().$x(".//a[@"+Schrodinger.DATA_S_ID+"='more']").waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        SelenideElement popover = getDisplayedPopover();
        popover.$x(".//input[@"+Schrodinger.DATA_S_ID+"='addText']").setValue(name);
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        popover.$x(".//a[@"+Schrodinger.DATA_S_ID+"='propLink']").click();
        return this;
    }

    public Popover<Search<T>> byItem(String name) {

        choiceBasicSearch();

        SelenideElement item = getItemByName(name);
        if (item == null) {
            addSearchItem(name);
            Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        }
        item = getItemByName(name);
        if (item == null) {
            throw new IllegalStateException("Couldn't find search item for name " + name);
        }

        item.waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return new Popover<>(this, getDisplayedPopover());
    }

    private SelenideElement getItemByName(String name) {
        ElementsCollection items = getParentElement().findAll(Schrodinger.byDataId("a", "mainButton"));
        for (SelenideElement item : items) {
            if (item.getText().startsWith(name + ":")) {
                return item;
            }
        }
        return null;
    }

    private SelenideElement getDisplayedPopover() {
        ElementsCollection popoverElements = getParentElement().$$(Schrodinger.byDataId("popover"));
        SelenideElement popover = null;
        for (SelenideElement popoverElement : popoverElements) {
            if (popoverElement.isDisplayed()) {
                popover = popoverElement;
                break;
            }
            popover = popoverElement;
        }
        return popover;
    }

    public Search<T> resetBasicSearch() {
        choiceBasicSearch();
        SelenideElement nameItem = getItemByName("Name");
        if (nameItem != null) {
            nameItem.waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
            Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
            new Popover<>(this, nameItem).inputValue("").updateSearch();
        }

        ElementsCollection deleteButtons = getParentElement().$$(Schrodinger.byDataId("deleteButton"));
        for (SelenideElement deleteButton : deleteButtons) {
            if (deleteButton.isDisplayed()) {
                deleteButton.click();
            }
        }
        Selenide.sleep(2000);
        return this;
    }
}

