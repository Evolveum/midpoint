/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.component.common.search;

import com.codeborne.selenide.*;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.InputBox;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;
import org.openqa.selenium.interactions.Actions;

import static com.codeborne.selenide.Selectors.byText;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Search<T> extends Component<T> {

    public Search(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public TextInputSearchItemPanel<Search<T>> byName() {
        choiceBasicSearch();

        SelenideElement nameElement = getItemByName("Name");
        if (nameElement == null){
            addSearchItemByNameLinkClick("Name");
            nameElement = getItemByName("Name");
        }
        SelenideElement nameInput = nameElement.parent().$x(".//input[@" + Schrodinger.DATA_S_ID + "='input']")
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new TextInputSearchItemPanel(this, nameInput);
    }

    public TextInputSearchItemPanel<Search<T>> textInputPanelByItemName(String itemName) {
        return textInputPanelByItemName(itemName, true);
    }

    public TextInputSearchItemPanel<Search<T>> textInputPanelByItemName(String itemName, boolean addIfAbsent) {
        return new TextInputSearchItemPanel(this, getItemSearchElement(itemName, addIfAbsent));
    }

    public DropDownSearchItemPanel<Search<T>> dropDownPanelByItemName(String itemName) {
        return dropDownPanelByItemName(itemName, true);
    }

    public DropDownSearchItemPanel<Search<T>> dropDownPanelByItemName(String itemName, boolean addIfAbsent) {
        return new DropDownSearchItemPanel(this, getItemSearchElement(itemName, addIfAbsent));
    }

    public ReferenceSearchItemPanel<Search<T>> referencePanelByItemName(String itemName) {
        return referencePanelByItemName(itemName, true);
    }

    public ReferenceSearchItemPanel<Search<T>> referencePanelByItemName(String itemName, boolean addIfAbsent) {
        return new ReferenceSearchItemPanel(this, getItemSearchElement(itemName, addIfAbsent));
    }

    public DateIntervalSearchItemPanel<Search<T>> dateIntervalPanelByItemName(String itemName) {
        return dateIntervalPanelByItemName(itemName, true);
    }

    public DateIntervalSearchItemPanel<Search<T>> dateIntervalPanelByItemName(String itemName, boolean addIfAbsent) {
        return new DateIntervalSearchItemPanel<>(this, getItemSearchElement(itemName, addIfAbsent));
    }

    public ItemPathSearchItemPanel<Search<T>> itemPathPanelByItemName(String itemName) {
        return itemPathPanelByItemName(itemName, true);
    }

    public ItemPathSearchItemPanel<Search<T>> itemPathPanelByItemName(String itemName, boolean addIfAbsent) {
        return new ItemPathSearchItemPanel<>(this, getItemSearchElement(itemName, addIfAbsent));
    }

    private SelenideElement getItemSearchElement(String itemName, boolean addIfAbsent) {
        choiceBasicSearch();
        SelenideElement itemElement = getItemByName(itemName);
        if (itemElement == null && addIfAbsent){
            addSearchItemByNameLinkClick(itemName);
            itemElement = getItemByName(itemName);
        }
        if (itemElement == null){
            return null;
        }
        return itemElement;
    }

    public Search<T> updateSearch(){
        SelenideElement simpleSearchButton = getParentElement().$x(".//button[@" + Schrodinger.DATA_S_ID + "='searchButtonBeforeDropdown']")
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);
        Actions builder = new Actions(WebDriverRunner.getWebDriver());
        builder.moveToElement(simpleSearchButton, 5, 5).click().build().perform();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return this;
    }

    private void choiceBasicSearch() {
        clickDroDownForSearchMode();
        try {
            SelenideElement basicLink = getParentElement().$x(".//a[@"+Schrodinger.DATA_S_ID+"='menuItemLink' and contains(text(), 'Basic')]");
            basicLink.waitUntil(Condition.appears, MidPoint.TIMEOUT_MEDIUM_6_S).click();
            basicLink.waitWhile(Condition.appears, MidPoint.TIMEOUT_MEDIUM_6_S);
        } catch (Throwable t) {
            getParentElement().$x(".//a[@"+Schrodinger.DATA_S_ID+"='more']").waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);
        }
    }

    private void clickDroDownForSearchMode() {
        SelenideElement dropDownButton = getParentElement().$x(".//div[@"+Schrodinger.DATA_S_ID+"='searchContainer']").$x(".//button[@data-toggle='dropdown']");
        dropDownButton.waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        dropDownButton.waitUntil(Condition.attribute("aria-expanded", "true"), MidPoint.TIMEOUT_MEDIUM_6_S);
    }

    public InputBox<Search<T>> byFullText() {
        clickDroDownForSearchMode();
        try {
            SelenideElement basicLink = getParentElement().$x(".//a[@"+Schrodinger.DATA_S_ID+"='menuItemLink' and contains(text(), 'Full text')]");
            basicLink.waitUntil(Condition.appears, MidPoint.TIMEOUT_MEDIUM_6_S).click();
            basicLink.waitWhile(Condition.appears, MidPoint.TIMEOUT_MEDIUM_6_S);
        } catch (Throwable t) {
            // all is ok, fullText search is already selected option, check is provided next in next row
        }

        // we assume fulltext is enabled in systemconfig, else error is thrown here:
        SelenideElement fullTextField = getParentElement().$(Schrodinger.byDataId("input", "fullTextField")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new InputBox<> (this, fullTextField);
    }

    public Search<T> addSearchItemByNameLinkClick(String name) {
        choiceBasicSearch();
        getParentElement().$x(".//a[@"+Schrodinger.DATA_S_ID+"='more']").waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        SelenideElement popover = getDisplayedPopover();
        popover.$(Schrodinger.byElementValue("a", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return this;
    }

    public Search<T> addSearchItemByAddButtonClick(String name) {
        choiceBasicSearch();
        getParentElement().$x(".//a[@"+Schrodinger.DATA_S_ID+"='more']").waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        SelenideElement popover = getDisplayedPopover();
        popover.$(Schrodinger.byElementValue("a", name))            //click checkbox next to search attribute
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S)
                .parent().$(By.tagName("input")).click();
        popover.$x(".//a[@"+Schrodinger.DATA_S_ID+"='add']").waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click(); //click Add button
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return this;
    }

    public SelenideElement getItemByName(String name) {
        ElementsCollection items = getParentElement().findAll(By.className("search-item"));
        for (SelenideElement item : items) {
            if (item.$(byText(name)).exists()) {
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
        ElementsCollection deleteButtons = getParentElement().$$(Schrodinger.byDataId("removeButton"));
        int i = 0;
        int size = deleteButtons.size();
        while (i < size) {
            SelenideElement deleteButton = deleteButtons.get(0);
            if (deleteButton.isDisplayed()) {
                deleteButton.click();
            }
            i++;
            Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
            deleteButtons = getParentElement().$$(Schrodinger.byDataId("removeButton"));
        }
        for (SelenideElement deleteButton : deleteButtons) {
            if (deleteButton.isDisplayed()) {
                deleteButton.click();
            }
        }
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return this;
    }

    public Search<T> clearTextSearchItemByNameAndUpdate(String itemName) {
        choiceBasicSearch();
        SelenideElement itemElement = getItemByName(itemName);
        if (itemElement == null){
            return this;
        }
        TextInputSearchItemPanel searchField = new TextInputSearchItemPanel(this, itemElement);
        searchField.inputValue("");
        updateSearch();
        return this;
    }
}

