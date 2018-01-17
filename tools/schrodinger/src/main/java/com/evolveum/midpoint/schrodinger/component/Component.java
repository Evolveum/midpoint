package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.SelenideElement;

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
}
