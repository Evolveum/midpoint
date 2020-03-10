/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import javax.xml.namespace.QName;

import java.io.File;
import java.util.List;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PrismForm<T> extends Component<T> {

    private static final String CARET_DOWN_ICON_STYLE = "fa-caret-down";

    public PrismForm(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public PrismForm<T> addAttributeValue(String name, String value) {
        SelenideElement property = findProperty(name);

        $(By.className("prism-properties")).waitUntil(Condition.appears,MidPoint.TIMEOUT_MEDIUM_6_S);

        ElementsCollection values = property.$$(By.className("prism-property-value"));
        if (values.size() == 1) {
            values.first().$(By.className("form-control")).setValue(value);
        }

        // todo implement
        return this;
    }

    public PrismForm<T> addProtectedAttributeValue(String protectedAttributeName, String value) {
        SelenideElement property = findProperty(protectedAttributeName);

        boolean existValue = $(Schrodinger.byDataId("changePasswordLink")).exists();
        if (existValue) {
            $(Schrodinger.byDataId("changePasswordLink")).click();
        }

        ElementsCollection values = property.$$(By.xpath(".//input[contains(@class,\"form-control\")]"));
        for (SelenideElement valueElemen : values) {
            valueElemen.setValue(value).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        }

        return this;
    }

    public PrismForm<T> removeAttributeValue(String name, String value) {
        // todo implement
        return this;
    }

    public PrismForm<T> changeAttributeValue(String name, String oldValue, String newValue) {
        SelenideElement property = $(Schrodinger.byDataResourceKey(name));

        $(By.className("prism-properties")).waitUntil(Condition.appears,MidPoint.TIMEOUT_MEDIUM_6_S);

        ElementsCollection values = property.$$(By.className("prism-property-value"));
        if (values.size() == 1) {
            values.first().$(By.className("form-control")).waitUntil(Condition.appears,MidPoint.TIMEOUT_MEDIUM_6_S).setValue(newValue);
        }

        // todo implement
        return this;
    }


    public PrismForm<T> setFileForUploadAsAttributeValue(String name, File file) {
        $(By.cssSelector("input.form-object-value-binary-file-input")).uploadFile(file);

        return this;
    }

    public PrismForm<T> removeFileAsAttributeValue(String name) {
        SelenideElement property = findProperty(name);
        property.$(Schrodinger.byElementAttributeValue("a", "title", "Remove file")).click();

        return this;
    }

    public PrismForm<T> showEmptyAttributes(String containerName) {
        $(Schrodinger.byAncestorPrecedingSiblingDescendantOrSelfElementEnclosedValue("div", "data-s-id", "showEmptyButton", "class", "prism-properties", containerName))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this;
    }

    public Boolean compareInputAttributeValue(String name, String expectedValue) {
        SelenideElement property = findProperty(name);
        SelenideElement value = property.parent().$(By.xpath(".//input[contains(@class,\"form-control\")]"));
        String valueElement = value.getValue();

        if (!valueElement.isEmpty()) {
            return valueElement.equals(expectedValue);
        } else {
            return expectedValue.isEmpty();
        }

    }

    public Boolean compareSelectAttributeValue(String name, String expectedValue) {
        SelenideElement property = findProperty(name);
        SelenideElement value = property.$(By.xpath(".//select[contains(@class,\"form-control\")]"));
        String selectedOptionText = value.getSelectedText();

        if (!selectedOptionText.isEmpty()) {
            return selectedOptionText.equals(expectedValue);
        } else {
            return expectedValue.isEmpty();
        }

    }

    public PrismForm<T> addAttributeValue(QName name, String value) {
        SelenideElement property = findProperty(name);

        ElementsCollection values = property.$$(By.className("prism-property-value"));
        if (values.size() == 1) {
            values.first().$(By.className("form-control")).setValue(value);
        }
        // todo implement
        return this;
    }

    public PrismForm<T> setPasswordFieldsValues(QName name, String value) {
        SelenideElement property = findProperty(name);

        ElementsCollection values = property.$$(By.className("prism-property-value"));
        if (values.size() > 0) {
            ElementsCollection passwordInputs = values.first().$$(By.tagName("input"));
            if (passwordInputs != null){
                passwordInputs.forEach(inputElement -> inputElement.setValue(value));
            }
        }
        return this;
    }

    public PrismForm<T> setPolyStringLocalizedValue(QName name, String locale, String value) {
        SelenideElement property = findProperty(name);

        property
                .$(By.className("fa-language"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
        SelenideElement localeInput =
                property
                        .$(Schrodinger.byDataId("fullDataContainer"))
                        .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                        .$(Schrodinger.byElementAttributeValue("input", "value", locale));
        boolean localeInputExists = localeInput.exists();
        if (!localeInputExists){
            SelenideElement localeDropDown =
                    property
                    .$(Schrodinger.byDataId("languagesList"))
                            .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                    .$(By.tagName("select"))
                            .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
            if (localeDropDown != null){
                localeDropDown.selectOption(locale);

                property
                        .$(Schrodinger.byDataId("languageEditor"))
                        .$(By.className("fa-plus-circle"))
                        .shouldBe(Condition.visible)
                        .click();
            }

            localeInput =
                    property
                            .$(Schrodinger.byDataId("fullDataContainer"))
                            .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                            .$(Schrodinger.byElementAttributeValue("input", "value", locale))
                            .shouldBe(Condition.visible);
        }

        localeInput
                .parent()
                .parent()
                .$(Schrodinger.byDataId("translation"))
                .shouldBe(Condition.visible)
                .$(By.className("form-control"))
                .setValue(value);

        return this;
    }

    public PrismForm<T> setDropDownAttributeValue(QName name, String value) {
        SelenideElement property = findProperty(name);

        ElementsCollection values = property.$$(By.className("prism-property-value"));
        if (values.size() > 0) {
            SelenideElement dropDown = values.first().$(By.tagName("select"));
            if (dropDown != null){
                dropDown.selectOptionContainingText(value);
            }
        }
        return this;
    }

    public PrismForm<T> setAttributeValue(QName name, String value) {
        // todo implement
        return this;
    }

    public PrismForm<T> removeAttributeValue(QName name, String value) {
        // todo implement
        return this;
    }

    public PrismForm<T> changeAttributeValue(QName name, String oldValue, String newValue) {
        // todo implement
        return this;
    }

    public PrismForm<T> showEmptyAttributes(QName containerName, String value) {
        // todo implement
        return this;
    }

    public PrismForm<T> setFileForUploadAsAttributeValue(QName containerName, File file) {
        // todo implement
        return this;
    }

    public PrismForm<T> removeFileAsAttributeValue(QName containerName) {
        // todo implement
        return this;
    }

    private SelenideElement findPropertyValueInput(String name) {
        Selenide.sleep(5000);

        return  $(Schrodinger.byElementAttributeValue("div", "contains",
                Schrodinger.DATA_S_QNAME, "#" + name)).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);

    }

    public SelenideElement findProperty(String name) {

        Selenide.sleep(5000);

        SelenideElement element = null;

        boolean doesElementAttrValueExist = $(Schrodinger.byElementAttributeValue(null, "contains",
                Schrodinger.DATA_S_QNAME, "#" + name)).exists();

        if (doesElementAttrValueExist) {
            element = $(Schrodinger.byElementAttributeValue(null, "contains",
                    Schrodinger.DATA_S_QNAME, "#" + name)).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);

        } else {
            element = $(By.xpath("//span[@data-s-id=\"label\"][contains(.,\"" + name + "\")]/..")).waitUntil(Condition.visible, MidPoint.TIMEOUT_MEDIUM_6_S)
                    .parent().waitUntil(Condition.visible, MidPoint.TIMEOUT_MEDIUM_6_S);
        }

        return element;
    }

    private SelenideElement findProperty(QName qname) {
        String name = Schrodinger.qnameToString(qname);
        return $(Schrodinger.byDataQName(name));
    }

    public PrismForm<T> selectOption(String attributeName, String option) {

        SelenideElement property = findProperty(attributeName);

        property.$(By.xpath(".//select[contains(@class,\"form-control\")]"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).selectOption(option);

        return this;
    }

    public PrismForm<T> expandContainerPropertiesPanel(String containerHeaderKey){
        SelenideElement panelHeader = $(Schrodinger.byElementAttributeValue("a", "data-s-resource-key", containerHeaderKey))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .parent()
                .parent();

        SelenideElement headerChevron = panelHeader.$(By.tagName("i"));
        if (headerChevron.getAttribute("class") != null && !headerChevron.getAttribute("class").contains(CARET_DOWN_ICON_STYLE)) {
            headerChevron.click();
            panelHeader
                    .$(Schrodinger.byElementAttributeValue("i", "class","fa fa-caret-down fa-lg"))
                    .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        }
        panelHeader
                .parent()
                .$(By.className("prism-properties"))
                .shouldBe(Condition.visible);
        return this;
    }

    public PrismForm<T> addNewContainerValue(String containerHeaderKey, String newContainerHeaderKey){
        SelenideElement panelHeader = $(By.linkText(containerHeaderKey))
                .parent()
                .parent();
        panelHeader.scrollTo();
        panelHeader.find(By.className("fa-plus-circle"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();

        panelHeader
                .parent()
                .parent()
                .$(By.linkText(newContainerHeaderKey))
                .shouldBe(Condition.visible)
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);

        return this;
    }

    public SelenideElement getPrismPropertiesPanel(String containerHeaderKey){
        expandContainerPropertiesPanel(containerHeaderKey);

        SelenideElement containerHeaderPanel = $(Schrodinger.byDataResourceKey("a", containerHeaderKey));
        return containerHeaderPanel
                .parent()
                .parent()
                .parent()
                .find(By.className("prism-properties"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);

    }

    public PrismForm<T> collapseAllChildrenContainers(String parentContainerHeraderKey){
        SelenideElement parentContainerPanel = null;
        if  ($(Schrodinger.byElementAttributeValue("a", "data-s-resource-key", parentContainerHeraderKey))
                .is(Condition.exist)) {
            parentContainerPanel = $(Schrodinger.byElementAttributeValue("a", "data-s-resource-key", parentContainerHeraderKey))
                    .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        } else {
            parentContainerPanel = $(By.linkText(parentContainerHeraderKey))
                    .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        }
        if (parentContainerPanel == null){
            return this;
        }
        parentContainerPanel = parentContainerPanel
                .parent()
                .parent()
                .parent()
                .parent()
                .$(By.className("container-wrapper"))
                .shouldBe(Condition.visible);

        while (parentContainerPanel.findAll(By.className(CARET_DOWN_ICON_STYLE)) != null &&
                        parentContainerPanel.findAll(By.className(CARET_DOWN_ICON_STYLE)).size() > 0){
            SelenideElement childContainerHeaderIcon = parentContainerPanel.find(By.className(CARET_DOWN_ICON_STYLE));
            childContainerHeaderIcon
                    .shouldBe(Condition.visible)
                    .click();
            childContainerHeaderIcon
                    .is(Condition.not(Condition.exist));
        }
        return this;
    }
}
