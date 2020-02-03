/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.modal;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;

/**
 * Created by matus on 4/26/2018.
 */
public class ModalBox<T> extends Component<T> {
    public ModalBox(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
