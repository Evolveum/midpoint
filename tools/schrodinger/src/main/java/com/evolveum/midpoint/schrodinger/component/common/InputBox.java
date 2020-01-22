/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;

/**
 * Created by martin.lizner on 12/08/2018.
 */
public class InputBox<T> extends Component<T> {
    public InputBox(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public InputBox<T> inputValue(String input) {
        getParentElement().setValue(input);

        return this;
    }

    public T pressEnter() {
        getParentElement().pressEnter();

        return this.getParent();
    }
}
