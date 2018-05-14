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
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import javax.xml.namespace.QName;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PrismForm<T> extends Component<T> {

    public PrismForm(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public PrismForm<T> addAttributeValue(String name, String value) {
        SelenideElement property = findProperty(name);

        ElementsCollection values = property.$$(By.className("prism-property-value"));
        System.out.println("Value size: " + values.size());
        if (values.size() == 1) {
            values.first().$(By.className("form-control")).setValue(value);
        }

        // todo implement
        return this;
    }

    public PrismForm<T> addProtectedAttributeValue(String protectedAttributeName, String value) {
        SelenideElement property = findProperty(protectedAttributeName);
        ElementsCollection values = property.$$(By.xpath(".//input[contains(@class,\"form-control\")]"));
        for (SelenideElement valueElemen : values) {
            valueElemen.setValue(value);
        }

        return this;
    }

    public PrismForm<T> removeAttributeValue(String name, String value) {
        // todo implement
        return this;
    }

    public PrismForm<T> changeAttributeValue(String name, String oldValue, String newValue) {
        SelenideElement property = findProperty(name);

        ElementsCollection values = property.$$(By.className("prism-property-value"));
        if (values.size() == 1) {
            values.first().$(By.className("form-control")).setValue(newValue);
        }

        // todo implement
        return this;
    }

    public PrismForm<T> showEmptyAttributes(String containerName) {
        $(Schrodinger.bySelfOrAncestorElementAttributeValue("button", "data-s-id", "showEmptyFields", "data-s-resource-key", containerName))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).click();

        return this;
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

    private SelenideElement findProperValueContainer() {
        return null;
    }

    private SelenideElement findProperty(String name) {

        SelenideElement element = null;

        boolean doesElementAttrValueExist = $(Schrodinger.byElementAttributeValue(null, "contains",
                Schrodinger.DATA_S_QNAME, "#" + name)).exists();

        if (doesElementAttrValueExist) {
            element = $(Schrodinger.byElementAttributeValue(null, "contains",
                    Schrodinger.DATA_S_QNAME, "#" + name));

        } else {
            element = $(By.xpath("//span[@data-s-id=\"label\"][text()=\"" + name + "\"]/.."))
                    .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).parent();
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
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT).selectOption(option);

        return this;
    }
}
