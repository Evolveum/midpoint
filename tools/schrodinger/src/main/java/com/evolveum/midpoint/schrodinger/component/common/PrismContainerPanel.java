/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;

public class PrismContainerPanel<T> extends Component<T> {

    public PrismContainerPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public PrismForm<PrismContainerPanel<T>> getContainerFormFragment() {
        return new PrismForm<PrismContainerPanel<T>>(this, getParentElement());
    }

}
