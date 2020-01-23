/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.resource;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;

/**
 * Created by matus on 3/28/2018.
 */
public class ResourceConnectorPoolTab<T> extends Component<T> {
    public ResourceConnectorPoolTab(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
