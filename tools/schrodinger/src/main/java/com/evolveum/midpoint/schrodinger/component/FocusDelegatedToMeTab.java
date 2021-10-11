/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.SelenideElement;

/**
 * Created by Viliam Repan (lazyman).
 */
public class FocusDelegatedToMeTab<T> extends Component<T> {

    public FocusDelegatedToMeTab(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
