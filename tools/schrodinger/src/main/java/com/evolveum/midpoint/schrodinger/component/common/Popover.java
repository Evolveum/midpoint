/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.Keys;

/**
 * Created by matus on 3/22/2018.
 */
public class Popover<T> extends Component<T> {
    public Popover(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public Popover<T> inputValue(String input) {
        getDisplayedElement(getParentElement().$$(Schrodinger.byDataId("textInput"))).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(input);

        return this;
    }

    public Popover<T> inputValueWithEnter(String input) {
        SelenideElement inputField = getDisplayedElement(getParentElement().$$(Schrodinger.byDataId("textInput")));
        inputField.waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(input);
        inputField.sendKeys(Keys.ENTER);
        return this;
    }

    public T updateSearch() {
        SelenideElement button = getDisplayedElement(getParentElement().$$(Schrodinger.byDataId("update")));
        button.click();
        button.waitUntil(Condition.disappears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return this.getParent();
    }

    public T close() {
        getDisplayedElement(getParentElement().$$(Schrodinger.byDataId("searchSimple"))).click();

        return this.getParent();
    }

    public Popover addAnotherValue() {
        //TODO

        return this;
    }

    public Popover removeValue(Integer i) {
        //TODO

        return this;
    }
}
