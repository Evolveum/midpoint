/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.configuration.SystemTab;

/**
 * @author skublik
 */

public class TabWithContainerWrapper<T extends TabWithContainerWrapper, P> extends Component<P> {

    public TabWithContainerWrapper(P parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public PrismForm<T> form() {
        SelenideElement element = null;
        return new PrismForm<T>((T) this, element);
    }
}
