/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class Component<T> {

    private T parent;

    private SelenideElement parentElement;

    public Component(T parent) {
        this(parent, null);
    }

    public Component(T parent, SelenideElement parentElement) {
        this.parent = parent;
        this.parentElement = parentElement;
    }

    public T and() {
        return parent;
    }

    public T getParent() {
        return parent;
    }

    public SelenideElement getParentElement() {
        return parentElement;
    }

    protected SelenideElement getDisplayedElement(ElementsCollection elements) {
        for (SelenideElement element : elements) {
            if (element.isDisplayed()) {
                return element;
            }
        }
        return null;
    }
}
